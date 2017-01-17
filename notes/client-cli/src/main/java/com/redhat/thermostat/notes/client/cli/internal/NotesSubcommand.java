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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.shared.locale.LocalizedString;
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

public abstract class NotesSubcommand {

    static final String NOTE_ID_ARGUMENT = "noteId";
    static final String NOTE_CONTENT_ARGUMENT = "content";

    static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    DependencyServices services = new DependencyServices();

    public abstract void run(CommandContext ctx) throws CommandException;

    static void assertExpectedAgentAndVmArgsProvided(Arguments args) throws CommandException {
        boolean hasVmArg = args.hasArgument(VmArgument.ARGUMENT_NAME);
        boolean hasAgentArg = args.hasArgument(AgentArgument.ARGUMENT_NAME);
        if (hasVmArg && hasAgentArg) {
            throw new CommandException(translator.localize(LocaleResources.HOST_AND_VM_ARGS_PROVIDED));
        } else if (!(hasVmArg || hasAgentArg)) {
            throw new CommandException(translator.localize(LocaleResources.NO_ARGS_PROVIDED));
        }
    }

    void checkVmExists(VmId vmId) throws CommandException {
        requireNonNull(vmId, translator.localize(LocaleResources.NULL_VMID));
        VmInfo info = services.getRequiredService(VmInfoDAO.class).getVmInfo(vmId);
        requireNonNull(info, translator.localize(LocaleResources.INVALID_VMID, vmId.get()));
    }

    void checkAgentExists(AgentId agentId) throws CommandException {
        requireNonNull(agentId, translator.localize(LocaleResources.NULL_AGENTID));
        AgentInformation info = services.getRequiredService(AgentInfoDAO.class).getAgentInformation(agentId);
        requireNonNull(info, translator.localize(LocaleResources.INVALID_AGENTID, agentId.get()));
    }

    VmRef getVmRefFromVmId(VmId vmId) throws CommandException {
        requireNonNull(vmId, translator.localize(LocaleResources.NULL_VMID));
        VmInfo vmInfo = services.getRequiredService(VmInfoDAO.class).getVmInfo(vmId);
        return new VmRef(getHostRefFromAgentId(new AgentId(vmInfo.getAgentId())), vmId.get(), vmInfo.getVmPid(), vmInfo.getVmName());
    }

    HostRef getHostRefFromAgentId(AgentId agentId) throws CommandException {
        String hostName = services.getRequiredService(HostInfoDAO.class).getHostInfo(agentId).getHostname();
        return new HostRef(agentId.get(), hostName);
    }

    static String getNoteId(Arguments args) throws CommandException {
        if (!args.hasArgument(NOTE_ID_ARGUMENT)) {
            throw new CommandException(translator.localize(LocaleResources.NOTE_ID_ARG_REQUIRED));
        }
        return args.getArgument(NOTE_ID_ARGUMENT);
    }

    static String getNoteContent(Arguments args) throws CommandException {
        if (!args.hasArgument(NOTE_CONTENT_ARGUMENT)) {
            throw new CommandException(translator.localize(LocaleResources.NOTE_CONTENT_ARG_REQUIRED));
        }
        return args.getArgument(NOTE_CONTENT_ARGUMENT);
    }

    static void requireNonNull(Object obj, LocalizedString message) throws CommandException {
        if (obj == null) {
            throw new CommandException(message);
        }
    }

    public void bindVmInfoDao(VmInfoDAO vmInfoDao) {
        services.addService(VmInfoDAO.class, vmInfoDao);
    }

    public void unbindVmInfoDao(VmInfoDAO vmInfoDao) {
        services.removeService(VmInfoDAO.class);
    }

    public void bindHostInfoDao(HostInfoDAO hostInfoDao) {
        services.addService(HostInfoDAO.class, hostInfoDao);
    }

    public void unbindHostInfoDao(HostInfoDAO hostInfoDao) {
        services.removeService(HostInfoDAO.class);
    }

    public void bindAgentInfoDao(AgentInfoDAO agentInfoDao) {
        services.addService(AgentInfoDAO.class, agentInfoDao);
    }

    public void unbindAgentInfoDao(AgentInfoDAO agentInfoDao) {
        services.removeService(AgentInfoDAO.class);
    }

    public void bindVmNoteDao(VmNoteDAO vmNoteDAO) {
        services.addService(VmNoteDAO.class, vmNoteDAO);
    }

    public void unbindVmNoteDao(VmNoteDAO vmNoteDao) {
        services.removeService(VmNoteDAO.class);
    }

    public void bindHostNoteDao(HostNoteDAO hostNoteDAO) {
        services.addService(HostNoteDAO.class, hostNoteDAO);
    }

    public void unbindHostNoteDao(HostNoteDAO hostNoteDao) {
        services.removeService(HostNoteDAO.class);
    }

    public void servicesUnavailable() {
        services.removeService(VmInfoDAO.class);
        services.removeService(HostInfoDAO.class);
        services.removeService(AgentInfoDAO.class);
        services.removeService(VmNoteDAO.class);
        services.removeService(HostNoteDAO.class);
    }

}
