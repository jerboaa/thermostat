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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class MatchContextTest {

    @Test
    public void testDefaultValuesAreNull() {
        MatchContext context = MatchContext.builder().build();
        assertThat(context.getAgentInfo(), is(equalTo(null)));
        assertThat(context.getHostInfo(), is(equalTo(null)));
        assertThat(context.getVmInfo(), is(equalTo(null)));
    }

    @Test
    public void testGetters() {
        AgentInformation agentInfo = new AgentInformation();
        HostInfo hostInfo = new HostInfo();
        VmInfo vmInfo = new VmInfo();
        MatchContext context = MatchContext.builder()
                .agentInfo(agentInfo)
                .hostInfo(hostInfo)
                .vmInfo(vmInfo)
                .build();
        assertThat(context.getAgentInfo(), is(agentInfo));
        assertThat(context.getHostInfo(), is(hostInfo));
        assertThat(context.getVmInfo(), is(vmInfo));
    }

    @Test
    public void testEquals() {
        MatchContext defaultContext = MatchContext.builder().build();
        AgentInformation agentInfo = new AgentInformation();
        HostInfo hostInfo = new HostInfo();
        VmInfo vmInfo = new VmInfo();
        MatchContext customizedContext = MatchContext.builder()
                .agentInfo(agentInfo)
                .hostInfo(hostInfo)
                .vmInfo(vmInfo)
                .build();
        MatchContext customizedContext2 = MatchContext.builder()
                .agentInfo(agentInfo)
                .hostInfo(hostInfo)
                .vmInfo(vmInfo)
                .build();
        assertThat(customizedContext.equals(customizedContext2), is(true));
        assertThat(customizedContext2.equals(customizedContext), is(true));
        assertThat(customizedContext.equals(defaultContext), is(false));
        assertThat(customizedContext.equals(new Object()), is(false));
        assertThat(customizedContext.equals(null), is(false));
    }

    @Test
    public void testHashCode() {
        MatchContext defaultContext = MatchContext.builder().build();
        AgentInformation agentInfo = new AgentInformation();
        HostInfo hostInfo = new HostInfo();
        VmInfo vmInfo = new VmInfo();
        MatchContext customizedContext = MatchContext.builder()
                .agentInfo(agentInfo)
                .hostInfo(hostInfo)
                .vmInfo(vmInfo)
                .build();
        MatchContext customizedContext2 = MatchContext.builder()
                .agentInfo(agentInfo)
                .hostInfo(hostInfo)
                .vmInfo(vmInfo)
                .build();
        assertThat(defaultContext.hashCode(), is(not(equalTo(customizedContext.hashCode()))));
        assertThat(customizedContext.hashCode(), is(equalTo(customizedContext2.hashCode())));
    }

}
