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

package com.redhat.thermostat.vm.compiler.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStat;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStatDao;
import com.redhat.thermostat.vm.compiler.common.internal.VmCompilerStatDaoImpl;

public class VmCompilerStatDaoImplTest {

    private static final Long TIMESTAMP = 1234L;
    private static final String VM_ID = "vmId";
    private static final Long TOTAL_COMPILES = 12345L;
    private static final Long TOTAL_BAILOUTS= 2345L;
    private static final Long TOTAL_INVALIDATES = 3456L;
    private static final Long COMPILATION_TIME = 4567L;
    private static final Long LAST_SIZE = 5678L;
    private static final Long LAST_TYPE = 6789L;
    private static final String LAST_METHOD = "lastMethod()";
    private static final Long LAST_FAILED_TYPE = 789L;
    private static final String LAST_FAILED_METHOD = "lastFailedMethod()";

    @Test
    public void testStatementDescriptorsAreSane() {
        String addVmCompilerStat = "ADD vm-compiler-stats SET 'agentId' = ?s , " +
                                                "'vmId' = ?s , " +
                                                "'timeStamp' = ?l , " +
                                                "'totalCompiles' = ?l , " +
                                                "'totalBailouts' = ?l , " +
                                                "'totalInvalidates' = ?l , " +
                                                "'compilationTime' = ?l , " +
                                                "'lastSize' = ?l , " +
                                                "'lastType' = ?l , " +
                                                "'lastMethod' = ?s , " +
                                                "'lastFailedType' = ?l , " +
                                                "'lastFailedMethod' = ?s";

        assertEquals(addVmCompilerStat, VmCompilerStatDaoImpl.DESC_ADD_VM_COMPILER_STAT);
    }

    @Test
    public void testCategory() {
        assertEquals("vm-compiler-stats", VmCompilerStatDao.vmCompilerStatsCategory.getName());
        Collection<Key<?>> keys = VmCompilerStatDao.vmCompilerStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Long>("totalCompiles")));
        assertTrue(keys.contains(new Key<Long>("totalBailouts")));
        assertTrue(keys.contains(new Key<Long>("totalInvalidates")));
        assertTrue(keys.contains(new Key<Long>("compilationTime")));
        assertTrue(keys.contains(new Key<Long>("lastSize")));
        assertTrue(keys.contains(new Key<Long>("lastType")));
        assertTrue(keys.contains(new Key<Long>("lastMethod")));
        assertTrue(keys.contains(new Key<Long>("lastFailedType")));
        assertTrue(keys.contains(new Key<Long>("lastFailedMethod")));
        assertEquals(12, keys.size());
    }

    @Test
    public void testGetLatestCompilerStatsBasic() throws DescriptorParsingException, StatementExecutionException {

        VmCompilerStat vmCompilerStat = getCompilerStat();

        @SuppressWarnings("unchecked")
        Cursor<VmCompilerStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(vmCompilerStat);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmCompilerStat> stmt = (PreparedStatement<VmCompilerStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn(VM_ID);

        VmCompilerStatDao dao = new VmCompilerStatDaoImpl(storage);
        List<VmCompilerStat> vmCompilerStats = dao.getLatestCompilerStats(vmRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, VM_ID);
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmCompilerStats.size());
        VmCompilerStat stat = vmCompilerStats.get(0);
        assertEquals(VM_ID, stat.getVmId());
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(TOTAL_COMPILES, (Long) stat.getTotalCompiles());
        assertEquals(TOTAL_BAILOUTS, (Long) stat.getTotalBailouts());
        assertEquals(TOTAL_INVALIDATES, (Long) stat.getTotalInvalidates());
        assertEquals(COMPILATION_TIME, (Long) stat.getCompilationTime());
        assertEquals(LAST_SIZE, (Long) stat.getLastSize());
        assertEquals(LAST_TYPE, (Long) stat.getLastType());
        assertEquals(LAST_METHOD, stat.getLastMethod());
        assertEquals(LAST_FAILED_TYPE, (Long) stat.getLastFailedType());
        assertEquals(LAST_FAILED_METHOD, stat.getLastFailedMethod());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmCompilerStat> anyDescriptor() {
        return (StatementDescriptor<VmCompilerStat>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmCompilerStat() throws DescriptorParsingException, StatementExecutionException {

        Storage storage = mock(Storage.class);
        PreparedStatement<VmCompilerStat> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        VmCompilerStat stat = getCompilerStat();
        VmCompilerStatDao dao = new VmCompilerStatDaoImpl(storage);
        dao.putVmCompilerStat(stat);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);

        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmCompilerStatDaoImpl.DESC_ADD_VM_COMPILER_STAT, desc.getDescriptor());

        verify(add).setString(0, stat.getAgentId());
        verify(add).setString(1, stat.getVmId());
        verify(add).setLong(2, stat.getTimeStamp());
        verify(add).setLong(3, stat.getTotalCompiles());
        verify(add).setLong(4, stat.getTotalBailouts());
        verify(add).setLong(5, stat.getTotalInvalidates());
        verify(add).setLong(6, stat.getCompilationTime());
        verify(add).setLong(7, stat.getLastSize());
        verify(add).setLong(8, stat.getLastType());
        verify(add).setString(9, stat.getLastMethod());
        verify(add).setLong(10, stat.getLastFailedType());
        verify(add).setString(11, stat.getLastFailedMethod());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    private VmCompilerStat getCompilerStat() {
        return new VmCompilerStat("foo-agent", VM_ID, TIMESTAMP,
                TOTAL_COMPILES, TOTAL_BAILOUTS, TOTAL_INVALIDATES,
                COMPILATION_TIME,
                LAST_SIZE, LAST_TYPE, LAST_METHOD,
                LAST_FAILED_TYPE, LAST_FAILED_METHOD);
    }

}

