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

package com.redhat.thermostat.killvm.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.Filter;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

public class KillVMActionTest {

    @Test
    public void killVMFilterOnlyMatchesLiveVMs() {
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);

        VmRef matching = mock(VmRef.class);

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfoDao.getVmInfo(matching)).thenReturn(vmInfo);

        RequestQueue queue = mock(RequestQueue.class);
        RequestResponseListener listener = mock(RequestResponseListener.class);

        KillVMAction action = new KillVMAction(agentDao, vmInfoDao, queue, listener);

        Filter<VmRef> filter = action.getFilter();

        when(vmInfo.isAlive()).thenReturn(true);
        assertTrue(filter.matches(matching));

        when(vmInfo.isAlive()).thenReturn(false);
        assertFalse(filter.matches(matching));
    }

    @Test
    public void canQueueKillRequest() {
        VmRef ref = mock(VmRef.class);
        HostRef hostref = mock(HostRef.class);
        when(ref.getAgent()).thenReturn(hostref);
        String agentAddress = "127.0.0.1:8888";

        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.getConfigListenAddress()).thenReturn(agentAddress);

        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAgentInformation(hostref)).thenReturn(agentInfo);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);

        RequestResponseListener agentResponseListener = mock(RequestResponseListener.class);

        RequestQueue queue = mock(RequestQueue.class);
        final Request req = mock(Request.class);
        KillVMAction action = new KillVMAction(agentDao, vmInfoDao, queue, agentResponseListener) {
            @Override
            Request getKillRequest(InetSocketAddress target) {
                return req;
            }
        };
        action.execute(ref);
        ArgumentCaptor<String> vmIdParamCaptor = ArgumentCaptor
                .forClass(String.class);
        verify(req).setParameter(vmIdParamCaptor.capture(), any(String.class));
        assertEquals("vm-id", vmIdParamCaptor.getValue());
        verify(req).addListener(agentResponseListener);
        ArgumentCaptor<String> receiverCaptor = ArgumentCaptor
                .forClass(String.class);
        verify(req).setReceiver(receiverCaptor.capture());
        assertEquals(
                "com.redhat.thermostat.killvm.agent.internal.KillVmReceiver",
                receiverCaptor.getValue());
        verify(queue).putRequest(req);
    }

}

