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
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ListNotesSubcommandTest extends AbstractNotesCommandTest<ListNotesSubcommand> {

    @Override
    public ListNotesSubcommand createCommand() {
        return new ListNotesSubcommand();
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
    public void testRunWithInvalidAgentId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(null);

        doInvalidAgentIdTest(args);
    }

    @Test
    public void testRunWithInvalidVmId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(null);

        doInvalidVmIdTest(args);
    }

    @Test
    public void testRunWithValidVmIdYieldingInvalidAgentId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");

        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(null);

        doInvalidAgentIdTest(args);
    }

    @Test
    public void testRunWithHostAndNoNotes() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        ArgumentCaptor<HostRef> refCaptor = ArgumentCaptor.forClass(HostRef.class);
        verify(hostNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getAgentId(), is("foo-agentid"));
        assertThat(contextFactory.getOutput(), is(""));
    }

    @Test
    public void testRunWithHostWithNotes() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        HostNote note1 = mock(HostNote.class);
        when(note1.getContent()).thenReturn("note 1");
        when(note1.getTimeStamp()).thenReturn(100L);
        when(note1.getId()).thenReturn(UUID.randomUUID().toString());
        HostNote note2 = mock(HostNote.class);
        when(note2.getContent()).thenReturn("note 2");
        when(note2.getTimeStamp()).thenReturn(100L);
        when(note2.getId()).thenReturn(UUID.randomUUID().toString());
        when(hostNoteDAO.getFor(any(HostRef.class))).thenReturn(Arrays.asList(note1, note2));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        ArgumentCaptor<HostRef> refCaptor = ArgumentCaptor.forClass(HostRef.class);
        verify(hostNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getAgentId(), is("foo-agentid"));
        String printed = contextFactory.getOutput();
        assertThat(printed, containsString(note1.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note1.getTimeStamp()))));
        assertThat(printed, not(containsString(note1.getId())));
        assertThat(printed, containsString(note2.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note2.getTimeStamp()))));
        assertThat(printed, not(containsString(note2.getId())));
    }

    @Test
    public void testRunWithHostWithNotesAndShowIdOption() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(args.hasArgument(ListNotesSubcommand.SHOW_NOTE_ID_ARGUMENT)).thenReturn(true);
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        HostNote note1 = mock(HostNote.class);
        when(note1.getContent()).thenReturn("note 1");
        when(note1.getTimeStamp()).thenReturn(100L);
        when(note1.getId()).thenReturn(UUID.randomUUID().toString());
        HostNote note2 = mock(HostNote.class);
        when(note2.getContent()).thenReturn("note 2");
        when(note2.getTimeStamp()).thenReturn(100L);
        when(note2.getId()).thenReturn(UUID.randomUUID().toString());
        when(hostNoteDAO.getFor(any(HostRef.class))).thenReturn(Arrays.asList(note1, note2));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        ArgumentCaptor<HostRef> refCaptor = ArgumentCaptor.forClass(HostRef.class);
        verify(hostNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getAgentId(), is("foo-agentid"));
        String printed = contextFactory.getOutput();
        assertThat(printed, containsString(note1.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note1.getTimeStamp()))));
        assertThat(printed, containsString(note1.getId()));
        assertThat(printed, containsString(note2.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note2.getTimeStamp()))));
        assertThat(printed, containsString(note2.getId()));
    }

    @Test
    public void testRunWithHostWithNotesAndQuietOption() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(args.hasArgument(ListNotesSubcommand.QUIET_ARGUMENT)).thenReturn(true);
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        HostNote note1 = mock(HostNote.class);
        when(note1.getContent()).thenReturn("note 1");
        when(note1.getTimeStamp()).thenReturn(100L);
        when(note1.getId()).thenReturn(UUID.randomUUID().toString());
        HostNote note2 = mock(HostNote.class);
        when(note2.getContent()).thenReturn("note 2");
        when(note2.getTimeStamp()).thenReturn(100L);
        when(note2.getId()).thenReturn(UUID.randomUUID().toString());
        when(hostNoteDAO.getFor(any(HostRef.class))).thenReturn(Arrays.asList(note1, note2));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        ArgumentCaptor<HostRef> refCaptor = ArgumentCaptor.forClass(HostRef.class);
        verify(hostNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getAgentId(), is("foo-agentid"));
        String printed = contextFactory.getOutput();
        assertThat(printed, not(containsString(note1.getContent())));
        assertThat(printed, not(containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note1.getTimeStamp())))));
        assertThat(printed, containsString(note1.getId()));
        assertThat(printed, not(containsString(note2.getContent())));
        assertThat(printed, not(containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note2.getTimeStamp())))));
        assertThat(printed, containsString(note2.getId()));
    }

    @Test
    public void testRunWithVmAndNoNotes() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        vmInfo.setVmPid(100);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(hostNoteDAO);
        ArgumentCaptor<VmRef> refCaptor = ArgumentCaptor.forClass(VmRef.class);
        verify(vmNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getHostRef().getAgentId(), is("foo-agentid"));
        assertThat(refCaptor.getValue().getVmId(), is("foo-vmid"));
        assertThat(refCaptor.getValue().getPid(), is(100));
    }

    @Test
    public void testRunWithVmWithNotes() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        VmNote note1 = mock(VmNote.class);
        when(note1.getContent()).thenReturn("note 1");
        when(note1.getTimeStamp()).thenReturn(100L);
        when(note1.getId()).thenReturn(UUID.randomUUID().toString());
        VmNote note2 = mock(VmNote.class);
        when(note2.getContent()).thenReturn("note 2");
        when(note2.getTimeStamp()).thenReturn(100L);
        when(note2.getId()).thenReturn(UUID.randomUUID().toString());
        when(vmNoteDAO.getFor(any(VmRef.class))).thenReturn(Arrays.asList(note1, note2));
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        vmInfo.setVmPid(100);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        command.run(ctx);
        verifyZeroInteractions(hostNoteDAO);
        ArgumentCaptor<VmRef> refCaptor = ArgumentCaptor.forClass(VmRef.class);
        verify(vmNoteDAO).getFor(refCaptor.capture());
        assertThat(refCaptor.getValue().getHostRef().getAgentId(), is("foo-agentid"));
        assertThat(refCaptor.getValue().getVmId(), is("foo-vmid"));
        assertThat(refCaptor.getValue().getPid(), is(100));
        String printed = contextFactory.getOutput();
        assertThat(printed, containsString(note1.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note1.getTimeStamp()))));
        assertThat(printed, not(containsString(note1.getId())));
        assertThat(printed, containsString(note2.getContent()));
        assertThat(printed, containsString(Clock.DEFAULT_DATE_FORMAT.format(new Date(note2.getTimeStamp()))));
        assertThat(printed, not(containsString(note2.getId())));
    }

}
