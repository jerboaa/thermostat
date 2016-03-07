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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;

import java.util.Objects;

public class MatchContext {

    private final HostInfo hostInfo;
    private final AgentInformation agentInfo;
    private final VmInfo vmInfo;

    private MatchContext(Builder builder) {
        this.hostInfo = builder.hostInfo;
        this.agentInfo = builder.agentInfo;
        this.vmInfo = builder.vmInfo;
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public AgentInformation getAgentInfo() {
        return agentInfo;
    }

    public VmInfo getVmInfo() {
        return vmInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MatchContext)) {
            return false;
        }

        MatchContext m = (MatchContext) o;
        return Objects.equals(hostInfo, m.hostInfo)
                && Objects.equals(agentInfo, m.agentInfo)
                && Objects.equals(vmInfo, m.vmInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostInfo, agentInfo, vmInfo);
    }

    @Override
    public String toString() {
        return "MatchContext{" +
                "hostInfo=" + hostInfo +
                ", agentInfo=" + agentInfo +
                ", vmInfo=" + vmInfo +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private HostInfo hostInfo;
        private AgentInformation agentInfo;
        private VmInfo vmInfo;

        private Builder() {
            hostInfo = null;
            agentInfo = null;
            vmInfo = null;
        }

        public Builder hostInfo(HostInfo hostInfo) {
            this.hostInfo = hostInfo;
            return this;
        }

        public Builder agentInfo(AgentInformation agentInfo) {
            this.agentInfo = agentInfo;
            return this;
        }

        public Builder vmInfo(VmInfo vmInfo) {
            this.vmInfo = vmInfo;
            return this;
        }

        public MatchContext build() {
            return new MatchContext(this);
        }

    }

}
