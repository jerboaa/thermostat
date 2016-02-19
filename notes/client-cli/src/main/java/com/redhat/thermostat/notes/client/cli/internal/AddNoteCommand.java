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
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;

import java.util.UUID;

public class AddNoteCommand extends AbstractNotesCommand {

    static final String NAME = "add-note";

    @Override
    public void run(CommandContext ctx) throws CommandException {
        setupServices();

        Arguments args = ctx.getArguments();
        assertExpectedAgentAndVmArgsProvided(args);

        String noteContent = getNoteContent(args);

        if (args.hasArgument(VmArgument.ARGUMENT_NAME)) {
            VmId vmId = VmArgument.required(args).getVmId();
            VmNote note = new VmNote();
            String agentId = vmInfoDAO.getVmInfo(vmId).getAgentId();
            note.setId(UUID.randomUUID().toString());
            note.setVmId(vmId.get());
            note.setAgentId(agentId);
            note.setContent(noteContent);
            note.setTimeStamp(System.currentTimeMillis());

            vmNoteDAO.add(note);
        } else {
            AgentId agentId = AgentArgument.required(args).getAgentId();
            HostNote note = new HostNote();
            note.setId(UUID.randomUUID().toString());
            note.setAgentId(agentId.get());
            note.setContent(noteContent);
            note.setTimeStamp(System.currentTimeMillis());

            hostNoteDAO.add(note);
        }
    }

}
