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

package com.redhat.thermostat.vm.jmx.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.vm.jmx.client.core.internal.JmxToggleNotificationRequest.JmxToggleResponseListener;
import com.redhat.thermostat.vm.jmx.client.core.internal.JmxToggleNotificationRequest.JmxToggleResponseListenerFactory;
import com.redhat.thermostat.vm.jmx.common.JmxCommand;

public class JmxToggleNotificationRequestTest {

    private static final String HOST = "example.com";
    private static final int PORT = 0;

    private RequestQueue queue;
    private ArgumentCaptor<Request> requestCaptor;
    private HostRef host;
    private VmRef vm;
    private AgentInfoDAO agentDAO;
    private AgentInformation agentInfo;
    private JmxToggleResponseListenerFactory factory;
    private JmxToggleResponseListener listener;
    private Runnable successAction;
    private Runnable failureAction;
    private JmxToggleNotificationRequest toggleReq;

    @Before
    public void setUp() {
        queue = mock(RequestQueue.class);
        requestCaptor = ArgumentCaptor.forClass(Request.class);

        host = mock(HostRef.class);
        vm = mock(VmRef.class);
        when(vm.getHostRef()).thenReturn(host);

        agentDAO = mock(AgentInfoDAO.class);

        agentInfo = mock(AgentInformation.class);
        when(agentInfo.getConfigListenAddress()).thenReturn(HOST + ":" + PORT);
        when(agentDAO.getAgentInformation(host)).thenReturn(agentInfo);
        
        factory = mock(JmxToggleResponseListenerFactory.class);
        listener = mock(JmxToggleResponseListener.class);
        successAction = mock(Runnable.class);
        failureAction = mock(Runnable.class);
        when(factory.createListener(successAction, failureAction)).thenReturn(listener);
        
        toggleReq = new JmxToggleNotificationRequest(queue, agentDAO, successAction, failureAction);
    }

    @Test
    public void testEnableNotificationsSuccess() {
        answerSuccess();
        toggleReq.sendEnableNotificationsRequestToAgent(vm, true);

        verify(queue).putRequest(requestCaptor.capture());

        Request req = requestCaptor.getValue();

        assertEquals(new InetSocketAddress(HOST, PORT), req.getTarget());
        assertEquals(JmxToggleNotificationRequest.CMD_CHANNEL_ACTION_NAME, req.getParameter(Request.ACTION));
        assertEquals(JmxCommand.RECEIVER, req.getReceiver());
        assertEquals(String.valueOf(vm.getPid()), req.getParameter(JmxCommand.VM_PID));
        assertEquals(vm.getVmId(), req.getParameter(JmxCommand.VM_ID));

        assertEquals(JmxCommand.ENABLE_JMX_NOTIFICATIONS.name(), req.getParameter(JmxCommand.class.getName()));
        
        verify(successAction).run();
        verify(failureAction, never()).run();
    }
    
    @Test
    public void testEnableNotificationsFailure() {
        answerFailure();
        toggleReq.sendEnableNotificationsRequestToAgent(vm, true);

        verify(successAction, never()).run();
        verify(failureAction).run();
    }

    @Test
    public void testDisableNotificationsSuccess() {
        answerSuccess();
        toggleReq.sendEnableNotificationsRequestToAgent(vm, false);

        verify(queue).putRequest(requestCaptor.capture());

        Request req = requestCaptor.getValue();

        assertEquals(new InetSocketAddress(HOST, PORT), req.getTarget());
        assertEquals(JmxToggleNotificationRequest.CMD_CHANNEL_ACTION_NAME, req.getParameter(Request.ACTION));
        assertEquals(JmxCommand.RECEIVER, req.getReceiver());
        assertEquals(String.valueOf(vm.getPid()), req.getParameter(JmxCommand.VM_PID));
        assertEquals(vm.getVmId(), req.getParameter(JmxCommand.VM_ID));

        assertEquals(JmxCommand.DISABLE_JMX_NOTIFICATIONS.name(), req.getParameter(JmxCommand.class.getName()));
        
        verify(successAction).run();
        verify(failureAction, never()).run();
    }
    
    @Test
    public void testDisableNotificationsFailure() {
        answerFailure();
        toggleReq.sendEnableNotificationsRequestToAgent(vm, false);

        verify(successAction, never()).run();
        verify(failureAction).run();
    }
    
    private void answerSuccess() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Fire complete OK
                Request req = (Request) invocation.getArguments()[0];
                Collection<RequestResponseListener> listeners = req.getListeners();
                assertEquals(1, listeners.size());
                RequestResponseListener listener = listeners.iterator().next();
                listener.fireComplete(req, new Response(ResponseType.OK));
                return null;
            }
        }).when(queue).putRequest(any(Request.class));
    }

    private void answerFailure() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Fire complete ERROR
                Request req = (Request) invocation.getArguments()[0];
                Collection<RequestResponseListener> listeners = req.getListeners();
                assertEquals(1, listeners.size());
                RequestResponseListener listener = listeners.iterator().next();
                listener.fireComplete(req, new Response(ResponseType.ERROR));
                return null;
            }
        }).when(queue).putRequest(any(Request.class));
    }
}
