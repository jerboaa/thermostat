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
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DeleteNoteCommandTest extends AbstractNotesCommandTest<DeleteNoteCommand> {

    @Override
    public DeleteNoteCommand createCommand() {
        return new DeleteNoteCommand();
    }

    @Test(expected = CommandException.class)
    public void testRunFailsWithBothVmAndAgentIdGiven() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testRunFailsWithNeitherVmNorAgentIdGiven() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
    }

    @Test
    public void testRunWithHostNote() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(args.hasArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        ArgumentCaptor<HostRef> refCaptor = ArgumentCaptor.forClass(HostRef.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(hostNoteDAO).removeById(refCaptor.capture(), idCaptor.capture());
        assertThat(refCaptor.getValue().getAgentId(), is("foo-agentid"));
        assertThat(idCaptor.getValue(), is("foo-noteid"));
    }

    @Test
    public void testRunWithVmNote() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        when(args.hasArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        vmInfo.setVmPid(100);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(hostNoteDAO);
        ArgumentCaptor<VmRef> refCaptor = ArgumentCaptor.forClass(VmRef.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(vmNoteDAO).removeById(refCaptor.capture(), idCaptor.capture());
        assertThat(refCaptor.getValue().getHostRef().getAgentId(), is("foo-agentid"));
        assertThat(refCaptor.getValue().getVmId(), is("foo-vmid"));
        assertThat(refCaptor.getValue().getPid(), is(100));
        assertThat(idCaptor.getValue(), is("foo-noteid"));
    }

}
