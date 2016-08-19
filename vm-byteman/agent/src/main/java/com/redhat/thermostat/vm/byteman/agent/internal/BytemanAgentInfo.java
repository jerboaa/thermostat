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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.util.Objects;

class BytemanAgentInfo {

    private final int vmPid;
    private final int agentListenPort;
    private final String listenHost;
    private final String vmId;
    private final String writerId;
    private final boolean isAttachFailedNoProc;
    
    BytemanAgentInfo(int vmPid, int agentListenPort, String listenHost, String vmId, String writerId, boolean isAttachFailedNoProc) {
        this.agentListenPort = agentListenPort;
        this.listenHost = listenHost;
        this.vmPid = vmPid;
        this.vmId = vmId;
        this.writerId = writerId;
        this.isAttachFailedNoProc = isAttachFailedNoProc;
    }

    int getVmPid() {
        return vmPid;
    }

    int getAgentListenPort() {
        return agentListenPort;
    }

    String getListenHost() {
        return listenHost;
    }

    String getVmId() {
        return vmId;
    }
    
    String getWriterId() {
        return writerId;
    }
    
    boolean isAttachFailedNoSuchProcess() {
        return isAttachFailedNoProc;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != BytemanAgentInfo.class) {
            return false;
        }
        BytemanAgentInfo o = (BytemanAgentInfo) other;
        return Objects.equals(agentListenPort, o.agentListenPort) &&
                Objects.equals(listenHost, o.listenHost) &&
                Objects.equals(vmPid, o.vmPid) &&
                Objects.equals(vmId, o.vmId) &&
                Objects.equals(writerId, o.writerId) &&
                Objects.equals(isAttachFailedNoProc, o.isAttachFailedNoProc);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agentListenPort, listenHost, vmPid, vmId, writerId, isAttachFailedNoProc);
    }
}
