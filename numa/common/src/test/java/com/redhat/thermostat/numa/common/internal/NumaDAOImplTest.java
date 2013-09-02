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

package com.redhat.thermostat.numa.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaHostInfo;
import com.redhat.thermostat.numa.common.NumaNodeStat;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class NumaDAOImplTest {

    private NumaDAO numaDAO;
    private Storage storage;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        numaDAO = new NumaDAOImpl(storage);
    }

    @After
    public void tearDown() {
        numaDAO = null;
        storage = null;
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryNumaInfo = "QUERY numa-host-info WHERE 'agentId' = ?s LIMIT 1";
        assertEquals(expectedQueryNumaInfo, NumaDAOImpl.QUERY_NUMA_INFO);
    }

    @Test
    public void testRegisterCategory() {
        verify(storage).registerCategory(NumaDAO.numaStatCategory);
    }

    @Test
    public void testPutNumaStat() {

        @SuppressWarnings("unchecked")
        Add<NumaStat> add = mock(Add.class);
        when(storage.createAdd(NumaDAO.numaStatCategory)).thenReturn(add);

        NumaNodeStat stat = new NumaNodeStat();
        stat.setNodeId(1);
        stat.setNumaHit(2);
        stat.setNumaMiss(3);
        stat.setNumaForeign(4);
        stat.setInterleaveHit(5);
        stat.setLocalNode(6);
        stat.setOtherNode(7);

        NumaStat numaStat = new NumaStat("agentId");
        numaStat.setTimeStamp(12345);
        numaStat.setNodeStats(new NumaNodeStat[] { stat });
        numaDAO.putNumaStat(numaStat);

        verify(storage).registerCategory(NumaDAO.numaStatCategory);
        verify(storage).registerCategory(NumaDAO.numaHostCategory);
        verify(storage).createAdd(NumaDAO.numaStatCategory);
        verifyNoMoreInteractions(storage);
        verify(add).setPojo(numaStat);
        verify(add).apply();
        verifyNoMoreInteractions(add);
        assertEquals("agentId", numaStat.getAgentId());
    }
    
    @Test
    public void testPutNumberOfNumaNodes() {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Add<NumaHostInfo> add = mock(Add.class);
        when(storage.createAdd(eq(NumaDAO.numaHostCategory))).thenReturn(add);

        NumaDAOImpl dao = new NumaDAOImpl(storage);

        NumaHostInfo info = new NumaHostInfo("foo");
        info.setNumNumaNodes(4);
        dao.putNumberOfNumaNodes(info);

        verify(storage).createAdd(NumaDAO.numaHostCategory);
        verify(add).setPojo(info);
        verify(add).apply();
    }
    
    @Test
    public void testGetNumberOfNumaNodes() throws DescriptorParsingException, StatementExecutionException {
        NumaHostInfo info = mock(NumaHostInfo.class);
        when(info.getNumNumaNodes()).thenReturn(2);
        
        @SuppressWarnings("unchecked")
        PreparedStatement<NumaHostInfo> stmt = (PreparedStatement<NumaHostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<NumaHostInfo> cursor = (Cursor<NumaHostInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(info).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);
        
        final String agentId = "system";
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(agentId);
        int result = numaDAO.getNumberOfNumaNodes(hostRef);
        assertEquals(2, result);
        
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, agentId);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<NumaHostInfo> anyDescriptor() {
        return (StatementDescriptor<NumaHostInfo>) any(StatementDescriptor.class);
    }
}

