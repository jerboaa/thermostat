/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.test.MockQuery;

public class VmInfoDAOTest {

    private int vmId;
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
    private List<String> libs;

    @Before
    public void setUp() {
        vmId = 1;
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
        libs = new ArrayList<>();
    }

    @Test
    public void testCategory() {
        assertEquals("vm-info", VmInfoDAO.vmInfoCategory.getName());
        Collection<Key<?>> keys = VmInfoDAO.vmInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Integer>("vmId", true)));
        assertTrue(keys.contains(new Key<Integer>("vmPid", false)));
        assertTrue(keys.contains(new Key<String>("javaVersion", false)));
        assertTrue(keys.contains(new Key<String>("javaHome", false)));
        assertTrue(keys.contains(new Key<String>("mainClass", false)));
        assertTrue(keys.contains(new Key<String>("javaCommandLine", false)));
        assertTrue(keys.contains(new Key<String>("vmArguments", false)));
        assertTrue(keys.contains(new Key<String>("vmName", false)));
        assertTrue(keys.contains(new Key<String>("vmInfo", false)));
        assertTrue(keys.contains(new Key<String>("vmVersion", false)));
        assertTrue(keys.contains(new Key<Map<String, String>>("properties", false)));
        assertTrue(keys.contains(new Key<Map<String, String>>("environment", false)));
        assertTrue(keys.contains(new Key<List<String>>("loadedNativeLibraries", false)));
        assertTrue(keys.contains(new Key<Long>("startTimeStamp", false)));
        assertTrue(keys.contains(new Key<Long>("stopTimeStamp", false)));
        assertEquals(16, keys.size());
    }

    @Test
    public void testGetVmInfo() {
        Chunk chunk = new Chunk(VmInfoDAO.vmInfoCategory, true);
        chunk.put(Key.VM_ID, vmId);
        chunk.put(VmInfoDAO.vmPidKey, vmId);
        chunk.put(VmInfoDAO.startTimeKey, startTime);
        chunk.put(VmInfoDAO.stopTimeKey, stopTime);
        chunk.put(VmInfoDAO.runtimeVersionKey, jVersion);
        chunk.put(VmInfoDAO.javaHomeKey, jHome);
        chunk.put(VmInfoDAO.mainClassKey, mainClass);
        chunk.put(VmInfoDAO.commandLineKey, commandLine);
        chunk.put(VmInfoDAO.vmNameKey, vmName);
        chunk.put(VmInfoDAO.vmInfoKey, vmInfo);
        chunk.put(VmInfoDAO.vmVersionKey, vmVersion);
        chunk.put(VmInfoDAO.vmArgumentsKey, vmArgs);
        chunk.put(VmInfoDAO.propertiesKey, props);
        chunk.put(VmInfoDAO.environmentKey, env);
        chunk.put(VmInfoDAO.librariesKey, libs);

        Storage storage = mock(Storage.class);
        Query query = new MockQuery();
        when(storage.createQuery()).thenReturn(query);
        VmInfo expected = new VmInfo(vmId, startTime, stopTime, jVersion, jHome, mainClass, commandLine, vmName, vmInfo, vmVersion, vmArgs, props, env, libs);
        when(storage.findPojo(query, VmInfo.class)).thenReturn(expected);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);

        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        VmInfo info = dao.getVmInfo(vmRef);
        assertEquals(expected, info);
    }

    @Test
    public void testGetVmInfoUnknownVM() {

        Storage storage = mock(Storage.class);
        Query query = new MockQuery();
        when(storage.createQuery()).thenReturn(query);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);

        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        try {
            dao.getVmInfo(vmRef);
            fail();
        } catch (DAOException ex) {
            assertEquals("Unknown VM: host:system;vm:321", ex.getMessage());
        }

    }

    @Test
    public void testSingleVM() {
        Storage storage = setupStorageForSingleVM();
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        HostRef host = new HostRef("123", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, 123, "mainClass1"));
    }

    private Storage setupStorageForSingleVM() {
      Query expectedQuery = new MockQuery()
          .from(VmInfoDAO.vmInfoCategory)
          .where(Key.AGENT_ID, Criteria.EQUALS, "123");

      Chunk vm1 = new Chunk(VmInfoDAO.vmInfoCategory, false);
      vm1.put(Key.VM_ID, 123);
      vm1.put(VmInfoDAO.mainClassKey, "mainClass1");

      Cursor singleVMCursor = mock(Cursor.class);
      when(singleVMCursor.hasNext()).thenReturn(true).thenReturn(false);
      when(singleVMCursor.next()).thenReturn(vm1);

      Storage storage = mock(Storage.class);
      when(storage.createQuery()).thenReturn(new MockQuery());
      when(storage.findAll(expectedQuery)).thenReturn(singleVMCursor);
      return storage;
  }

    @Test
    public void testMultiVMs() {
        Storage storage = setupStorageForMultiVM();
        VmInfoDAO dao = new VmInfoDAOImpl(storage);

        HostRef host = new HostRef("456", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, 123, "mainClass1"), new VmRef(host, 456, "mainClass2"));
    }

    private Storage setupStorageForMultiVM() {
      Query expectedQuery = new MockQuery()
          .from(VmInfoDAO.vmInfoCategory)
          .where(Key.AGENT_ID, Criteria.EQUALS, "456");

      Chunk vm1 = new Chunk(VmInfoDAO.vmInfoCategory, false);
      vm1.put(Key.VM_ID, 123);
      vm1.put(VmInfoDAO.mainClassKey, "mainClass1");

      Chunk vm2 = new Chunk(VmInfoDAO.vmInfoCategory, false);
      vm2.put(Key.VM_ID, 456);
      vm2.put(VmInfoDAO.mainClassKey, "mainClass2");

      Cursor multiVMsCursor = mock(Cursor.class);
      when(multiVMsCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
      when(multiVMsCursor.next()).thenReturn(vm1).thenReturn(vm2);

      Storage storage = mock(Storage.class);
      when(storage.createQuery()).thenReturn(new MockQuery());
      when(storage.findAll(expectedQuery)).thenReturn(multiVMsCursor);
      return storage;
  }

    private void assertCollection(Collection<VmRef> vms, VmRef... expectedVMs) {
        assertEquals(expectedVMs.length, vms.size());
        for (VmRef expectedVM : expectedVMs) {
            assertTrue(vms.contains(expectedVM));
        }
    }

    @Test
    public void testGetCount() {
        Storage storage = mock(Storage.class);
        when(storage.getCount(any(Category.class))).thenReturn(5L);
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        Long count = dao.getCount();
        assertEquals((Long) 5L, count);
    }

    @Test
    public void testPutVmInfo() {

        Storage storage = mock(Storage.class);
        VmInfo info = new VmInfo(vmId, startTime, stopTime, jVersion, jHome,
                mainClass, commandLine, vmName, vmInfo, vmVersion, vmArgs,
                props, env, libs);
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        dao.putVmInfo(info);

        verify(storage).putPojo(VmInfoDAO.vmInfoCategory, true, info);
    }

    @Test
    public void testPutVmStoppedTime() {
        Storage storage = mock(Storage.class);
        VmInfoDAO dao = new VmInfoDAOImpl(storage);
        dao.putVmStoppedTime(vmId, stopTime);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).updateChunk(arg.capture());
        Chunk chunk = arg.getValue();

        assertEquals(VmInfoDAO.vmInfoCategory, chunk.getCategory());
        assertEquals((Integer) vmId, chunk.get(Key.VM_ID));
        assertEquals((Long) stopTime, chunk.get(VmInfoDAO.stopTimeKey));
    }
}
