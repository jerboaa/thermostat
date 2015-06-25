/*
 * Copyright 2012-2015 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.vm.jmx.agent.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxBackend extends BaseBackend {

    private static final Logger logger = LoggingUtils.getLogger(JmxBackend.class);

    private final ReceiverRegistry registry;
    private final RequestReceiver receiver;
    private final JmxNotificationDAO dao;
    private final MXBeanConnectionPool pool;
    private final Clock clock;

    private final NotificationListener registrationNotificationListener;
    private final NotificationListener notificationWriter;

    private final Map<Integer, MXBeanConnection> connections = new HashMap<>();
    private final WriterID writerId;

    private boolean isActive = false;

    // Used as a callback handler
    private static class VmIdAndPid {
        public final String vmId;
        public final int pid;

        public VmIdAndPid(String vmId, int pid) {
            this.vmId = vmId;
            this.pid = pid;
        }
    }

    public JmxBackend(Version version, ReceiverRegistry registry,
            JmxNotificationDAO dao, MXBeanConnectionPool pool,
            RequestReceiver receiver, WriterID writerId) {
        this(version, registry, dao, pool, receiver, new SystemClock(), writerId);
    }

    public JmxBackend(Version version, ReceiverRegistry registry,
            JmxNotificationDAO dao, MXBeanConnectionPool pool,
            RequestReceiver receiver, Clock clock, WriterID writerId) {
        super("VM JMX Backend", "gathers JMX information using JMX", "Red Hat, Inc.", version.getVersionNumber());

        this.registry = registry;
        this.pool = pool;
        this.dao = dao;
        this.clock = clock;
        this.writerId = writerId;

        this.registrationNotificationListener = new RegistrationNotificationListener();
        this.notificationWriter = new NotificationWriter();

        this.receiver = receiver;
    }

    @Override
    public int getOrderValue() {
        return ORDER_USER_GROUP + 99;
    }

    @Override
    public boolean activate() {
        if (isActive) {
            return true;
        }

        registry.registerReceiver(receiver);

        isActive = true;
        return isActive;
    }

    @Override
    public boolean deactivate() {
        if (!isActive) {
            return true;
        }

        registry.unregisterReceivers();

        isActive = false;
        return true;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    public void enableNotificationsFor(String vmId, int pid) {
        VmIdAndPid idAndPid = new VmIdAndPid(vmId, pid);
        try {
            MXBeanConnection connection = pool.acquire(pid);
            connections.put(pid, connection);
            MBeanServerConnection server = connection.get();
            Set<ObjectName> names = server.queryNames(null, null);
            for (ObjectName name : names) {
                if (name.equals(MBeanServerDelegate.DELEGATE_NAME)) {
                    server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this.registrationNotificationListener, null, idAndPid);
                } else {
                    addNotificationListenerToMBean(idAndPid, server, name);
                }
            }
            String wId = writerId.getWriterID();
            JmxNotificationStatus update = new JmxNotificationStatus(wId);
            update.setVmId(vmId);
            update.setEnabled(true);
            update.setTimeStamp(clock.getRealTimeMillis());
            dao.addNotificationStatus(update);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to connect to the mx bean connector", e);
        }
    }

    public void disableNotificationsFor(String vmId, int pid) {
        MXBeanConnection connection = connections.get(pid);

        String wId = writerId.getWriterID();
        JmxNotificationStatus update = new JmxNotificationStatus(wId);
        update.setVmId(vmId);
        update.setEnabled(false);
        update.setTimeStamp(clock.getRealTimeMillis());
        dao.addNotificationStatus(update);

        try {
            pool.release(pid, connection);
        } catch (MXBeanConnectionException e) {
            logger.warning("Unable to release mx bean connection");
        }
    }

    private class RegistrationNotificationListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            VmIdAndPid idAndPid = (VmIdAndPid) handback;
            MBeanServerConnection server = connections.get(idAndPid.pid).get();
            MBeanServerNotification serverNotification = (MBeanServerNotification) notification;
            ObjectName name = serverNotification.getMBeanName();

            try {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(serverNotification.getType())) {
                    logger.fine("MBean Registered: " + name);
                    addNotificationListenerToMBean(idAndPid, server, name);
                } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(serverNotification.getType())) {
                    logger.fine("MBean Unregistered: " + name);
                    // we should remove the listener, but the object is not
                    // around to emit notifications
                }
            } catch (IOException | InstanceNotFoundException | IntrospectionException | ReflectionException e) {
                logger.log(Level.WARNING, "exception while handling MBeanServerNotification", e);
            }
        }

    }

    // Writes the notification to storage
    private class NotificationWriter implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            VmIdAndPid idAndPid = (VmIdAndPid) handback;

            String wId = writerId.getWriterID();
            JmxNotification data = new JmxNotification(wId);
            data.setVmId(idAndPid.vmId);
            data.setTimeStamp(notification.getTimeStamp());
            data.setSourceBackend(JmxBackend.class.getName());
            data.setSourceDetails(((ObjectName) notification.getSource()).getCanonicalName());
            data.setContents(notification.getMessage());
            dao.addNotification(data);

        }
    }

    private void addNotificationListenerToMBean(VmIdAndPid idAndPid, MBeanServerConnection server, ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        if (server.getMBeanInfo(name).getNotifications().length > 0) {
            server.addNotificationListener(name, JmxBackend.this.notificationWriter, null, idAndPid);
        }
    }

}

