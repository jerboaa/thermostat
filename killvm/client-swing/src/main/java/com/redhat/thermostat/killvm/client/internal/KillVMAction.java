/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.net.InetSocketAddress;
import java.util.Objects;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.killvm.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

/**
 * Implements the {@link VMContextAction} entry point to provide a kill switch
 * for the currently selected Virtual Machine. 
 */
public class KillVMAction implements ReferenceContextAction {

    private static final String RECEIVER = "com.redhat.thermostat.killvm.agent.internal.KillVmReceiver";
    private static final String CMD_CHANNEL_ACTION_NAME = "killvm";
    private final AgentInfoDAO agentDao;
    private final VmInfoDAO vmDao;
    private final Translate<LocaleResources> t;
    private final RequestQueue queue;
    private final RequestResponseListener listener;

    public KillVMAction(AgentInfoDAO agentDao, VmInfoDAO vmDao, RequestQueue queue, RequestResponseListener listener) {
        Objects.requireNonNull(listener, "Listener can't be null");
        this.agentDao = agentDao;
        this.vmDao = vmDao;
        this.t = LocaleResources.createLocalizer();
        this.queue = queue;
        this.listener = listener;
    }

    @Override
    public LocalizedString getName() {
        return t.localize(LocaleResources.ACTION_NAME);
    }

    @Override
    public LocalizedString getDescription() {
        return t.localize(LocaleResources.ACTION_DESCRIPTION);
    }

    @Override
    public void execute(Ref ref) {
        
        if (!(ref instanceof VmRef)) {
            return;
        }
        
        VmRef reference = (VmRef) ref;
        
        String address = agentDao.getAgentInformation(reference.getHostRef()).getConfigListenAddress();
        
        String [] host = address.split(":");
        InetSocketAddress target = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
        Request murderer = getKillRequest(target);
        murderer.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        murderer.setParameter("vm-pid", String.valueOf(reference.getPid()));
        murderer.setReceiver(RECEIVER);
        murderer.addListener(listener);

        queue.putRequest(murderer);
    }

    // testing hook; keep this package private
    Request getKillRequest(InetSocketAddress target) {
        return new Request(RequestType.RESPONSE_EXPECTED, target);
    }

    @Override
    public ReferenceFilter getFilter() {
        return new LocalAndAliveFilter();
    }

    private class LocalAndAliveFilter extends ReferenceFilter {
        
        @Override
        public boolean applies(Ref reference) {
            return (reference instanceof VmRef);
        }
        
        @Override
        public boolean matches(Ref ref) {
            boolean match = false;
            
            if (applies(ref)) {
                VmRef reference = (VmRef) ref;
                VmInfo vmInfo = vmDao.getVmInfo(reference);
                match = vmInfo.isAlive();
            }
            
            return match;
        }

    }
}

