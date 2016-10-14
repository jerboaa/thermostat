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

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TabCompleter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotesControlCommandTest {

    private AddNoteSubcommand addNoteCommand;
    private DeleteNoteSubcommand deleteNoteCommand;
    private ListNotesSubcommand listNotesCommand;
    private UpdateNoteSubcommand updateNoteCommand;
    private NoteIdsFinder noteIdsFinder;

    private NotesControlCommand notesControlCommand;

    @Before
    public void setup() {
        addNoteCommand = mock(AddNoteSubcommand.class);
        deleteNoteCommand = mock(DeleteNoteSubcommand.class);
        listNotesCommand = mock(ListNotesSubcommand.class);
        updateNoteCommand = mock(UpdateNoteSubcommand.class);
        noteIdsFinder = mock(NoteIdsFinder.class);

        notesControlCommand = new NotesControlCommand(noteIdsFinder, addNoteCommand, deleteNoteCommand, updateNoteCommand,
                listNotesCommand);
    }

    @Test
    public void testCommandName() {
        assertThat(NotesControlCommand.COMMAND_NAME, is("notes"));
    }

    @Test
    public void testSubcommandCompleters() {
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = notesControlCommand.getSubcommandCompleters();

        assertThat(map.size(), is(2));
        assertThat(new HashSet<>(map.keySet()),
                is(equalTo(new HashSet<>(Arrays.asList(UpdateNoteSubcommand.SUBCOMMAND_NAME, DeleteNoteSubcommand.SUBCOMMAND_NAME)))));
        Map<CliCommandOption, ? extends TabCompleter> completer = map.get(UpdateNoteSubcommand.SUBCOMMAND_NAME);
        assertThat(completer.size(), is(1));
        assertThat(completer.keySet(), is(equalTo(Collections.singleton(NotesControlCommand.NOTE_ID_OPTION))));
        assertThat(completer.get(NotesControlCommand.NOTE_ID_OPTION), is(not(equalTo(null))));
    }

    @Test
    public void testNoteIdOption() {
        assertThat(NotesControlCommand.NOTE_ID_OPTION.getOpt(), is("n"));
        assertThat(NotesControlCommand.NOTE_ID_OPTION.getLongOpt(), is("noteId"));
    }

    @Test
    public void verifyAddNoteSubcommandDelegation() throws CommandException {
        performSubcommandTest(AddNoteSubcommand.SUBCOMMAND_NAME, addNoteCommand);
    }

    @Test
    public void verifyDeleteNoteSubcommandDelegation() throws CommandException {
        performSubcommandTest(DeleteNoteSubcommand.SUBCOMMAND_NAME, deleteNoteCommand);
    }

    @Test
    public void verifyListNotesSubcommandDelegation() throws CommandException {
        performSubcommandTest(ListNotesSubcommand.SUBCOMMAND_NAME, listNotesCommand);
    }

    @Test
    public void verifyUpdateNoteSubcommandDelegation() throws CommandException {
        performSubcommandTest(UpdateNoteSubcommand.SUBCOMMAND_NAME, updateNoteCommand);
    }

    private void performSubcommandTest(String subcommandName, NotesSubcommand subcommand) throws CommandException {
        CommandContext ctx = mock(CommandContext.class);
        Arguments args = mock(Arguments.class);
        when(ctx.getArguments()).thenReturn(args);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(subcommandName));

        notesControlCommand.run(ctx);
        verify(subcommand).run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testUnknownSubcommand() throws CommandException {
        CommandContext ctx = mock(CommandContext.class);
        Arguments args = mock(Arguments.class);
        when(ctx.getArguments()).thenReturn(args);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList("fake-subcommand"));

        notesControlCommand.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testNoSubcommand() throws CommandException {
        CommandContext ctx = mock(CommandContext.class);
        Arguments args = mock(Arguments.class);
        when(ctx.getArguments()).thenReturn(args);
        when(args.getNonOptionArguments()).thenReturn(Collections.<String>emptyList());

        notesControlCommand.run(ctx);
    }

    @Test
    public void verifyNoteIdsFinderIsPassed() {
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = notesControlCommand.getSubcommandCompleters();
        map.get(DeleteNoteSubcommand.SUBCOMMAND_NAME).get(NotesControlCommand.NOTE_ID_OPTION).complete("", 0, new ArrayList<CharSequence>());
        verify(noteIdsFinder).findCompletions();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOptionCompletersIsEmpty() {
        assertThat(notesControlCommand.getOptionCompleters().size(), is(0));
    }

}
