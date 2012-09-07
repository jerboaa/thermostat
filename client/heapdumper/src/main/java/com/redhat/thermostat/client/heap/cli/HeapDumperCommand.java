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

package com.redhat.thermostat.client.heap.cli;

import java.net.InetSocketAddress;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.OSGIUtils;

public class HeapDumperCommand {
    
    private static final String RECEIVER_CLASS_NAME = "com.redhat.thermostat.agent.heapdumper.internal.HeapDumpReceiver";
    private static final String VM_ID_PARAM = "vmId";

    private class HeapDumpListener implements RequestResponseListener {

        private Runnable heapDumpCompleteAction;

        private HeapDumpListener(Runnable heapDumpCompleteAction) {
            this.heapDumpCompleteAction = heapDumpCompleteAction;
        }

        @Override
        public void fireComplete(Request request, Response response) {
            heapDumpCompleteAction.run();
        }
        
    }

    public void execute(VmRef reference, Runnable heapDumpCompleteAction) {

        DAOFactory df = ApplicationContext.getInstance().getDAOFactory();
        HostRef targetHostRef = reference.getAgent();
        String address = df.getAgentInfoDAO().getAgentInformation(targetHostRef).getConfigListenAddress();
        
        String [] host = address.split(":");
        InetSocketAddress target = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
        Request req = new Request(RequestType.RESPONSE_EXPECTED, target);
        req.setReceiver(RECEIVER_CLASS_NAME);
        req.setParameter(VM_ID_PARAM, reference.getIdString());
        req.addListener(new HeapDumpListener(heapDumpCompleteAction));

        RequestQueue queue = OSGIUtils.getInstance().getService(RequestQueue.class);
        queue.putRequest(req);

    }

}
