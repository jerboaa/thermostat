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

package com.redhat.thermostat.killvm.command;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.killvm.command.internal.ShellVMKilledListener;
import com.redhat.thermostat.killvm.command.locale.LocaleResources;
import com.redhat.thermostat.killvm.common.KillVMRequest;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class KillVMCommand extends AbstractCommand {

    private static final String RECEIVER = "com.redhat.thermostat.killvm.agent.internal.KillVmReceiver";
    private static final String CMD_CHANNEL_ACTION_NAME = "killvm";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final ShellVMKilledListener listener;

    private HostInfoDAO hostInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private KillVMRequest request;

    public KillVMCommand(ShellVMKilledListener listener) {
        this.listener = listener;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        requireNonNull(hostInfoDAO, translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(request, translator.localize(LocaleResources.REQUEST_SERVICE_UNAVAILABLE));

        listener.setOut(ctx.getConsole().getOutput());
        listener.setErr(ctx.getConsole().getError());

        HostVMArguments args = new HostVMArguments(ctx.getArguments(), true, true);

        attemptToKillVM(args.getVM());
    }

    private void attemptToKillVM(VmRef vmRef) throws CommandException {
        try {
            VmInfo result = vmInfoDAO.getVmInfo(vmRef);
            sendKillRequest(vmRef.getHostRef(), result.getVmPid());
        } catch (DAOException e) {
            //FIXME: VmInfoDaoImpl currently throws DAOException when VM is not found
            //This should be changed when VmInfoDAOImpl gets fixed
            throw new CommandException(translator.localize(LocaleResources.VM_NOT_FOUND, vmRef.getVmId()));
        }

    }

    private void sendKillRequest(HostRef ref, int vmPid) throws CommandException {
        VmRef vmRef = new VmRef(ref, "dummy", vmPid, "dummy");
        request.sendKillVMRequestToAgent(vmRef, agentInfoDAO, listener);

        waitForListenerResponse();
    }

    private void waitForListenerResponse() throws CommandException {
        try {
            listener.await(1000l);
        } catch (InterruptedException e) {
            throw new CommandException(translator.localize(LocaleResources.KILL_INTERRUPTED));
        }
    }

    public void setHostInfoDAO(HostInfoDAO hostInfoDAO) {
        this.hostInfoDAO = hostInfoDAO;
    }

    public void setVmInfoDAO(VmInfoDAO vmInfoDAO) {
        this.vmInfoDAO = vmInfoDAO;
    }

    public void setAgentInfoDAO(AgentInfoDAO agentInfoDAO) {
        this.agentInfoDAO = agentInfoDAO;
    }

    public void setKillVMRequest(KillVMRequest request) {
        this.request = request;
    }
}
