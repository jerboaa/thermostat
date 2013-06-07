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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationDAOImplTest {

    private final String AGENT_ID = "an-agent's-id";
    private final int VM_ID = -1;

    private Storage storage;

    private JmxNotificationDAOImpl dao;
    private HostRef host;
    private VmRef vm;

    @Before
    public void setUp() {
        host = mock(HostRef.class);
        when(host.getAgentId()).thenReturn(AGENT_ID);

        vm = mock(VmRef.class);
        when(vm.getAgent()).thenReturn(host);
        when(vm.getId()).thenReturn(VM_ID);

        storage = mock(Storage.class);

        dao = new JmxNotificationDAOImpl(storage);
    }

    @Test
    public void verifyAddNotificationStatus() {
        Add add = mock(Add.class);
        when(storage.createAdd(JmxNotificationDAOImpl.NOTIFICATION_STATUS)).thenReturn(add);

        JmxNotificationStatus data = mock(JmxNotificationStatus.class);

        dao.addNotifcationStatus(data);

        verify(add).setPojo(data);
        verify(add).apply();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void verifyGetLatestNotificationStatus() {
        JmxNotificationStatus data = new JmxNotificationStatus();

        Query<JmxNotificationStatus> query = mock(Query.class);
        when(storage.createQuery(JmxNotificationDAOImpl.NOTIFICATION_STATUS)).thenReturn(query);

        Cursor<JmxNotificationStatus> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(data).thenThrow(new AssertionError("should not be called"));

        when(query.execute()).thenReturn(cursor);

        JmxNotificationStatus result = dao.getLatestNotificationStatus(vm);

        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.and(factory.equalTo(Key.AGENT_ID, AGENT_ID),
                factory.equalTo(Key.VM_ID, VM_ID));
        verify(query).where(expr);
        verify(query).sort(Key.TIMESTAMP, SortDirection.DESCENDING);
        
        assertTrue(result == data);
    }

    @Test
    public void verfiyAddNotification() {
        Add add = mock(Add.class);
        when(storage.createAdd(JmxNotificationDAOImpl.NOTIFICATIONS)).thenReturn(add);

        JmxNotification data = mock(JmxNotification.class);

        dao.addNotification(data);

        verify(add).setPojo(data);
        verify(add).apply();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void verifyGetNotificationsForVmSince() {
        long timeStamp = 10;

        JmxNotification data = mock(JmxNotification.class);

        Query<JmxNotification> query = mock(Query.class);
        when(storage.createQuery(JmxNotificationDAOImpl.NOTIFICATIONS)).thenReturn(query);

        Cursor<JmxNotification> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(data).thenThrow(new AssertionError("not supposed to be called again"));

        when(query.execute()).thenReturn(cursor);

        List<JmxNotification> result = dao.getNotifications(vm, timeStamp);

        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.and(
                factory.equalTo(Key.AGENT_ID, AGENT_ID),
                factory.and(factory.equalTo(Key.VM_ID, VM_ID),
                        factory.greaterThan(Key.TIMESTAMP, timeStamp)));
        verify(query).where(eq(expr));

        assertEquals(1, result.size());
        assertSame(data, result.get(0));
    }
}
