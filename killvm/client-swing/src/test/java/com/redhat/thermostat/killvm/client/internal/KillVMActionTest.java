/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.killvm.common.KillVMRequest;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class KillVMActionTest {

    @Test
    public void killVMFilterDiscardHost() {
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);

        VmRef matching = mock(VmRef.class);
        HostRef notMatching = mock(HostRef.class);

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfoDao.getVmInfo(matching)).thenReturn(vmInfo);

        KillVMRequest request = mock(KillVMRequest.class);
        RequestResponseListener listener = mock(RequestResponseListener.class);

        KillVMAction action = new KillVMAction(agentDao, vmInfoDao, request, listener);

        Filter<Ref> filter = action.getFilter();

        assertFalse(filter.matches(notMatching));
        
        when(vmInfo.isAlive()).thenReturn(true);
        assertTrue(filter.matches(matching));

        when(vmInfo.isAlive()).thenReturn(false);
        assertFalse(filter.matches(matching));
    }
    
    @Test
    public void killVMFilterOnlyMatchesLiveVMs() {
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);

        VmRef matching = mock(VmRef.class);

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfoDao.getVmInfo(matching)).thenReturn(vmInfo);

        KillVMRequest request = mock(KillVMRequest.class);
        RequestResponseListener listener = mock(RequestResponseListener.class);

        KillVMAction action = new KillVMAction(agentDao, vmInfoDao, request, listener);

        Filter<Ref> filter = action.getFilter();

        when(vmInfo.isAlive()).thenReturn(true);
        assertTrue(filter.matches(matching));

        when(vmInfo.isAlive()).thenReturn(false);
        assertFalse(filter.matches(matching));
    }

    @Test
    public void canQueueKillRequest() {
        VmRef ref = mock(VmRef.class);
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);

        HostRef hostRef = mock(HostRef.class);
        when(ref.getHostRef()).thenReturn(hostRef);

        AgentId agentId = new AgentId("a1b2c3");
        when(hostRef.getAgentId()).thenReturn(agentId.get());

        int pid = 0;

        RequestResponseListener agentResponseListener = mock(RequestResponseListener.class);

        KillVMRequest request = mock(KillVMRequest.class);
        KillVMAction action = new KillVMAction(agentDao, vmInfoDao, request, agentResponseListener);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(request).sendKillVMRequestToAgent(agentId, pid, agentDao, agentResponseListener);

        action.execute(ref);

        assertTrue(complete[0]);
    }

}

