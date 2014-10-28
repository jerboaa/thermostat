/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.jmx.client.core.internal;

import java.net.InetSocketAddress;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.vm.jmx.common.JmxCommand;

public class JmxToggleNotificationRequest {
    
    static final String CMD_CHANNEL_ACTION_NAME = "jmx-toggle-notifications";

    private RequestQueue queue;
    private AgentInfoDAO agentDAO;
    private Runnable successAction;
    private Runnable failureAction;
    private JmxToggleResponseListenerFactory factory;

    public JmxToggleNotificationRequest(RequestQueue queue, AgentInfoDAO agentDAO,
            Runnable successAction, Runnable failureAction) {
        this(queue, agentDAO, successAction, failureAction, new JmxToggleResponseListenerFactory());
    }
    
    JmxToggleNotificationRequest(RequestQueue queue, AgentInfoDAO agentDAO, Runnable successAction, 
            Runnable failureAction, JmxToggleResponseListenerFactory factory) {
        this.queue = queue;
        this.agentDAO = agentDAO;
        this.successAction = successAction;
        this.failureAction = failureAction;
        this.factory = factory;
    }

    public void sendEnableNotificationsRequestToAgent(VmRef vm, boolean enable) {
        HostRef targetHostRef = vm.getHostRef();

        InetSocketAddress target = agentDAO.getAgentInformation(targetHostRef).getRequestQueueAddress();
        Request req = new Request(RequestType.RESPONSE_EXPECTED, target);

        req.setReceiver(JmxCommand.RECEIVER);

        req.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        req.setParameter(JmxCommand.class.getName(), enable ? JmxCommand.ENABLE_JMX_NOTIFICATIONS.name() : JmxCommand.DISABLE_JMX_NOTIFICATIONS.name());
        req.setParameter(JmxCommand.VM_PID, String.valueOf(vm.getPid()));
        req.setParameter(JmxCommand.VM_ID, vm.getVmId());

        JmxToggleResponseListener listener = factory.createListener(successAction, failureAction);
        req.addListener(listener);

        queue.putRequest(req);
    }
    
    static class JmxToggleResponseListener implements RequestResponseListener {
        
        private Runnable successAction;
        private Runnable failureAction;
        
        public JmxToggleResponseListener(Runnable successAction, Runnable failureAction) {
            this.successAction = successAction;
            this.failureAction = failureAction;
        }

        @Override
        public void fireComplete(Request request, Response response) {
            switch (response.getType()) {
            case OK:
                successAction.run();
                break;
            default:
                failureAction.run();
                break;
            }
        }
        
    }
    
    static class JmxToggleResponseListenerFactory {
        
        JmxToggleResponseListener createListener(Runnable successAction, 
                Runnable failureAction) {
            return new JmxToggleResponseListener(successAction, failureAction);
        }
        
    }
}

