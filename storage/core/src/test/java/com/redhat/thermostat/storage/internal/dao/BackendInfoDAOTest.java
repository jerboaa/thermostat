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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.BackendInformation;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class BackendInfoDAOTest {

    private BackendInformation backendInfo1;
    private BackendInformation backend1;
    private ExpressionFactory factory;

    @Before
    public void setUp() {

        backendInfo1 = new BackendInformation();

        backendInfo1.setName("backend-name");
        backendInfo1.setDescription("description");
        backendInfo1.setActive(true);
        backendInfo1.setObserveNewJvm(true);
        backendInfo1.setPids(new int[] { -1, 0, 1});
        backendInfo1.setOrderValue(100);

        backend1 = new BackendInformation();
        backend1.setName("backend-name");
        backend1.setDescription("description");
        backend1.setActive(true);
        backend1.setObserveNewJvm(true);
        backend1.setPids(new int[] { -1, 0, 1});
        backend1.setOrderValue(100);
        
        factory = new ExpressionFactory();
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedBackendInfo = "QUERY backend-info WHERE 'agentId' = ?s";
        assertEquals(expectedBackendInfo, BackendInfoDAOImpl.QUERY_BACKEND_INFO);
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

    @Test
    public void verifyAddBackendInformation() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);

        BackendInfoDAO dao = new BackendInfoDAOImpl(storage);

        dao.addBackendInformation(backendInfo1);

        verify(storage).createAdd(BackendInfoDAO.CATEGORY);
        verify(add).setPojo(backendInfo1);
        verify(add).apply();
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

    @Test
    public void verifyRemoveBackendInformation() {
        Remove remove = QueryTestHelper.createMockRemove();
        Storage storage = mock(Storage.class);
        when(storage.createRemove()).thenReturn(remove);
        BackendInfoDAO dao = new BackendInfoDAOImpl(storage);

        dao.removeBackendInformation(backendInfo1);

        verify(storage).removePojo(remove);
        InOrder inOrder = inOrder(remove);
        inOrder.verify(remove).from(BackendInfoDAO.CATEGORY);
        Expression expr = factory.equalTo(BackendInfoDAO.BACKEND_NAME, "backend-name");
        inOrder.verify(remove).where(eq(expr));
    }

}

