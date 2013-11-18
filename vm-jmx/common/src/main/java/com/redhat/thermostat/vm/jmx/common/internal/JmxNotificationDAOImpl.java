/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.jmx.common.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationDAOImpl implements JmxNotificationDAO {

    private static final Key<Boolean> NOTIFICATIONS_ENABLED = new Key<>("enabled");
    private static final Logger logger = LoggingUtils.getLogger(JmxNotificationDAOImpl.class);

    static final Category<JmxNotificationStatus> NOTIFICATION_STATUS =
            new Category<>("vm-jmx-notification-status", JmxNotificationStatus.class,
                    Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, NOTIFICATIONS_ENABLED);

    private static final Key<String> SOURCE_BACKEND = new Key<>("sourceBackend");
    private static final Key<String> SOURCE_DETAILS = new Key<>("sourceDetails");
    private static final Key<String> CONTENTS = new Key<>("contents");

    static final Category<JmxNotification> NOTIFICATIONS =
            new Category<>("vm-jmx-notification", JmxNotification.class,
                    Arrays.<Key<?>>asList(
                            Key.AGENT_ID,
                            Key.VM_ID,
                            Key.TIMESTAMP,
                            SOURCE_BACKEND,
                            SOURCE_DETAILS,
                            CONTENTS),
                    Arrays.<Key<?>>asList(Key.TIMESTAMP));
    
    // Query descriptors
            
    static final String QUERY_LATEST_NOTIFICATION_STATUS = "QUERY "
            + NOTIFICATION_STATUS.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    static final String QUERY_NOTIFICATIONS = "QUERY "
            + NOTIFICATIONS.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l";
    
    // Write descriptors
    
    // ADD vm-jmx-notification-status SET 'agentId' = ?s , \
    //                                    'vmId' = ?s , \
    //                                    'timeStamp' = ?l , \
    //                                    'enabled' = ?b
    static final String DESC_ADD_NOTIFICATION_STATUS = "ADD " + NOTIFICATION_STATUS.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + NOTIFICATIONS_ENABLED.getName() + "' = ?b";
    // ADD vm-jmx-notification SET 'agentId' = ?s , \
    //                             'vmId' = ?s , \
    //                             'timeStamp' = ?l , \
    //                             'contents' = ?s , \
    //                             'sourceDetails' = ?s , \
    //                             'sourceBackend' = ?s
    static final String DESC_ADD_NOTIFICATION = "ADD " + NOTIFICATIONS.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + CONTENTS.getName() + "' = ?s , " +
                 "'" + SOURCE_DETAILS.getName() + "' = ?s , " +
                 "'" + SOURCE_BACKEND.getName() + "' = ?s";
    
    private Storage storage;

    public JmxNotificationDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(NOTIFICATION_STATUS);
        storage.registerCategory(NOTIFICATIONS);
    }

    @Override
    public void addNotificationStatus(JmxNotificationStatus status) {
        StatementDescriptor<JmxNotificationStatus> desc = new StatementDescriptor<>(NOTIFICATION_STATUS, DESC_ADD_NOTIFICATION_STATUS);
        PreparedStatement<JmxNotificationStatus> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, status.getAgentId());
            prepared.setString(1, status.getVmId());
            prepared.setLong(2, status.getTimeStamp());
            prepared.setBoolean(3, status.isEnabled());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public JmxNotificationStatus getLatestNotificationStatus(VmRef statusFor) {
        StatementDescriptor<JmxNotificationStatus> desc = new StatementDescriptor<>(NOTIFICATION_STATUS, 
                QUERY_LATEST_NOTIFICATION_STATUS);
        PreparedStatement<JmxNotificationStatus> stmt;
        Cursor<JmxNotificationStatus> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, statusFor.getHostRef().getAgentId());
            stmt.setString(1, statusFor.getVmId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        
        JmxNotificationStatus result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }

        return result;
    }

    @Override
    public void addNotification(JmxNotification notification) {
        StatementDescriptor<JmxNotification> desc = new StatementDescriptor<>(NOTIFICATIONS, DESC_ADD_NOTIFICATION);
        PreparedStatement<JmxNotification> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, notification.getAgentId());
            prepared.setString(1, notification.getVmId());
            prepared.setLong(2, notification.getTimeStamp());
            prepared.setString(3, notification.getContents());
            prepared.setString(4, notification.getSourceDetails());
            prepared.setString(5, notification.getSourceBackend());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public List<JmxNotification> getNotifications(VmRef notificationsFor, long timeStampSince) {
        StatementDescriptor<JmxNotification> desc = new StatementDescriptor<>(NOTIFICATIONS, QUERY_NOTIFICATIONS);
        PreparedStatement<JmxNotification> stmt;
        Cursor<JmxNotification> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, notificationsFor.getHostRef().getAgentId());
            stmt.setString(1, notificationsFor.getVmId());
            stmt.setLong(2, timeStampSince);
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }

        List<JmxNotification> results = new ArrayList<>();
        while (cursor.hasNext()) {
            results.add(cursor.next());
        }

        return results;
    }

}
