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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
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
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.VmInfo;

public class VmInfoDAOTest {

    private String vmId;
    private int vmPid;
    private long startTime;
    private long stopTime;
    private String jVersion;
    private String jHome;
    private String mainClass;
    private String commandLine;
    private String vmName;
    private String vmInfo;
    private String vmVersion;
    private String vmArgs;
    private Map<String, String> props;
    private Map<String, String> env;
    private String[] libs;
    private long uid;
    private String username;

    @Before
    public void setUp() {
        vmId = "vmId";
        vmPid = 1;
        startTime = 2;
        stopTime = 3;
        jVersion = "java 1.0";
        jHome = "/path/to/jdk/home";
        mainClass = "Hello.class";
        commandLine = "World";
        vmArgs = "-XX=+FastestJITPossible";
        vmName = "Hotspot";
        vmInfo = "Some info";
        vmVersion = "1.0";
        props = new HashMap<>();
        env = new HashMap<>();
        libs = new String[0];
        uid = 2000;
        username = "myUser";
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedVmInfo = "QUERY vm-info WHERE 'agentId' = ?s AND 'vmId' = ?s LIMIT 1";
        assertEquals(expectedVmInfo, VmInfoDAOImpl.QUERY_VM_INFO);
        String expectedVmInfoAll = "QUERY vm-info WHERE 'agentId' = ?s";
        assertEquals(expectedVmInfoAll, VmInfoDAOImpl.QUERY_ALL_VMS_FOR_HOST);
        String expectedAllVms = "QUERY vm-info";
        assertEquals(expectedAllVms, VmInfoDAOImpl.QUERY_ALL_VMS);
        String aggregateAllVms = "QUERY-COUNT vm-info";
        assertEquals(aggregateAllVms, VmInfoDAOImpl.AGGREGATE_COUNT_ALL_VMS);
        String addVmInfo = "ADD vm-info SET 'agentId' = ?s , " +
                                            "'vmId' = ?s , " +
                                            "'vmPid' = ?i , " +
                                            "'startTimeStamp' = ?l , " +
                                            "'stopTimeStamp' = ?l , " +
                                            "'javaVersion' = ?s , " +
                                            "'javaHome' = ?s , " +
                                            "'mainClass' = ?s , " +
                                            "'javaCommandLine' = ?s , " +
                                            "'vmName' = ?s , " +
                                            "'vmArguments' = ?s , " +
                                            "'vmInfo' = ?s , " +
                                            "'vmVersion' = ?s , " +
                                            "'propertiesAsArray' = ?p[ , " +
                                            "'environmentAsArray' = ?p[ , " +
                                            "'loadedNativeLibraries' = ?s[ , " +
                                            "'uid' = ?l , " +
                                            "'username' = ?s";
        assertEquals(addVmInfo, VmInfoDAOImpl.DESC_ADD_VM_INFO);
        String updateVmStopTime = "UPDATE vm-info SET 'stopTimeStamp' = ?l WHERE 'vmId' = ?s";
        assertEquals(updateVmStopTime, VmInfoDAOImpl.DESC_UPDATE_VM_STOP_TIME);
    }

