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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;

public class BytemanRequestReceiverTest {
    
    @After
    public void tearDown() {
        BytemanRequestReceiver.helperJars = null;
    }

    @Test
    public void testLoadRules() throws Exception {
        Submit submit = mock(Submit.class);
        doLoadRulesTest(submit);
        verify(submit, never()).deleteAllRules();
    }
    
    @Test
    public void testLoadRulesWithRulesExisting() throws Exception {
        Submit submit = mock(Submit.class);
        when(submit.getAllScripts()).thenReturn(Arrays.asList(mock(ScriptText.class)));
        doLoadRulesTest(submit);
        verify(submit).deleteAllRules();
    }
    
    @Test
    public void testUnLoadRulesWithNoExistingRules() throws Exception {
        Submit submit = mock(Submit.class);
        when(submit.getAllScripts()).thenReturn(Collections.<ScriptText>emptyList());
        CommonPaths paths = mock(CommonPaths.class);
        File helperRootFile = getHelperRootFile();
        when(paths.getSystemPluginRoot()).thenReturn(helperRootFile);
        BytemanRequestReceiver receiver = createReceiver(submit, null, paths, null);
        Response response = receiver.receive(BytemanRequest.create(mock(InetSocketAddress.class), new VmId("ignored"), RequestAction.UNLOAD_RULES, -1));
        assertEquals(ResponseType.OK, response.getType());
        verify(submit, never()).deleteAllRules();
    }
    
    @Test
    public void testUnLoadRulesWithExistingRules() throws Exception {
        Submit submit = mock(Submit.class);
        when(submit.getAllScripts()).thenReturn(Arrays.asList(mock(ScriptText.class)));
        CommonPaths paths = mock(CommonPaths.class);
        File helperRootFile = getHelperRootFile();
        when(paths.getSystemPluginRoot()).thenReturn(helperRootFile);
        WriterID writerId = mock(WriterID.class);
        String someAgentId = "some-agent-id";
        String someVmId = "some-vm-id";
        int someListenPort = 3333;
        VmBytemanDAO bytemanDao = mock(VmBytemanDAO.class);
        when(writerId.getWriterID()).thenReturn(someAgentId);
        BytemanRequestReceiver receiver = createReceiver(submit, writerId, paths, bytemanDao);
        ArgumentCaptor<VmBytemanStatus> statusCaptor = ArgumentCaptor.forClass(VmBytemanStatus.class);
        Response response = receiver.receive(BytemanRequest.create(mock(InetSocketAddress.class), new VmId(someVmId), RequestAction.UNLOAD_RULES, someListenPort));
        assertEquals(ResponseType.OK, response.getType());
        verify(submit).deleteAllRules();
        verify(bytemanDao).addOrReplaceBytemanStatus(statusCaptor.capture());
        VmBytemanStatus status = statusCaptor.getValue();
        assertEquals(someAgentId, status.getAgentId());
        assertEquals(someVmId, status.getVmId());
        assertEquals(someListenPort, status.getListenPort());
        assertNull(status.getRule());
    }
    
    @Test
    public void testReceiveWithBadAction() {
        Request badRequest = new Request(RequestType.RESPONSE_EXPECTED, mock(InetSocketAddress.class));
        badRequest.setParameter(BytemanRequest.ACTION_PARAM_NAME, Integer.toString(-1));
        BytemanRequestReceiver receiver = new BytemanRequestReceiver();
        Response response = receiver.receive(badRequest);
        assertEquals(ResponseType.ERROR, response.getType());
    }

    @SuppressWarnings("unchecked")
    private void doLoadRulesTest(Submit submit) throws Exception {
        File helperRootFile = getHelperRootFile();
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemPluginRoot()).thenReturn(helperRootFile);
        VmBytemanDAO bytemanDao = mock(VmBytemanDAO.class);
        WriterID wid = mock(WriterID.class);
        String writerId = "some-writer-id";
        String someVmId = "some-id";
        int listenPort = 333;
        when(wid.getWriterID()).thenReturn(writerId);
        BytemanRequestReceiver receiver = createReceiver(submit, wid, paths, bytemanDao);
        String rule = "some-rule";
        ArgumentCaptor<VmBytemanStatus> statusCaptor = ArgumentCaptor.forClass(VmBytemanStatus.class);
        Response response = receiver.receive(BytemanRequest.create(mock(InetSocketAddress.class), new VmId(someVmId), RequestAction.LOAD_RULES, listenPort, rule));
        verify(bytemanDao).addOrReplaceBytemanStatus(statusCaptor.capture());
        VmBytemanStatus capturedStatus = statusCaptor.getValue();
        assertEquals(someVmId, capturedStatus.getVmId());
        assertEquals(writerId, capturedStatus.getAgentId());
        assertEquals(rule, capturedStatus.getRule());
        assertEquals(listenPort, capturedStatus.getListenPort());
        List<String> expectedList = new ArrayList<>();
        expectedList.add(helperRootFile.getAbsolutePath() + File.separator + "vm-byteman" + File.separator + "plugin-libs" + File.separator + "thermostat-helper" + File.separator + "not-really-a-jar-file.jar");
        // Verify thermostat helper jars get added
        verify(submit).addJarsToSystemClassloader(eq(expectedList));
        verify(submit).addRulesFromResources(any(List.class));
        assertEquals(ResponseType.OK, response.getType());
    }

    private File getHelperRootFile() {
        URL rootFile = getClass().getResource("/byteman-helper-root");
        File helperRootFile = new File(rootFile.getFile());
        return helperRootFile;
    }
    
    private BytemanRequestReceiver createReceiver(final Submit submit, WriterID writerId, CommonPaths paths, VmBytemanDAO dao) {
        BytemanRequestReceiver receiver = new BytemanRequestReceiver() {
            @Override 
            protected Submit getSubmit(int port) {
                return submit;
            }
            
        };
        receiver.bindPaths(paths);
        receiver.bindVmBytemanDao(dao);
        receiver.bindWriterId(writerId);
        return receiver;
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
        List<String> jars = BytemanRequestReceiver.initListOfHelperJars(file);
        assertEquals(7, jars.size());
        for (int i = 0; i < 7; i++) {
            assertEquals("/foo/test-file" + i + ".jar", jars.get(i));
        }
    }

    private File getFileMockWithName(String parent, String name) {
        File f = mock(File.class);
        when(f.getAbsolutePath()).thenReturn(parent + "/" + name);
        return f;
    }
}
