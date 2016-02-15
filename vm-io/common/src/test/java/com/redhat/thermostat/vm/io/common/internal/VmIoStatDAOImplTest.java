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

package com.redhat.thermostat.vm.io.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.testutils.StatementDescriptorTester;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoStatDAOImplTest {

    private static final long SOME_TIMESTAMP = 1234;
    private static final String SOME_VM_ID = "321";
    private static final long SOME_CHARACTERS_READ = 123456;
    private static final long SOME_CHARACTERS_WRITTEN = 67798;
    private static final long SOME_READ_SYSCALLS = 123456;
    private static final long SOME_WRITE_SYSCALLS = 67798;

    private VmIoStat ioStat;

    @Before
    public void setUp() {
        ioStat = new VmIoStat("foo-agent", SOME_VM_ID, SOME_TIMESTAMP,
                SOME_CHARACTERS_READ, SOME_CHARACTERS_WRITTEN,
                SOME_READ_SYSCALLS, SOME_WRITE_SYSCALLS);
    }

    @Test
    public void verifyDescriptorsAreSane() {
        String addIoStat = "ADD vm-io-stats SET 'agentId' = ?s , " +
                                               "'vmId' = ?s , " +
                                               "'timeStamp' = ?l , " +
                                               "'charactersRead' = ?l , " +
                                               "'charactersWritten' = ?l , " +
                                               "'readSyscalls' = ?l , " +
                                               "'writeSyscalls' = ?l";

        assertEquals(addIoStat, VmIoStatDAOImpl.DESC_ADD_VM_IO_STAT);
    }

    @Test
    public void canParseDescriptor() {
        StatementDescriptorTester<VmIoStat> tester = new StatementDescriptorTester<>();
        StatementDescriptor<VmIoStat> desc = new StatementDescriptor<>(VmIoStatDAOImpl.CATEGORY, VmIoStatDAOImpl.DESC_ADD_VM_IO_STAT);
        try {
            tester.testParseBasic(desc);
            tester.testParseSemantic(desc);
            // pass
        } catch (DescriptorParsingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCategory() {
        assertEquals("vm-io-stats", VmIoStatDAOImpl.CATEGORY.getName());
        Collection<Key<?>> keys = VmIoStatDAOImpl.CATEGORY.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Integer>("charactersRead")));
        assertTrue(keys.contains(new Key<Integer>("charactersWritten")));
        assertTrue(keys.contains(new Key<Integer>("readSyscalls")));
        assertTrue(keys.contains(new Key<Integer>("writeSyscalls")));
        assertEquals(7, keys.size());
    }

    @Test
    public void testVmRefGetLatestIoStatsBasic() throws DescriptorParsingException, StatementExecutionException {
        Pair<Storage, PreparedStatement<VmIoStat>> setup = setupGetLatest();
        Storage storage = setup.getFirst();
        PreparedStatement<VmIoStat> stmt = setup.getSecond();

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn(SOME_VM_ID);

        VmIoStatDAO dao = new VmIoStatDAOImpl(storage);
        List<VmIoStat> vmIoStats = dao.getLatestVmIoStats(vmRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, SOME_VM_ID);
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmIoStats.size());
        VmIoStat stat = vmIoStats.get(0);
        assertEquals(SOME_TIMESTAMP, stat.getTimeStamp());
        assertEquals(SOME_VM_ID, stat.getVmId());
        assertEquals(SOME_CHARACTERS_READ, stat.getCharactersRead());
        assertEquals(SOME_CHARACTERS_WRITTEN, stat.getCharactersWritten());
        assertEquals(SOME_READ_SYSCALLS, stat.getReadSyscalls());
        assertEquals(SOME_WRITE_SYSCALLS, stat.getWriteSyscalls());
    }

    @Test
    public void testGetLatestIoStatsBasic() throws DescriptorParsingException, StatementExecutionException {
        Pair<Storage, PreparedStatement<VmIoStat>> setup = setupGetLatest();
        Storage storage = setup.getFirst();
        PreparedStatement<VmIoStat> stmt = setup.getSecond();

        AgentId agentId = new AgentId("system");
        VmId vmId = new VmId(SOME_VM_ID);

        VmIoStatDAO dao = new VmIoStatDAOImpl(storage);
        List<VmIoStat> vmIoStats = dao.getLatestVmIoStats(agentId, vmId, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, SOME_VM_ID);
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmIoStats.size());
        VmIoStat stat = vmIoStats.get(0);
        assertEquals(SOME_TIMESTAMP, stat.getTimeStamp());
        assertEquals(SOME_VM_ID, stat.getVmId());
        assertEquals(SOME_CHARACTERS_READ, stat.getCharactersRead());
        assertEquals(SOME_CHARACTERS_WRITTEN, stat.getCharactersWritten());
        assertEquals(SOME_READ_SYSCALLS, stat.getReadSyscalls());
        assertEquals(SOME_WRITE_SYSCALLS, stat.getWriteSyscalls());
    }

    private Pair<Storage, PreparedStatement<VmIoStat>> setupGetLatest() throws
            DescriptorParsingException, StatementExecutionException {

        @SuppressWarnings("unchecked")
        Cursor<VmIoStat> cursor = (Cursor<VmIoStat>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(ioStat);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmIoStat> stmt = (PreparedStatement<VmIoStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        return new Pair<>(storage, stmt);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmIoStat> anyDescriptor() {
        return (StatementDescriptor<VmIoStat>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmIoStat() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmIoStat> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        VmIoStat stat = new VmIoStat("foo-agent", SOME_VM_ID, SOME_TIMESTAMP,
                SOME_CHARACTERS_READ, SOME_CHARACTERS_WRITTEN,
                SOME_READ_SYSCALLS, SOME_WRITE_SYSCALLS);
        VmIoStatDAO dao = new VmIoStatDAOImpl(storage);
        dao.putVmIoStat(stat);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);

        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmIoStatDAOImpl.DESC_ADD_VM_IO_STAT, desc.getDescriptor());

        verify(add).setString(0, stat.getAgentId());
        verify(add).setString(1, stat.getVmId());
        verify(add).setLong(2, stat.getTimeStamp());
        verify(add).setLong(3, stat.getCharactersRead());
        verify(add).setLong(4, stat.getCharactersWritten());
        verify(add).setLong(5, stat.getReadSyscalls());
        verify(add).setLong(6, stat.getWriteSyscalls());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

}
