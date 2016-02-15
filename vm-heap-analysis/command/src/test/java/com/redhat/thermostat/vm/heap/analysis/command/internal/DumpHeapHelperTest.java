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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

public class DumpHeapHelperTest {

    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDao;
    private DumpHeapHelper cmd;
    private VmId vmId;
    private AgentId agentId;
    private AgentInformation agentInfo;
    private RequestQueue reqQueue;
    private Runnable heapDumpCompleteAction;
    private Runnable heapDumpFailedAction;

    @Before
    public void setUp() {
        reqQueue = mock(RequestQueue.class);

        agentInfoDao = mock(AgentInfoDAO.class);

        cmd = new DumpHeapHelper();

        agentInfo = mock(AgentInformation.class);
        when(agentInfo.getRequestQueueAddress()).thenReturn(new InetSocketAddress("test", 123));

        agentId = mock(AgentId.class);
        when(agentInfoDao.getAgentInformation(agentId)).thenReturn(agentInfo);

        vmId = mock(VmId.class);
        when(vmId.get()).thenReturn("vmId");

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.getVmPid()).thenReturn(123);

        vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmInfo(vmId)).thenReturn(vmInfo);
        
        heapDumpCompleteAction = mock(Runnable.class);
        heapDumpFailedAction = mock(Runnable.class);
    }

    @After
    public void tearDown() {
        heapDumpCompleteAction = null;
        vmId = null;
        cmd = null;
        reqQueue = null;
    }

    @Test
    public void testExecute() {
        cmd.execute(vmInfoDAO, agentInfoDao, agentId, vmId, reqQueue, heapDumpCompleteAction, heapDumpFailedAction);

        ArgumentCaptor<Request> reqArg = ArgumentCaptor.forClass(Request.class);
        verify(reqQueue).putRequest(reqArg.capture());
        Request req = reqArg.getValue();
        assertEquals("com.redhat.thermostat.vm.heap.analysis.agent.internal.HeapDumpReceiver", req.getReceiver());
        verifyClassExists(req.getReceiver());
        assertEquals(RequestType.RESPONSE_EXPECTED, req.getType());
        assertEquals("vmId", req.getParameter("vmId"));
        assertEquals("123", req.getParameter("vmPid"));
        assertEquals(new InetSocketAddress("test", 123), req.getTarget());

        Collection<RequestResponseListener> ls = req.getListeners();
        for (RequestResponseListener l : ls) {
            l.fireComplete(req, new Response(ResponseType.OK));
        }
        verify(heapDumpCompleteAction).run();
        verify(heapDumpFailedAction, times(0)).run();
    }

    @Test
    public void testExecuteFailure() {

        cmd.execute(vmInfoDAO, agentInfoDao, agentId, vmId, reqQueue, heapDumpCompleteAction, heapDumpFailedAction);

        ArgumentCaptor<Request> reqArg = ArgumentCaptor.forClass(Request.class);
        verify(reqQueue).putRequest(reqArg.capture());
        Request req = reqArg.getValue();
        assertEquals("com.redhat.thermostat.vm.heap.analysis.agent.internal.HeapDumpReceiver", req.getReceiver());
        verifyClassExists(req.getReceiver());
        assertEquals(RequestType.RESPONSE_EXPECTED, req.getType());
        assertEquals("vmId", req.getParameter("vmId"));
        assertEquals("123", req.getParameter("vmPid"));
        assertEquals(new InetSocketAddress("test", 123), req.getTarget());

        Collection<RequestResponseListener> ls = req.getListeners();
        for (RequestResponseListener l : ls) {
            l.fireComplete(req, new Response(ResponseType.ERROR));
        }
        verify(heapDumpCompleteAction, times(0)).run();
        verify(heapDumpFailedAction).run();
    }

    private void verifyClassExists(String receiver) {
        try {
            Class.forName(receiver);
        } catch (ClassNotFoundException e) {
            throw new AssertionError();
        }
    }

}