    @Test
    public void testCategory() {
        assertEquals("vm-info", VmInfoDAO.vmInfoCategory.getName());
        Collection<Key<?>> keys = VmInfoDAO.vmInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Integer>("vmPid")));
        assertTrue(keys.contains(new Key<String>("javaVersion")));
        assertTrue(keys.contains(new Key<String>("javaHome")));
        assertTrue(keys.contains(new Key<String>("mainClass")));
        assertTrue(keys.contains(new Key<String>("javaCommandLine")));
        assertTrue(keys.contains(new Key<String>("vmArguments")));
        assertTrue(keys.contains(new Key<String>("vmName")));
        assertTrue(keys.contains(new Key<String>("vmInfo")));
        assertTrue(keys.contains(new Key<String>("vmVersion")));
        assertTrue(keys.contains(new Key<Map<String, String>>("properties")));
        assertTrue(keys.contains(new Key<Map<String, String>>("environment")));
        assertTrue(keys.contains(new Key<List<String>>("loadedNativeLibraries")));
        assertTrue(keys.contains(new Key<Long>("startTimeStamp")));
        assertTrue(keys.contains(new Key<Long>("stopTimeStamp")));
        assertTrue(keys.contains(new Key<Long>("uid")));
        assertTrue(keys.contains(new Key<Long>("username")));
        assertEquals(18, keys.size());
    }

    @Test
    public void testGetVmInfo() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmInfo> stmt = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        VmInfo expected = new VmInfo("foo-agent", vmId, vmPid, startTime, stopTime, jVersion, jHome, mainClass, commandLine, vmName, vmInfo, vmVersion, vmArgs, props, env, libs, uid, username);
        @SuppressWarnings("unchecked")
        Cursor<VmInfo> cursor = (Cursor<VmInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(expected).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn("vmId");

        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        VmInfo info = dao.getVmInfo(vmRef);
        assertEquals(expected, info);
        
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, "vmId");
        verify(stmt).executeQuery();
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmInfo> anyDescriptor() {
        return (StatementDescriptor<VmInfo>) any(StatementDescriptor.class);
    }

    @Test
    public void testGetVmInfoUnknownVM() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmInfo> stmt = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<VmInfo> cursor = (Cursor<VmInfo>) mock(Cursor.class);
        when(stmt.executeQuery()).thenReturn(cursor);
        
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getVmId()).thenReturn("noVm");
        when(vmRef.getHostRef()).thenReturn(hostRef);

        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        try {
            dao.getVmInfo(vmRef);
            fail();
        } catch (DAOException ex) {
            assertEquals("Unknown VM: host:system;vm:noVm", ex.getMessage());
        }

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, "noVm");
        verify(stmt).executeQuery();
    }

    @Test
    public void testSingleVM() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = setupStorageForSingleVM();
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        HostRef host = new HostRef("123", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, "vmId", 123, "mainClass1"));
    }

    private Storage setupStorageForSingleVM() throws DescriptorParsingException, StatementExecutionException {

      VmInfo vm1 = new VmInfo();
      vm1.setVmId("vmId");
      vm1.setVmPid(123);
      vm1.setMainClass("mainClass1");

      @SuppressWarnings("unchecked")
      Cursor<VmInfo> singleVMCursor = (Cursor<VmInfo>) mock(Cursor.class);
      when(singleVMCursor.hasNext()).thenReturn(true).thenReturn(false);
      when(singleVMCursor.next()).thenReturn(vm1);

      Storage storage = mock(Storage.class);
      @SuppressWarnings("unchecked")
      PreparedStatement<VmInfo> stmt = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
      when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
      when(stmt.executeQuery()).thenReturn(singleVMCursor);
      return storage;
  }

    @Test
    public void testMultiVMs() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = setupStorageForMultiVM();
        VmInfoDAO dao = new VmInfoDAOImpl(storage);

        HostRef host = new HostRef("456", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, "vmId1", 123, "mainClass1"), new VmRef(host, "vmId2", 456, "mainClass2"));
    }

    private Storage setupStorageForMultiVM() throws DescriptorParsingException, StatementExecutionException {
      VmInfo vm1 = new VmInfo();
      vm1.setVmId("vmId1");
      vm1.setVmPid(123);
      vm1.setMainClass("mainClass1");

      VmInfo vm2 = new VmInfo();
      vm2.setVmId("vmId2");
      vm2.setVmPid(456);
      vm2.setMainClass("mainClass2");

      @SuppressWarnings("unchecked")
      Cursor<VmInfo> multiVMsCursor = mock(Cursor.class);
      when(multiVMsCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
      when(multiVMsCursor.next()).thenReturn(vm1).thenReturn(vm2);

      Storage storage = mock(Storage.class);
      @SuppressWarnings("unchecked")
      PreparedStatement<VmInfo> stmt = (PreparedStatement<VmInfo>) mock(PreparedStatement.class);
      when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
      when(stmt.executeQuery()).thenReturn(multiVMsCursor);
      return storage;
  }

    private void assertCollection(Collection<VmRef> vms, VmRef... expectedVMs) {
        assertEquals(expectedVMs.length, vms.size());
        for (VmRef expectedVM : expectedVMs) {
            assertTrue(vms.contains(expectedVM));
        }
    }

    @Test
    public void testGetCount()
            throws DescriptorParsingException, StatementExecutionException {
        AggregateCount count = new AggregateCount();
        count.setCount(2);
        
        @SuppressWarnings("unchecked")
        Cursor<AggregateCount> countCursor = (Cursor<AggregateCount>) mock(Cursor.class);
        when(countCursor.next()).thenReturn(count);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> stmt = (PreparedStatement<AggregateCount>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        StatementDescriptor<AggregateCount> desc = any(StatementDescriptor.class);
        when(storage.prepareStatement(desc)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(countCursor);
        VmInfoDAOImpl dao = new VmInfoDAOImpl(storage);

        assertEquals(2, dao.getCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmInfo() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmInfo> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);
        
        VmInfo info = new VmInfo("foo-agent", vmId, vmPid, startTime, stopTime, jVersion, jHome,
                mainClass, commandLine, vmName, vmInfo, vmVersion, vmArgs,
                props, env, libs, uid, username);
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        dao.putVmInfo(info);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmInfoDAOImpl.DESC_ADD_VM_INFO, desc.getDescriptor());

        verify(add).setString(0, info.getAgentId());
        verify(add).setString(1, info.getVmId());
        verify(add).setInt(2, info.getVmPid());
        verify(add).setLong(3, info.getStartTimeStamp());
        verify(add).setLong(4, info.getStopTimeStamp());
        verify(add).setString(5, info.getJavaVersion());
        verify(add).setString(6, info.getJavaHome());
        verify(add).setString(7, info.getMainClass());
        verify(add).setString(8, info.getJavaCommandLine());
        verify(add).setString(9, info.getVmName());
        verify(add).setString(10, info.getVmArguments());
        verify(add).setString(11, info.getVmInfo());
        verify(add).setString(12, info.getVmVersion());
        verify(add).setPojoList(13, info.getPropertiesAsArray());
        verify(add).setPojoList(14, info.getEnvironmentAsArray());
        verify(add).setStringList(15, info.getLoadedNativeLibraries());
        verify(add).setLong(16, info.getUid());
        verify(add).setString(17, info.getUsername());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmStoppedTime() throws DescriptorParsingException,
            StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmInfo> update = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(update);

        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        dao.putVmStoppedTime(vmId, stopTime);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmInfoDAOImpl.DESC_UPDATE_VM_STOP_TIME, desc.getDescriptor());
        
        verify(update).setLong(0, stopTime);
        verify(update).setString(1, vmId);
        verify(update).execute();
        verifyNoMoreInteractions(update);
    }
}

