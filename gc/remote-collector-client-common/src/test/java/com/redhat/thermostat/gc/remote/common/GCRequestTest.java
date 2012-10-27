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

package com.redhat.thermostat.gc.remote.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.gc.remote.common.command.GCCommand;

public class GCRequestTest {

    private AgentInfoDAO agentDAO;
    private RequestQueue queue;
    private VmRef vm;

    private GCRequest gcRequest;
    private Request request;
    
    @Before
    public void setUp() {
        agentDAO = mock(AgentInfoDAO.class);
        vm = mock(VmRef.class);

        request = mock(Request.class);
        
        HostRef ref = mock(HostRef.class);        
        when(vm.getAgent()).thenReturn(ref);
        when(vm.getIdString()).thenReturn("123456");

        AgentInformation info = mock(AgentInformation.class);
        when(info.getConfigListenAddress()).thenReturn("0.0.42.42:42");
        
        when(agentDAO.getAgentInformation(ref)).thenReturn(info);
        
        queue = mock(RequestQueue.class);
    }
    
    @Test
    public void testSendGCRequestToAgent() {
        
        final boolean [] results = new boolean [3];
        gcRequest = new GCRequest(queue) {
            @Override
            Request createRequest(InetSocketAddress target) {
                results[0] = true;
                if (target.getHostString().equals("0.0.42.42")) {
                    results[1] = true;
                }
                if (target.getPort() == 42) {
                    results[2] = true;
                }
                
                return request;
            }
        };
        
        gcRequest.sendGCRequestToAgent(vm, agentDAO);
        verify(vm).getAgent();
        verify(vm).getIdString();
        
        assertTrue(results[0]);
        assertTrue(results[1]);
        assertTrue(results[2]);
        
        verify(request).setReceiver(GCCommand.RECEIVER);
        verify(request).setParameter(GCCommand.class.getName(), GCCommand.REQUEST_GC.name());
        verify(request).setParameter(GCCommand.VM_ID, "123456");
        
        verify(queue).putRequest(request);
    }
}
