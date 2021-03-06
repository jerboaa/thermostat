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

package com.redhat.thermostat.vm.gc.command.internal;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.model.VmInfo.AliveStatus;

public class GCCommand extends AbstractCommand {

    // The name as which this command gets registered
    static final String REGISTER_NAME = "gc";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private GCRequest request;
    private AgentInfoDAO agentInfoDAO;
    private VmInfoDAO vmInfoDAO;

    private final GCCommandListenerFactory listenerFactory;
    private GCCommandListener listener;
    private Semaphore servicesAvailable = new Semaphore(0);

    GCCommand(GCCommandListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        waitForServices(500l);

        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(request, translator.localize(LocaleResources.GCREQUEST_SERVICE_UNAVAILABLE));

        listener = listenerFactory.createListener(ctx.getConsole().getOutput(), ctx.getConsole().getError());

        VmArgument vmArgument = VmArgument.required(ctx.getArguments());
        attemptGC(vmArgument.getVmId());
    }

    private void waitForServices(long timeout) throws CommandException {
        try {
            servicesAvailable.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_INTERRUPTED));
        }
    }

    private void attemptGC(VmId vmId) throws CommandException {
        VmInfo result = vmInfoDAO.getVmInfo(vmId);

        if (result == null) {
            throw new CommandException(translator.localize(LocaleResources.VM_NOT_FOUND, vmId.get()));
        }
        AgentInformation agentInfo = agentInfoDAO.getAgentInformation(new AgentId(result.getAgentId()));
        AliveStatus status = result.isAlive(agentInfo);
        if (status != AliveStatus.RUNNING) {
            throw new CommandException(translator.localize(LocaleResources.VM_NOT_ALIVE, vmId.get()));
        }
        
        HostRef dummyRef = new HostRef(result.getAgentId(), "dummy");
        sendGCRequest(new VmRef(dummyRef, result.getVmId(), result.getVmPid(), result.getVmName()));
    }

    private void sendGCRequest(VmRef vmRef) throws CommandException {
        request.sendGCRequestToAgent(vmRef, agentInfoDAO, listener);

        waitForListenerResponse();
    }

    private void waitForListenerResponse() {
        try {
            listener.await(30_000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setServices(GCRequest request, AgentInfoDAO agentInfoDAO, VmInfoDAO vmInfoDAO) {
        this.request = request;
        this.agentInfoDAO = agentInfoDAO;
        this.vmInfoDAO = vmInfoDAO;

        if (request == null || agentInfoDAO == null || vmInfoDAO == null) {
            servicesUnavailable();
        } else {
            servicesAvailable();
        }
    }

    private void servicesAvailable() {
        this.servicesAvailable.release();
    }

    private void servicesUnavailable() {
        this.servicesAvailable.drainPermits();
    }
}
