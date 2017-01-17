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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.util.Objects;

class VmSocketIdentifier {

    private static final int AGENT_ID_PART_LENGTH = 8;
    private static final int VM_ID_PART_LENGTH = 4;
    private static final String SOCKET_FORMAT = "%s_%s_%06d";
    private final int vmPid;
    private final String vmId;
    private final String agentId;
    
    VmSocketIdentifier(String vmId, int pid, String agentId) {
        this.vmId = Objects.requireNonNull(vmId);
        this.vmPid = pid;
        this.agentId = Objects.requireNonNull(agentId);
    }
    
    String getName() {
        int agentIdLength = Math.min(agentId.length(), AGENT_ID_PART_LENGTH);
        int vmIdLength = Math.min(vmId.length(), VM_ID_PART_LENGTH);
        String agentIdPart = agentId.substring(0, agentIdLength);
        String vmIdPart = vmId.substring(0, vmIdLength);
        return String.format(SOCKET_FORMAT, agentIdPart, vmIdPart, vmPid);
    }

    String getVmId() {
        return vmId;
    }

    String getAgentId() {
        return agentId;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != VmSocketIdentifier.class) {
            return false;
        }
        VmSocketIdentifier o = (VmSocketIdentifier) other;
        return Objects.equals(vmPid, o.vmPid) &&
                Objects.equals(getAgentId(), o.getAgentId()) &&
                Objects.equals(getVmId(), o.getVmId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getVmId(), getAgentId(), vmPid);
    }
    
}
