/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationDAOImpl extends AbstractDao implements JmxNotificationDAO {

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
                    Collections.<Key<?>>singletonList(Key.TIMESTAMP));
    
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
    public void addNotificationStatus(final JmxNotificationStatus status) {
        executeStatement(new AbstractDaoStatement<JmxNotificationStatus>(storage, NOTIFICATION_STATUS, DESC_ADD_NOTIFICATION_STATUS) {
            @Override
            public PreparedStatement<JmxNotificationStatus> customize(PreparedStatement<JmxNotificationStatus> preparedStatement) {
                preparedStatement.setString(0, status.getAgentId());
                preparedStatement.setString(1, status.getVmId());
                preparedStatement.setLong(2, status.getTimeStamp());
                preparedStatement.setBoolean(3, status.isEnabled());
                return preparedStatement;
            }
        });
    }

    @Override
    public JmxNotificationStatus getLatestNotificationStatus(final VmRef statusFor) {
        return executeQuery(new AbstractDaoQuery<JmxNotificationStatus>(storage, NOTIFICATION_STATUS, QUERY_LATEST_NOTIFICATION_STATUS) {
            @Override
            public PreparedStatement<JmxNotificationStatus> customize(PreparedStatement<JmxNotificationStatus> preparedStatement) {
                preparedStatement.setString(0, statusFor.getHostRef().getAgentId());
                preparedStatement.setString(1, statusFor.getVmId());
                return preparedStatement;
            }
        }).head();
    }

    @Override
    public void addNotification(final JmxNotification notification) {
        executeStatement(new AbstractDaoStatement<JmxNotification>(storage, NOTIFICATIONS, DESC_ADD_NOTIFICATION) {
            @Override
            public PreparedStatement<JmxNotification> customize(PreparedStatement<JmxNotification> preparedStatement) {
                preparedStatement.setString(0, notification.getAgentId());
                preparedStatement.setString(1, notification.getVmId());
                preparedStatement.setLong(2, notification.getTimeStamp());
                preparedStatement.setString(3, notification.getContents());
                preparedStatement.setString(4, notification.getSourceDetails());
                preparedStatement.setString(5, notification.getSourceBackend());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<JmxNotification> getNotifications(final VmRef notificationsFor, final long timeStampSince) {
        return executeQuery(new AbstractDaoQuery<JmxNotification>(storage, NOTIFICATIONS, QUERY_NOTIFICATIONS) {
            @Override
            public PreparedStatement<JmxNotification> customize(PreparedStatement<JmxNotification> preparedStatement) {
                preparedStatement.setString(0, notificationsFor.getHostRef().getAgentId());
                preparedStatement.setString(1, notificationsFor.getVmId());
                preparedStatement.setLong(2, timeStampSince);
                return preparedStatement;
            }
        }).asList();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

