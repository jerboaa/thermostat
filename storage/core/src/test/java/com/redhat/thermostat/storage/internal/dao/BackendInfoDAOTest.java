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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.BackendInformation;

public class BackendInfoDAOTest {

    private BackendInformation backendInfo1;
    private BackendInformation backend1;

    @Before
    public void setUp() {

        backendInfo1 = new BackendInformation("foo-agent1");

        backendInfo1.setName("backend-name");
        backendInfo1.setDescription("description");
        backendInfo1.setActive(true);
        backendInfo1.setObserveNewJvm(true);
        backendInfo1.setPids(new int[] { -1, 0, 1});
        backendInfo1.setOrderValue(100);

        backend1 = new BackendInformation("foo-agent2");
        backend1.setName("backend-name");
        backend1.setDescription("description");
        backend1.setActive(true);
        backend1.setObserveNewJvm(true);
        backend1.setPids(new int[] { -1, 0, 1});
        backend1.setOrderValue(100);
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedBackendInfo = "QUERY backend-info WHERE 'agentId' = ?s";
        assertEquals(expectedBackendInfo, BackendInfoDAOImpl.QUERY_BACKEND_INFO);
        String addBackendInfo = "ADD backend-info SET " +
                                        "'agentId' = ?s , " +
                                        "'name' = ?s , " +
                                        "'description' = ?s , " +
                                        "'observeNewJvm' = ?b , " +
                                        "'pids' = ?i[ , " +
                                        "'active' = ?b , " +
                                        "'orderValue' = ?i";
        assertEquals(addBackendInfo, BackendInfoDAOImpl.DESC_ADD_BACKEND_INFO);
    }

    @Test
    public void verifyCategoryName() {
        Category<BackendInformation> c = BackendInfoDAO.CATEGORY;
        assertEquals("backend-info", c.getName());
    }

    @Test
    public void verifyCategoryHasAllKeys() {
        Category<BackendInformation> c = BackendInfoDAO.CATEGORY;
        Collection<Key<?>> keys = c.getKeys();

        assertTrue(keys.contains(Key.AGENT_ID));
        assertTrue(keys.contains(BackendInfoDAO.BACKEND_NAME));
        assertTrue(keys.contains(BackendInfoDAO.BACKEND_DESCRIPTION));
        assertTrue(keys.contains(BackendInfoDAO.IS_ACTIVE));
        assertTrue(keys.contains(BackendInfoDAO.PIDS_TO_MONITOR));
        assertTrue(keys.contains(BackendInfoDAO.SHOULD_MONITOR_NEW_PROCESSES));
        assertTrue(keys.contains(BackendInfoDAO.ORDER_VALUE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyAddBackendInformation()
            throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<BackendInformation> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        BackendInfoDAO dao = new BackendInfoDAOImpl(storage);

        dao.addBackendInformation(backendInfo1);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(BackendInfoDAOImpl.DESC_ADD_BACKEND_INFO, desc.getDescriptor());

        verify(add).setString(0, backendInfo1.getAgentId());
        verify(add).setString(1, backendInfo1.getName());
        verify(add).setString(2, backendInfo1.getDescription());
        verify(add).setBoolean(3, backendInfo1.isObserveNewJvm());
        verify(add).setIntList(4, backendInfo1.getPids());
        verify(add).setBoolean(5, backendInfo1.isActive());
        verify(add).setInt(6, backendInfo1.getOrderValue());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void verifyGetBackendInformation() throws DescriptorParsingException, StatementExecutionException {
        final String AGENT_ID = "agent-id";
        HostRef agentref = mock(HostRef.class);
        when(agentref.getAgentId()).thenReturn(AGENT_ID);

        @SuppressWarnings("unchecked")
        Cursor<BackendInformation> backendCursor = (Cursor<BackendInformation>) mock(Cursor.class);
        when(backendCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(backendCursor.next()).thenReturn(backend1).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<BackendInformation> stmt = (PreparedStatement<BackendInformation>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(backendCursor);

        BackendInfoDAO dao = new BackendInfoDAOImpl(storage);

        List<BackendInformation> result = dao.getBackendInformation(agentref);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, AGENT_ID);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(Arrays.asList(backendInfo1), result);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<BackendInformation> anyDescriptor() {
        return (StatementDescriptor<BackendInformation>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyRemoveBackendInformation()
            throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<BackendInformation> remove = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(remove);
        
        BackendInfoDAO dao = new BackendInfoDAOImpl(storage);

        dao.removeBackendInformation(backendInfo1);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(BackendInfoDAOImpl.DESC_REMOVE_BACKEND_INFO, desc.getDescriptor());

        verify(remove).setString(0, backendInfo1.getName());
        verify(remove).execute();
        verifyNoMoreInteractions(remove);
    }

}

