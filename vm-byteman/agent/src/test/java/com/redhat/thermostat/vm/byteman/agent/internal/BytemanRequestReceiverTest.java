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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;

public class BytemanRequestReceiverTest {
    
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
        BytemanRequestReceiver receiver = createReceiver(submit, null, null);
        Response response = receiver.receive(BytemanRequest.create(mock(InetSocketAddress.class), new VmId("ignored"), RequestAction.UNLOAD_RULES, -1));
        assertEquals(ResponseType.OK, response.getType());
        verify(submit, never()).deleteAllRules();
    }
    
    @Test
    public void testUnLoadRulesWithExistingRules() throws Exception {
        Submit submit = mock(Submit.class);
        when(submit.getAllScripts()).thenReturn(Arrays.asList(mock(ScriptText.class)));
        WriterID writerId = mock(WriterID.class);
        String someAgentId = "some-agent-id";
        String someVmId = "some-vm-id";
        int someListenPort = 3333;
        VmBytemanDAO bytemanDao = mock(VmBytemanDAO.class);
        when(writerId.getWriterID()).thenReturn(someAgentId);
        BytemanRequestReceiver receiver = createReceiver(submit, writerId, bytemanDao);
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
        VmBytemanDAO bytemanDao = mock(VmBytemanDAO.class);
        WriterID wid = mock(WriterID.class);
        String writerId = "some-writer-id";
        String someVmId = "some-id";
        int listenPort = 333;
        when(wid.getWriterID()).thenReturn(writerId);
        BytemanRequestReceiver receiver = createReceiver(submit, wid, bytemanDao);
        String rule = "some-rule";
        ArgumentCaptor<VmBytemanStatus> statusCaptor = ArgumentCaptor.forClass(VmBytemanStatus.class);
        Response response = receiver.receive(BytemanRequest.create(mock(InetSocketAddress.class), new VmId(someVmId), RequestAction.LOAD_RULES, listenPort, rule));
        verify(bytemanDao).addOrReplaceBytemanStatus(statusCaptor.capture());
        VmBytemanStatus capturedStatus = statusCaptor.getValue();
        assertEquals(someVmId, capturedStatus.getVmId());
        assertEquals(writerId, capturedStatus.getAgentId());
        assertEquals(rule, capturedStatus.getRule());
        assertEquals(listenPort, capturedStatus.getListenPort());
        // verify no helper jars get added on rule submission
        verify(submit, times(0)).addJarsToSystemClassloader(any(List.class));
        verify(submit).addRulesFromResources(any(List.class));
        assertEquals(ResponseType.OK, response.getType());
    }

    private BytemanRequestReceiver createReceiver(final Submit submit, WriterID writerId, VmBytemanDAO dao) {
        BytemanRequestReceiver receiver = new BytemanRequestReceiver() {
            @Override 
            protected Submit getSubmit(int port) {
                return submit;
            }
            
        };
        receiver.bindVmBytemanDao(dao);
        receiver.bindWriterId(writerId);
        return receiver;
    }
    
}
