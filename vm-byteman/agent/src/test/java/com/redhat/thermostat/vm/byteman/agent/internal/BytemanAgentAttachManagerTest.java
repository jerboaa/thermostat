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

package com.redhat.thermostat.vm.byteman.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.utils.ProcessChecker;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.agent.internal.BytemanAgentAttachManager.SubmitHelper;
import com.redhat.thermostat.vm.byteman.agent.internal.BytemanAttacher.BtmInstallHelper;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;

public class BytemanAgentAttachManagerTest {
    
    private static final VmId SOME_VM_ID = new VmId("some-vm-id");
    private static final int SOME_VM_PID = 99910;
    private static final String SOME_AGENT_ID = "some-agent-id";
    private static final BytemanAgentInfo SOME_SUCCESS_BYTEMAN_INFO = new BytemanAgentInfo(SOME_VM_PID, 3344, null, SOME_VM_ID.get(), SOME_AGENT_ID, false);
    
    private BytemanAgentAttachManager manager;
    private IPCEndpointsManager ipcManager;
    private VmBytemanDAO vmBytemanDao;
    private SubmitHelper submit;
    private BytemanAttacher bytemanAttacher;
    
    @Before
    public void setup() {
        ipcManager = mock(IPCEndpointsManager.class);
        vmBytemanDao = mock(VmBytemanDAO.class);
        submit = mock(SubmitHelper.class);
        bytemanAttacher = mock(BytemanAttacher.class);
        WriterID writerId = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(SOME_AGENT_ID);
        manager = new BytemanAgentAttachManager(bytemanAttacher, ipcManager, vmBytemanDao, submit, writerId);
    }
    
    @After
    public void tearDown() {
        BytemanAgentAttachManager.helperJars = null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void canAttachAgentToVmStartIPCandAddsStatus() {
        String workingVmId = "working-vm-id";
        int vmPid = 1001;
        String agentId = "working-agent-id";
        int listenPort = 9881;
        BytemanAgentInfo bytemanAgentInfo = new BytemanAgentInfo(vmPid, listenPort, null, workingVmId, agentId, false);
        when(bytemanAttacher.attach(workingVmId, vmPid, agentId)).thenReturn(bytemanAgentInfo);
        
        // mock that installing of helper jars works
        when(submit.addJarsToSystemClassLoader(any(List.class), any(BytemanAgentInfo.class))).thenReturn(true);
        
        VmId vmId = new VmId(workingVmId);
        WriterID writerId = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(agentId);
        manager.setWriterId(writerId);
        VmBytemanStatus bytemanStatus = manager.attachBytemanToVm(vmId, vmPid);
        VmSocketIdentifier socketId = new VmSocketIdentifier(workingVmId, vmPid, agentId);
        
        // IPC endpoint must be started
        verify(ipcManager).startIPCEndpoint(eq(socketId), isA(BytemanMetricsReceiver.class));
        
        // Status should have been updated/inserted
        ArgumentCaptor<VmBytemanStatus> statusCaptor = ArgumentCaptor.forClass(VmBytemanStatus.class);
        verify(vmBytemanDao).addOrReplaceBytemanStatus(statusCaptor.capture());
        VmBytemanStatus capturedStatus = statusCaptor.getValue();
        assertNotNull(capturedStatus);
        assertEquals(workingVmId, capturedStatus.getVmId());
        assertEquals(agentId, capturedStatus.getAgentId());
        assertEquals(listenPort, capturedStatus.getListenPort());
        assertNull(capturedStatus.getRule());
        assertTrue(capturedStatus.getTimeStamp() > 0);
        
        // Helper jars must have been added to classpath
        verify(submit).addJarsToSystemClassLoader(eq((List)null), eq(bytemanAgentInfo));
        
        assertEquals(listenPort, bytemanStatus.getListenPort());
    }
    
    @Test
    public void failureToAttachDoesNotStartIPC() throws Exception {
        BytemanAttacher failAttacher = getFailureAttacher();
        manager.setAttacher(failAttacher);
        VmBytemanStatus status = manager.attachBytemanToVm(SOME_VM_ID, SOME_VM_PID);
        verify(ipcManager, never()).startIPCEndpoint(any(VmSocketIdentifier.class), any(ThermostatIPCCallbacks.class));
        assertNull(status);
    }

    @Test
    public void failureToAttachDoesNotInsertStatus() throws Exception {
        BytemanAttacher failAttacher = getFailureAttacher();
        manager.setAttacher(failAttacher);
        VmBytemanStatus status = manager.attachBytemanToVm(SOME_VM_ID, SOME_VM_PID);
        verify(vmBytemanDao, never()).addOrReplaceBytemanStatus(any(VmBytemanStatus.class));
        assertNull(status);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void successfulAttachAddsHelperJars() {
        // mock that installing of helper jars works
        when(submit.addJarsToSystemClassLoader(any(List.class), any(BytemanAgentInfo.class))).thenReturn(true);
        
        when(bytemanAttacher.attach(SOME_VM_ID.get(), SOME_VM_PID, SOME_AGENT_ID)).thenReturn(SOME_SUCCESS_BYTEMAN_INFO);
        manager.attachBytemanToVm(SOME_VM_ID, SOME_VM_PID);
        verify(submit).addJarsToSystemClassLoader(any(List.class), any(BytemanAgentInfo.class));
    }
    
    @Test
    public void canGetListOfJarsForBytemanHelper() {
        String parent = "/foo";
        File file = mock(File.class);
        File[] mockFiles = new File[7];
        for (int i = 0; i < 7; i++) {
            mockFiles[i] = getFileMockWithName(parent, "test-file" + i + ".jar");
        }
        when(file.listFiles()).thenReturn(mockFiles);
        List<String> jars = BytemanAgentAttachManager.initListOfHelperJars(file);
        assertEquals(7, jars.size());
        for (int i = 0; i < 7; i++) {
            assertEquals("/foo/test-file" + i + ".jar", jars.get(i));
        }
    }
    
    @SuppressWarnings("unchecked")
    private BytemanAttacher getFailureAttacher() throws Exception {
        CommonPaths paths = mock(CommonPaths.class);
        BtmInstallHelper failInstaller = mock(BtmInstallHelper.class);
        when(failInstaller.install(any(String.class),
                                   any(boolean.class),
                                   any(boolean.class),
                                   any(String.class),
                                   any(int.class),
                                   any(String[].class))).thenThrow(IOException.class);
        when(paths.getUserIPCConfigurationFile()).thenReturn(mock(File.class));
        ProcessChecker processChecker = mock(ProcessChecker.class);
        BytemanAttacher failAttacher = new BytemanAttacher(failInstaller, processChecker, paths);
        return failAttacher;
    }

    private File getFileMockWithName(String parent, String name) {
        File f = mock(File.class);
        when(f.getAbsolutePath()).thenReturn(parent + "/" + name);
        return f;
    }
}
