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
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UpdateNoteSubcommandTest extends AbstractNotesCommandTest<UpdateNoteSubcommand> {

    @Override
    public UpdateNoteSubcommand createCommand() {
        return new UpdateNoteSubcommand();
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
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("note content");
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(null);

        doInvalidAgentIdTest(args);
    }

    @Test
    public void testRunWithInvalidVmId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("note content");
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(null);

        doInvalidVmIdTest(args);
    }

    @Test
    public void testRunWithNoMatchingHostNoteFromStorage() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("note content");
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(mock(HostInfo.class));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(mock(AgentInformation.class));
        when(hostNoteDAO.getById(any(HostRef.class), any(String.class))).thenReturn(null);

        CommandContext ctx = contextFactory.createContext(args);
        try {
            command.run(ctx);
            fail();
        } catch (CommandException ex) {
            // pass
        }
    }

    @Test
    public void testRunWithNoMatchingVmNoteFromStorage() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("note content");
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmNoteDAO.getById(any(VmRef.class), any(String.class))).thenReturn(null);

        CommandContext ctx = contextFactory.createContext(args);
        try {
            command.run(ctx);
            fail();
        } catch (CommandException ex) {
            // pass
        }
    }

    @Test
    public void testRunWithValidVmIdYieldingInvalidAgentId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("note content");
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");

        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(null);

        doInvalidAgentIdTest(args);
    }

    @Test
    public void testRunWithHostNote() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn("foo-agentid");
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("new note content");
        HostNote oldNote = new HostNote();
        oldNote.setId("foo-noteid");
        oldNote.setAgentId("foo-agentid");
        oldNote.setTimeStamp(100L);
        oldNote.setContent("old note content");
        when(hostNoteDAO.getById(any(HostRef.class), eq("foo-noteid"))).thenReturn(oldNote);
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        ArgumentCaptor<HostNote> noteCaptor = ArgumentCaptor.forClass(HostNote.class);
        command.run(ctx);
        verifyZeroInteractions(vmNoteDAO);
        verify(hostNoteDAO).update(noteCaptor.capture());
        HostNote note = noteCaptor.getValue();
        assertThat(note.getContent(), is("new note content"));
        assertThat(note.getAgentId(), is(oldNote.getAgentId()));
        assertThat(note.getId(), is(oldNote.getId()));
        assertThat(note.getTimeStamp(), is(atLeast(oldNote.getTimeStamp())));
    }

    @Test
    public void testRunWithVmNote() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn("foo-vmid");
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("new note content");
        VmNote oldNote = new VmNote();
        oldNote.setId("foo-noteid");
        oldNote.setAgentId("foo-agentid");
        oldNote.setTimeStamp(100L);
        oldNote.setContent("old note content");
        when(vmNoteDAO.getById(any(VmRef.class), eq("foo-noteid"))).thenReturn(oldNote);
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-hostname");
        hostInfo.setAgentId("foo-agentid");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(new AgentInformation());
        CommandContext ctx = contextFactory.createContext(args);
        ArgumentCaptor<VmNote> noteCaptor = ArgumentCaptor.forClass(VmNote.class);
        command.run(ctx);
        verifyZeroInteractions(hostNoteDAO);
        verify(vmNoteDAO).update(noteCaptor.capture());
        VmNote note = noteCaptor.getValue();
        assertThat(note.getContent(), is("new note content"));
        assertThat(note.getAgentId(), is(oldNote.getAgentId()));
        assertThat(note.getId(), is(oldNote.getId()));
        assertThat(note.getTimeStamp(), is(atLeast(oldNote.getTimeStamp())));
    }

    private static Matcher<Long> atLeast(final Long l) {
        return new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Long && ((long) o) >= l;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("long greater than or equal to ")
                        .appendValue(l);
            }
        };
    }

}
