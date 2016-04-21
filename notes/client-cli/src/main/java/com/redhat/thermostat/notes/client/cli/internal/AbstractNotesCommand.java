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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

import java.util.Objects;

public abstract class AbstractNotesCommand extends AbstractCommand implements NotesCommand {

    protected static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    protected DependencyServices dependencyServices;

    protected VmInfoDAO vmInfoDAO;
    protected HostInfoDAO hostInfoDAO;
    protected AgentInfoDAO agentInfoDAO;
    protected VmNoteDAO vmNoteDAO;
    protected HostNoteDAO hostNoteDAO;

    public AbstractNotesCommand() {
        this.dependencyServices = new DependencyServices();
    }

    protected void setupServices() throws CommandException {
        vmInfoDAO = dependencyServices.getService(VmInfoDAO.class);
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_INFO_DAO_UNAVAILABLE));
        hostInfoDAO = dependencyServices.getService(HostInfoDAO.class);
        requireNonNull(hostInfoDAO, translator.localize(LocaleResources.HOST_INFO_DAO_UNAVAILABLE));
        agentInfoDAO = dependencyServices.getService(AgentInfoDAO.class);
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_INFO_DAO_UNAVAILABLE));
        vmNoteDAO = dependencyServices.getService(VmNoteDAO.class);
        requireNonNull(vmNoteDAO, translator.localize(LocaleResources.VM_NOTE_DAO_UNAVAILABLE));
        hostNoteDAO = dependencyServices.getService(HostNoteDAO.class);
        requireNonNull(hostNoteDAO, translator.localize(LocaleResources.HOST_NOTE_DAO_UNAVAILABLE));
    }

    protected static void assertExpectedAgentAndVmArgsProvided(Arguments args) throws CommandException {
        boolean hasVmArg = args.hasArgument(VmArgument.ARGUMENT_NAME);
        boolean hasAgentArg = args.hasArgument(AgentArgument.ARGUMENT_NAME);
        if (hasVmArg && hasAgentArg) {
            throw new CommandException(translator.localize(LocaleResources.HOST_AND_VM_ARGS_PROVIDED));
        } else if (!(hasVmArg || hasAgentArg)) {
            throw new CommandException(translator.localize(LocaleResources.NO_ARGS_PROVIDED));
        }
    }

    protected void checkVmExists(VmId vmId) throws CommandException {
        Objects.requireNonNull(vmId);
        VmInfo info = vmInfoDAO.getVmInfo(vmId);
        requireNonNull(info, translator.localize(LocaleResources.INVALID_VMID, vmId.get()));
    }

    protected void checkAgentExists(AgentId agentId) throws CommandException {
        Objects.requireNonNull(agentId);
        AgentInformation info = agentInfoDAO.getAgentInformation(agentId);
        requireNonNull(info, translator.localize(LocaleResources.INVALID_AGENTID, agentId.get()));
    }

    protected VmRef getVmRefFromVmId(VmId vmId) {
        VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        return new VmRef(getHostRefFromAgentId(new AgentId(vmInfo.getAgentId())), vmId.get(), vmInfo.getVmPid(), vmInfo.getVmName());
    }

    protected HostRef getHostRefFromAgentId(AgentId agentId) {
        String hostName = hostInfoDAO.getHostInfo(agentId).getHostname();
        return new HostRef(agentId.get(), hostName);
    }

    protected static String getNoteId(Arguments args) throws CommandException {
        if (!args.hasArgument(NOTE_ID_ARGUMENT)) {
            throw new CommandException(translator.localize(LocaleResources.NOTE_ID_ARG_REQUIRED));
        }
        return args.getArgument(NOTE_ID_ARGUMENT);
    }

    protected static String getNoteContent(Arguments args) throws CommandException {
        if (args.hasArgument(NOTE_CONTENT_ARGUMENT)) {
            String content = args.getArgument(NOTE_CONTENT_ARGUMENT);
            if (content == null) {
                content = "";
            }
            return content;
        } else { // assume all non-option arguments are together meant to be the note content
            StringBuilder sb = new StringBuilder();
            for (String word : args.getNonOptionArguments()) {
                sb.append(word).append(' ');
            }
            String content = sb.toString().trim();
            if (content.isEmpty()) {
                throw new CommandException(translator.localize(LocaleResources.NOTE_CONTENT_ARG_REQUIRED));
            }
            return content;
        }
    }

    @Override
    public void setVmInfoDao(VmInfoDAO vmInfoDao) {
        dependencyServices.addService(VmInfoDAO.class, vmInfoDao);
    }

    @Override
    public void setHostInfoDao(HostInfoDAO hostInfoDao) {
        dependencyServices.addService(HostInfoDAO.class, hostInfoDao);
    }

    @Override
    public void setAgentInfoDao(AgentInfoDAO agentInfoDao) {
        dependencyServices.addService(AgentInfoDAO.class, agentInfoDao);
    }

    @Override
    public void setVmNoteDao(VmNoteDAO vmNoteDAO) {
        dependencyServices.addService(VmNoteDAO.class, vmNoteDAO);
    }

    @Override
    public void setHostNoteDao(HostNoteDAO hostNoteDAO) {
        dependencyServices.addService(HostNoteDAO.class, hostNoteDAO);
    }

    @Override
    public void servicesUnavailable() {
        dependencyServices.removeService(VmInfoDAO.class);
        dependencyServices.removeService(HostInfoDAO.class);
        dependencyServices.removeService(AgentInfoDAO.class);
        dependencyServices.removeService(VmNoteDAO.class);
        dependencyServices.removeService(HostNoteDAO.class);
    }

}
