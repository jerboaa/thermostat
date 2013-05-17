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
import java.util.List;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationDAOImpl implements JmxNotificationDAO {

    private static final Key<Boolean> NOTIFICATIONS_ENABLED = new Key<>("notififcationsEnabled", false);

    static final Category<JmxNotificationStatus> NOTIFICATION_STATUS =
            new Category<>("vm-jmx-notification-status", JmxNotificationStatus.class,
                    Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, NOTIFICATIONS_ENABLED);

    // TODO: private static final Key IMPORTANCE = new Key<>("importance",
    // false);

    private static final Key<String> SOURCE_BACKEND = new Key<>("sourceBackend", false);
    private static final Key<String> SOURCE_DESCRPTION = new Key<>("sourceDescription", false);
    private static final Key<String> CONTENTS = new Key<>("contents", false);

    static final Category<JmxNotification> NOTIFICATIONS =
            new Category<>("vm-jmx-notification", JmxNotification.class,
                    Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
                    SOURCE_BACKEND, SOURCE_DESCRPTION, CONTENTS);

    private Storage storage;

    public JmxNotificationDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(NOTIFICATION_STATUS);
        storage.registerCategory(NOTIFICATIONS);
    }

    @Override
    public void addNotifcationStatus(JmxNotificationStatus status) {
        Add add = storage.createAdd(NOTIFICATION_STATUS);
        add.setPojo(status);
        add.apply();
    }

    @Override
    public JmxNotificationStatus getLatestNotificationStatus(VmRef statusFor) {
        Query<JmxNotificationStatus> query = storage.createQuery(NOTIFICATION_STATUS);
        query.where(Key.AGENT_ID, Criteria.EQUALS, statusFor.getAgent().getAgentId());
        query.where(Key.VM_ID, Criteria.EQUALS, statusFor.getId());

        query.sort(Key.TIMESTAMP, SortDirection.DESCENDING);
        Cursor<JmxNotificationStatus> results = query.execute();
        if (results.hasNext()) {
            return results.next();
        }

        return null;
    }

    @Override
    public void addNotification(JmxNotification notification) {
        Add add = storage.createAdd(NOTIFICATIONS);
        add.setPojo(notification);
        add.apply();
    }

    @Override
    public List<JmxNotification> getNotifications(VmRef notificationsFor, long timeStampSince) {
        Query<JmxNotification> query = storage.createQuery(NOTIFICATIONS);
        query.where(Key.AGENT_ID, Criteria.EQUALS, notificationsFor.getAgent().getAgentId());
        query.where(Key.VM_ID, Criteria.EQUALS, notificationsFor.getId());
        query.where(Key.TIMESTAMP, Criteria.GREATER_THAN, timeStampSince);

        List<JmxNotification> results = new ArrayList<>();
        Cursor<JmxNotification> cursor = query.execute();
        while (cursor.hasNext()) {
            results.add(cursor.next());
        }

        return results;
    }

}
