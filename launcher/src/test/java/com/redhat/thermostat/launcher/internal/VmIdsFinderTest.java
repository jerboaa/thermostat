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

package com.redhat.thermostat.launcher.internal;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.testutils.StubBundleContext;
import org.junit.Before;
import org.junit.Test;

public class VmIdsFinderTest {

    private VmIdsFinder vmIdsFinder;
    private final String id0 = "pequ-14as-54yt";
    private final String id1 = "412345-56789";
    private final String id2 = "111111-22222";
    private final String id3 = "98765-543210";
    private final String id4 = "abcdef-01234564-848156";
    private final String id5 = "456-879-4512";
    private final String id6 = "4101-1010-0111";
    private final String mainClass0 = "com.redhat.thermostat.main";
    private final String mainClass1 = "com.redhat.thermostat.blue.launcher.main";
    private final String mainClass2 = "com.redhat.thermostat.vmIdsFinder.main";
    private final String mainClass3 = "com.redhat.thermostat.distribution.main";
    private final String mainClass4 = "com.redhat.thermostat.orange.main";
    private final String mainClass5 = "com.redhat.thermostat.look.search.main";
    private final String mainClass6 = "com.redhat.thermostat.gui.chartspanel.chart.main";
    private final VmInfo.AliveStatus aliveStatus0 = VmInfo.AliveStatus.RUNNING;
    private final VmInfo.AliveStatus aliveStatus1 = VmInfo.AliveStatus.RUNNING;
    private final VmInfo.AliveStatus aliveStatus2 = VmInfo.AliveStatus.EXITED;
    private final VmInfo.AliveStatus aliveStatus3 = VmInfo.AliveStatus.EXITED;
    private final VmInfo.AliveStatus aliveStatus4 = VmInfo.AliveStatus.UNKNOWN;
    private final VmInfo.AliveStatus aliveStatus5 = VmInfo.AliveStatus.RUNNING;
    private final VmInfo.AliveStatus aliveStatus6 = VmInfo.AliveStatus.UNKNOWN;

    private VmIdsFinder vmIdsFinderWithOnlyOneVm;

    @Before
    public void setupVmIdsFinder() {
        StubBundleContext context = new StubBundleContext();
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        VmInfoDAO vmsInfoDAO = mock(VmInfoDAO.class);
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(HostInfoDAO.class, hostInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmsInfoDAO, null);

        vmIdsFinder = new VmIdsFinder(context);

        Collection<HostRef> hostRefs = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        hostRefs.add(hostRef1);
        hostRefs.add(hostRef2);

        when(hostInfoDAO.getHosts()).thenReturn(hostRefs);
        AgentInformation agentInformation1 = mock(AgentInformation.class);
        when(agentInfoDAO.getAgentInformation(hostRef1)).thenReturn(agentInformation1);
        AgentInformation agentInformation2 = mock(AgentInformation.class);
        when(agentInfoDAO.getAgentInformation(hostRef2)).thenReturn(agentInformation2);

        Collection<VmRef> vms1 = new ArrayList<>();
        VmRef vm0 = mock(VmRef.class);
        VmRef vm1 = mock(VmRef.class);
        VmRef vm2 = mock(VmRef.class);
        VmRef vm3 = mock(VmRef.class);
        vms1.add(vm0);
        vms1.add(vm1);
        vms1.add(vm2);
        vms1.add(vm3);
        VmInfo info0 = mock(VmInfo.class);
        VmInfo info1 = mock(VmInfo.class);
        VmInfo info2 = mock(VmInfo.class);
        VmInfo info3 = mock(VmInfo.class);
        when(vmsInfoDAO.getVmInfo(vm0)).thenReturn(info0);
        when(vmsInfoDAO.getVmInfo(vm1)).thenReturn(info1);
        when(vmsInfoDAO.getVmInfo(vm2)).thenReturn(info2);
        when(vmsInfoDAO.getVmInfo(vm3)).thenReturn(info3);

        Collection<VmRef> vms2 = new ArrayList<>();
        VmRef vm4 = mock(VmRef.class);
        VmRef vm5 = mock(VmRef.class);
        VmRef vm6 = mock(VmRef.class);
        vms2.add(vm4);
        vms2.add(vm5);
        vms2.add(vm6);
        VmInfo info4 = mock(VmInfo.class);
        VmInfo info5 = mock(VmInfo.class);
        VmInfo info6 = mock(VmInfo.class);
        when(vmsInfoDAO.getVmInfo(vm4)).thenReturn(info4);
        when(vmsInfoDAO.getVmInfo(vm5)).thenReturn(info5);
        when(vmsInfoDAO.getVmInfo(vm6)).thenReturn(info6);

        when(info0.getVmId()).thenReturn(id0);
        when(info1.getVmId()).thenReturn(id1);
        when(info2.getVmId()).thenReturn(id2);
        when(info3.getVmId()).thenReturn(id3);
        when(info4.getVmId()).thenReturn(id4);
        when(info5.getVmId()).thenReturn(id5);
        when(info6.getVmId()).thenReturn(id6);

        when(info0.getMainClass()).thenReturn(mainClass0);
        when(info1.getMainClass()).thenReturn(mainClass1);
        when(info2.getMainClass()).thenReturn(mainClass2);
        when(info3.getMainClass()).thenReturn(mainClass3);
        when(info4.getMainClass()).thenReturn(mainClass4);
        when(info5.getMainClass()).thenReturn(mainClass5);
        when(info6.getMainClass()).thenReturn(mainClass6);

        when(info0.isAlive(agentInformation1)).thenReturn(aliveStatus0);
        when(info1.isAlive(agentInformation1)).thenReturn(aliveStatus1);
        when(info2.isAlive(agentInformation1)).thenReturn(aliveStatus2);
        when(info3.isAlive(agentInformation1)).thenReturn(aliveStatus3);

        when(info4.isAlive(agentInformation2)).thenReturn(aliveStatus4);
        when(info5.isAlive(agentInformation2)).thenReturn(aliveStatus5);
        when(info6.isAlive(agentInformation2)).thenReturn(aliveStatus6);

        when(vmsInfoDAO.getVMs(hostRef1)).thenReturn(vms1);
        when(vmsInfoDAO.getVMs(hostRef2)).thenReturn(vms2);

        setupVmIdsFinderWithOnlyOneVm();
    }

