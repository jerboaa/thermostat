/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester.internal;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadSession;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MalformedObjectNameException;

@SuppressWarnings("restriction")
class Harvester {

    static final long DEFAULT_INITIAL_DELAY = 0;
    static final long DEFAULT_PERIOD = 250;
    static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private static final Logger logger = LoggingUtils.getLogger(Harvester.class);

    private final ScheduledExecutorService threadPool;
    private final int pid;

    private boolean isConnected;
    private ScheduledFuture<?> harvester;
    private MXBeanConnection connection;
    private ThreadMXBean collectorBean;

    private HarvesterHelper harvesterHelper;
    private final MXBeanConnectionPool connectionPool;
    private final DeadlockHelper deadlockHelper;

    public Harvester(ThreadDao threadDao, ScheduledExecutorService threadPool,
                     String vmId, int pid, MXBeanConnectionPool connectionPool,
                     WriterID writerId)
    {
        this(pid, threadPool, connectionPool,
             new HarvesterHelper(threadDao, new SystemClock(), vmId, writerId),
             new DeadlockHelper(threadDao, new SystemClock(), vmId, writerId));
    }

    Harvester(int pid, ScheduledExecutorService threadPool,
              MXBeanConnectionPool connectionPool,
              HarvesterHelper harvesterHelper,
              DeadlockHelper deadlockHelper)
    {
        this.pid = pid;
        this.threadPool = threadPool;
        this.connectionPool = connectionPool;
        this.harvesterHelper = harvesterHelper;
        this.deadlockHelper = deadlockHelper;
    }
    
    synchronized boolean start() {
        if (isConnected()) {
            return true;
        }

        if (!connect()) {
            return false;
        }

        harvester = threadPool.scheduleAtFixedRate(new HarvesterAction(true),
                                                   DEFAULT_INITIAL_DELAY,
                                                   DEFAULT_PERIOD,
                                                   DEFAULT_TIME_UNIT);

        return isConnected();
    }

    private synchronized boolean connect() {
        try {
            connection = connectionPool.acquire(pid);
        } catch (MXBeanConnectionException ex) {
            logger.log(Level.SEVERE, "can't connect", ex);
            return false;
        }

        setConnected(true);
        return true;
    }

    synchronized boolean isConnected() {
        return isConnected;
    }

    synchronized void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    synchronized boolean stop() {
        if (!isConnected()) {
            return true;
        }
        
        harvester.cancel(false);
        
        return disconnect();
    }
    
    int getPid() {
        return pid;
    }

    private boolean disconnect() {
        if (collectorBean != null) {
            collectorBean = null;
        }
        
        setConnected(false);

        try {
            connectionPool.release(pid, connection);
        } catch (MXBeanConnectionException ex) {
            logger.log(Level.SEVERE, "can't disconnect", ex);
            return false;
        }

        return true;
    }
    
    ThreadMXBean getDataCollectorBean(MXBeanConnection connection) {

        if (connection == null) {
            logger.log(Level.WARNING, "MXBeanConnection is null");
            return null;
        }

        ThreadMXBean bean = null;
        try {
            bean = connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                          com.sun.management.ThreadMXBean.class);
        } catch (MalformedObjectNameException e) {
            logger.log(Level.FINE,
                       "com.sun.management.ThreadMXBean.class not " +
                       "available for " +
                       ManagementFactory.THREAD_MXBEAN_NAME, e);
        }

        if (bean == null) {
            try {
                bean = connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                              ThreadMXBean.class);
            } catch (MalformedObjectNameException e) {
                logger.log(Level.WARNING,
                           "java.lang.management.ThreadMXBean.class not " +
                           "available for " +
                           ManagementFactory.THREAD_MXBEAN_NAME, e);
            }
        }

        return bean;
    }

    private class HarvesterAction implements Runnable {

        private boolean newSession;
        private ThreadSession session;

        private HarvesterAction(boolean newSession) {
            this.newSession = newSession;
        }

        @Override
        public void run() {
            if (collectorBean == null) {
                collectorBean = getDataCollectorBean(connection);
            }

            // FIXME: should we do something in case this is still null?
            // it basically means JMX is not available at this point...
            if (collectorBean != null) {

                // try to set some properties we're going to need
                try {
                    if (collectorBean.isCurrentThreadCpuTimeSupported() &&
                        !collectorBean.isThreadCpuTimeEnabled())
                    {
                        collectorBean.setThreadCpuTimeEnabled(true);
                    }

                    if (collectorBean.isThreadContentionMonitoringSupported() &&
                        !collectorBean.isThreadContentionMonitoringEnabled())
                    {
                        collectorBean.setThreadContentionMonitoringEnabled(true);
                    }
                } catch (UnsupportedOperationException ignore) {}

                if (newSession) {
                    session = harvesterHelper.createSession();
                    harvesterHelper.saveSession(session);

                    newSession = false;
                }

                harvesterHelper.collectAndSaveThreadData(session, collectorBean);

            } else {
                logger.log(Level.WARNING, "ThreadMXBean is null, is JMX available?");
            }
        }
    }

    public boolean saveDeadLockData() {
        boolean disconnectAtEnd = false;
        if (!isConnected()) {
            disconnectAtEnd = true;

            connect();
        }

        if (collectorBean == null) {
            collectorBean = getDataCollectorBean(connection);
        }

        if (collectorBean != null) {
            deadlockHelper.saveDeadlockInformation(collectorBean);
        }

        if (disconnectAtEnd) {
            disconnect();
        }

        return true;
    }
    
}

