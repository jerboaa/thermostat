/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.client.core.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.VmIssue;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

public class VmGcIssueDiagnoserTest {

    private static final String AGENT_ID = "agent-id";
    private static final String VM_ID = "vm-id";

    private AgentId agentId;
    private VmId vmId;

    private Clock clock;
    private VmGcStatDAO gcDao;

    private VmGcIssueDiagnoser diagnoser;

    @Before
    public void setup() {
        agentId = new AgentId(AGENT_ID);
        vmId = new VmId(VM_ID);

        clock = mock(Clock.class);

        gcDao = mock(VmGcStatDAO.class);

        diagnoser = new VmGcIssueDiagnoser(clock, gcDao);
    }

    @Test
    public void verifyNoDataMeansNoIssues() throws Exception {
        Collection<VmIssue> issues = diagnoser.diagnoseIssue(agentId, vmId);
        assertTrue(issues.isEmpty());
    }

    @Test
    public void verifyOnlyOneDataPointMeansNoIssues() throws Exception {
        long TIMESTAMP = 1;
        String COLLECTOR = "some-collector";
        long RUN_COUNT = 1;
        long WALL_TIME = 1;

        VmGcStat stat = new VmGcStat(AGENT_ID, VM_ID, TIMESTAMP, COLLECTOR, RUN_COUNT, WALL_TIME);

        when(gcDao.getLatestVmGcStats(eq(agentId), eq(vmId), anyLong()))
            .thenReturn(Collections.singletonList(stat));
        Collection<VmIssue> issues = diagnoser.diagnoseIssue(agentId, vmId);

        assertTrue(issues.isEmpty());
    }

    @Test
    public void verifyGoodDataMeansNoIssues() throws Exception {
        String COLLECTOR = "some-collector";
        long TIMESTAMP_1 = System.currentTimeMillis();
        long TIMESTAMP_2 = TIMESTAMP_1 + TimeUnit.SECONDS.toMillis(1);
        long RUN_COUNT_1 = 1;
        long RUN_COUNT_2 = 2;
        long WALL_TIME_1 = 1;
        long WALL_TIME_2 = WALL_TIME_1 + TimeUnit.MILLISECONDS.toMicros(10);

        VmGcStat stat1 = new VmGcStat(AGENT_ID, VM_ID, TIMESTAMP_1, COLLECTOR, RUN_COUNT_1, WALL_TIME_1);
        VmGcStat stat2 = new VmGcStat(AGENT_ID, VM_ID, TIMESTAMP_2, COLLECTOR, RUN_COUNT_2, WALL_TIME_2);

        when(gcDao.getLatestVmGcStats(eq(agentId), eq(vmId), anyLong()))
            .thenReturn(Arrays.asList(stat1, stat2));
        Collection<VmIssue> issues = diagnoser.diagnoseIssue(agentId, vmId);

        assertTrue(issues.isEmpty());
    }

    @Test
    public void verifyLongGcMeansIssues() throws Exception {
        String COLLECTOR = "some-collector";
        long TIMESTAMP_1 = System.currentTimeMillis();
        long TIMESTAMP_2 = TIMESTAMP_1 + TimeUnit.SECONDS.toMillis(1);
        long RUN_COUNT_1 = 1;
        long RUN_COUNT_2 = 2;
        long WALL_TIME_1 = 1;
        long WALL_TIME_2 = WALL_TIME_1 + TimeUnit.SECONDS.toMicros(1);

        VmGcStat stat1 = new VmGcStat(AGENT_ID, VM_ID, TIMESTAMP_1, COLLECTOR, RUN_COUNT_1, WALL_TIME_1);
        VmGcStat stat2 = new VmGcStat(AGENT_ID, VM_ID, TIMESTAMP_2, COLLECTOR, RUN_COUNT_2, WALL_TIME_2);

        when(gcDao.getLatestVmGcStats(eq(agentId), eq(vmId), anyLong()))
            .thenReturn(Arrays.asList(stat1, stat2));
        Collection<VmIssue> issues = diagnoser.diagnoseIssue(agentId, vmId);

        assertFalse(issues.isEmpty());
    }

}
