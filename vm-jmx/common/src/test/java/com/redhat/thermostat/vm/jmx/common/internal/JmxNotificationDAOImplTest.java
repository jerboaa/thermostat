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

package com.redhat.thermostat.vm.jmx.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationDAOImplTest {

    private final String AGENT_ID = "an-agent's-id";
    private final String VM_ID = "vmId";

    private Storage storage;

    private JmxNotificationDAOImpl dao;
    private HostRef host;
    private VmRef vm;

    @Before
    public void setUp() {
        host = mock(HostRef.class);
        when(host.getAgentId()).thenReturn(AGENT_ID);

        vm = mock(VmRef.class);
        when(vm.getHostRef()).thenReturn(host);
        when(vm.getVmId()).thenReturn(VM_ID);

        storage = mock(Storage.class);

        dao = new JmxNotificationDAOImpl(storage);
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryLatestNotificationStatus = "QUERY vm-jmx-notification-status WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestNotificationStatus, JmxNotificationDAOImpl.QUERY_LATEST_NOTIFICATION_STATUS);
        String expectedQueryNotifications = "QUERY vm-jmx-notification WHERE 'agentId' = ?s AND 'vmId' = ?s AND 'timeStamp' > ?l";
        assertEquals(expectedQueryNotifications, JmxNotificationDAOImpl.QUERY_NOTIFICATIONS);
        String addNotificationStatus = "ADD vm-jmx-notification-status SET 'agentId' = ?s , " +
                                            "'vmId' = ?s , " +
                                            "'timeStamp' = ?l , " +
                                            "'enabled' = ?b";
        assertEquals(addNotificationStatus, JmxNotificationDAOImpl.DESC_ADD_NOTIFICATION_STATUS);
        String addNotificationDesc = "ADD vm-jmx-notification SET 'agentId' = ?s , " +
                                            "'vmId' = ?s , " +
                                            "'timeStamp' = ?l , " +
                                            "'contents' = ?s , " +
                                            "'sourceDetails' = ?s , " +
                                            "'sourceBackend' = ?s";
        assertEquals(addNotificationDesc, JmxNotificationDAOImpl.DESC_ADD_NOTIFICATION);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyAddNotificationStatus()
            throws DescriptorParsingException, StatementExecutionException {
        PreparedStatement<JmxNotificationStatus> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        JmxNotificationStatus data = new JmxNotificationStatus("foo-agent");
        data.setVmId("foo-vmId");
        data.setEnabled(true);
        data.setTimeStamp(System.currentTimeMillis());

        dao.addNotificationStatus(data);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(JmxNotificationDAOImpl.DESC_ADD_NOTIFICATION_STATUS, desc.getDescriptor());

        verify(add).setString(0, data.getAgentId());
        verify(add).setString(1, data.getVmId());
        verify(add).setLong(2, data.getTimeStamp());
        verify(add).setBoolean(3, data.isEnabled());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void verifyGetLatestNotificationStatus() throws DescriptorParsingException, StatementExecutionException {
        JmxNotificationStatus data = new JmxNotificationStatus("foo-agent");

        @SuppressWarnings("unchecked")
        PreparedStatement<JmxNotificationStatus> stmt = (PreparedStatement<JmxNotificationStatus>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor(JmxNotificationStatus.class))).thenReturn(stmt);

        @SuppressWarnings("unchecked")
        Cursor<JmxNotificationStatus> cursor = (Cursor<JmxNotificationStatus>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(data).thenThrow(new AssertionError("should not be called"));

        when(stmt.executeQuery()).thenReturn(cursor);

        JmxNotificationStatus result = dao.getLatestNotificationStatus(vm);

        verify(storage).prepareStatement(anyDescriptor(JmxNotificationStatus.class));
        verify(stmt).setString(0, AGENT_ID);
        verify(stmt).setString(1, VM_ID);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        assertTrue(result == data);
    }

    @SuppressWarnings("unchecked")
    private <T extends Pojo> StatementDescriptor<T> anyDescriptor(Class<T> type) {
        return (StatementDescriptor<T>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verfiyAddNotification() throws DescriptorParsingException,
            StatementExecutionException {
        PreparedStatement<JmxNotificationStatus> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        JmxNotification data = new JmxNotification("foo-agent");
        data.setVmId("foo-vmId");
        data.setContents("something-content");
        data.setTimeStamp(System.currentTimeMillis());
        data.setSourceBackend("foo-source-backend");
        data.setSourceDetails("foo-source-details");

        dao.addNotification(data);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(JmxNotificationDAOImpl.DESC_ADD_NOTIFICATION, desc.getDescriptor());

        verify(add).setString(0, data.getAgentId());
        verify(add).setString(1, data.getVmId());
        verify(add).setLong(2, data.getTimeStamp());
        verify(add).setString(3, data.getContents());
        verify(add).setString(4, data.getSourceDetails());
        verify(add).setString(5, data.getSourceBackend());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void verifyGetNotificationsForVmSince() throws DescriptorParsingException, StatementExecutionException {
        long timeStamp = 10;

        JmxNotification data = mock(JmxNotification.class);

        @SuppressWarnings("unchecked")
        PreparedStatement<JmxNotification> stmt = (PreparedStatement<JmxNotification>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor(JmxNotification.class))).thenReturn(stmt);

        @SuppressWarnings("unchecked")
        Cursor<JmxNotification> cursor = (Cursor<JmxNotification>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(data).thenThrow(new AssertionError("not supposed to be called again"));

        when(stmt.executeQuery()).thenReturn(cursor);

        List<JmxNotification> result = dao.getNotifications(vm, timeStamp);

        verify(storage).prepareStatement(anyDescriptor(JmxNotification.class));
        verify(stmt).setString(0, AGENT_ID);
        verify(stmt).setString(1, VM_ID);
        verify(stmt).setLong(2, timeStamp);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        assertEquals(1, result.size());
        assertSame(data, result.get(0));
    }
}