    private void setupVmIdsFinderWithOnlyOneVm() {
        StubBundleContext context = new StubBundleContext();
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        VmInfoDAO vmsInfoDAO = mock(VmInfoDAO.class);
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(HostInfoDAO.class, hostInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmsInfoDAO, null);

        vmIdsFinderWithOnlyOneVm = new VmIdsFinder(context);

        Collection<HostRef> hostRefs = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        hostRefs.add(hostRef1);

        when(hostInfoDAO.getHosts()).thenReturn(hostRefs);
        AgentInformation agentInformation1 = mock(AgentInformation.class);
        when(agentInfoDAO.getAgentInformation(hostRef1)).thenReturn(agentInformation1);

        Collection<VmRef> vms1 = new ArrayList<>();
        VmRef vm1 = mock(VmRef.class);
        vms1.add(vm1);
        VmInfo info1 = mock(VmInfo.class);
        when(vmsInfoDAO.getVmInfo(vm1)).thenReturn(info1);

        when(info1.getVmId()).thenReturn(id0);
        when(info1.getMainClass()).thenReturn(mainClass0);
        when(info1.isAlive(agentInformation1)).thenReturn(aliveStatus0);

        when(vmsInfoDAO.getVMs(hostRef1)).thenReturn(vms1);
        AgentInformation agentInfo0 = mock(AgentInformation.class);
        agentInfo0.setAgentId(id0);
    }

    @Test
    public void testFindIds() {

        List<CompletionInfo> result = vmIdsFinder.findIds();
        assertEquals(7, result.size());
        assertEquals(formatExpected(id0, mainClass0, aliveStatus0), result.get(0).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id1, mainClass1, aliveStatus1), result.get(1).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id2, mainClass2, aliveStatus2), result.get(2).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id3, mainClass3, aliveStatus3), result.get(3).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id4, mainClass4, aliveStatus4), result.get(4).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id5, mainClass5, aliveStatus5), result.get(5).getCompletionWithUserVisibleText());
        assertEquals(formatExpected(id6, mainClass6, aliveStatus6), result.get(6).getCompletionWithUserVisibleText());
    }

    @Test
    public void testFindsIdsWithOnlyOneVm() {
        List<CompletionInfo> result = vmIdsFinderWithOnlyOneVm.findIds();
        assertEquals(1, result.size());
        assertEquals(formatExpected(id0, mainClass0, aliveStatus0), result.get(0).getCompletionWithUserVisibleText());
    }

    private String formatExpected(String id, String mainClass, VmInfo.AliveStatus aliveStatus) {
        return id + "[" + mainClass + "](" + aliveStatus.toString() + ")";
    }
}
