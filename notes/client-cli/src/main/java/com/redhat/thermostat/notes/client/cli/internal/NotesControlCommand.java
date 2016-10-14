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

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.CompletionFinderTabCompleter;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotesControlCommand extends AbstractCommand implements CompleterService {

    public static final String COMMAND_NAME = "notes";
    static final CliCommandOption NOTE_ID_OPTION = new CliCommandOption("n", "noteId", true, "Note ID", false);

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private NoteIdsFinder noteIdsFinder;

    private AddNoteSubcommand addNoteCommand;
    private DeleteNoteSubcommand deleteNoteCommand;
    private UpdateNoteSubcommand updateNoteCommand;
    private ListNotesSubcommand listNotesCommand;

    public NotesControlCommand(NoteIdsFinder noteIdsFinder, AddNoteSubcommand addNoteCommand,
                               DeleteNoteSubcommand deleteNoteCommand, UpdateNoteSubcommand updateNoteCommand,
                               ListNotesSubcommand listNotesCommand) {
        this.noteIdsFinder = noteIdsFinder;
        this.addNoteCommand = addNoteCommand;
        this.deleteNoteCommand = deleteNoteCommand;
        this.updateNoteCommand = updateNoteCommand;
        this.listNotesCommand = listNotesCommand;
    }

    @Override
    public Set<String> getCommands() {
        return Collections.singleton(COMMAND_NAME);
    }

    @Override
    public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Map<CliCommandOption, ? extends TabCompleter>> getSubcommandCompleters() {
        Map<CliCommandOption, ? extends TabCompleter> noteIdCompletion =
                Collections.singletonMap(NOTE_ID_OPTION, new CompletionFinderTabCompleter(noteIdsFinder));

        Map<String, Map<CliCommandOption, ? extends TabCompleter>> completions = new HashMap<>();
        completions.put(UpdateNoteSubcommand.SUBCOMMAND_NAME, noteIdCompletion);
        completions.put(DeleteNoteSubcommand.SUBCOMMAND_NAME, noteIdCompletion);
        return completions;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        List<String> nonOptionArgs = args.getNonOptionArguments();
        if (nonOptionArgs.isEmpty()) {
            throw new CommandException(translator.localize(LocaleResources.SUBCOMMAND_EXPECTED));
        }

        String subcommand = nonOptionArgs.get(0);
        switch (subcommand) {
            case AddNoteSubcommand.SUBCOMMAND_NAME:
                addNoteCommand.run(ctx);
                break;
            case DeleteNoteSubcommand.SUBCOMMAND_NAME:
                deleteNoteCommand.run(ctx);
                break;
            case UpdateNoteSubcommand.SUBCOMMAND_NAME:
                updateNoteCommand.run(ctx);
                break;
            case ListNotesSubcommand.SUBCOMMAND_NAME:
                listNotesCommand.run(ctx);
                break;
            default:
                throw new CommandException(translator.localize(LocaleResources.UNKNOWN_SUBCOMMAND, subcommand));
        }
    }

}
