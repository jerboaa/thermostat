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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.net.InetSocketAddress;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class DumpHeapHelper {
    
    private static final String RECEIVER_CLASS_NAME = "com.redhat.thermostat.vm.heap.analysis.agent.internal.HeapDumpReceiver";
    private static final String CMD_CHANNEL_ACTION_NAME = "dump-heap";
    private static final String VM_ID_PARAM = "vmId";
    private static final String VM_PID_PARAM = "vmPid";

    private class HeapDumpListener implements RequestResponseListener {

        private Runnable successAction;
        private Runnable failureAction;

        private HeapDumpListener(Runnable heapDumpSuccessfulAction, Runnable heapDumpFailureAction) {
            this.successAction = heapDumpSuccessfulAction;
            this.failureAction = heapDumpFailureAction;
        }

        @Override
        public void fireComplete(Request request, Response response) {
            if (response.getType() == ResponseType.ERROR) {
                if (failureAction != null) {
                    failureAction.run();
                }
            } else {
                if (successAction != null) {
                    successAction.run();
                }
            }
        }

    }

    public void execute(VmInfoDAO vmInfoDAO, AgentInfoDAO agentInfoDAO, AgentId agentId, VmId vmId,
                        RequestQueue queue, Runnable heapDumpSuccessAction,
                        Runnable heapDumpFailureAction) {
        // Get PID
        VmInfo info = vmInfoDAO.getVmInfo(vmId);
        int pid = info.getVmPid();
        
        InetSocketAddress target = agentInfoDAO.getAgentInformation(agentId).getRequestQueueAddress();
        Request req = new Request(RequestType.RESPONSE_EXPECTED, target);
        req.setReceiver(RECEIVER_CLASS_NAME);
        req.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        req.setParameter(VM_ID_PARAM, vmId.get());
        req.setParameter(VM_PID_PARAM, String.valueOf(pid));
        req.addListener(new HeapDumpListener(heapDumpSuccessAction, heapDumpFailureAction));

        queue.putRequest(req);
    }

}

