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
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class ListNotesSubcommand extends NotesSubcommand {

    static final String SUBCOMMAND_NAME = "list";
    static final String SHOW_NOTE_ID_ARGUMENT = "show-note-id";
    static final String QUIET_ARGUMENT = "quiet";

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        assertExpectedAgentAndVmArgsProvided(args);

        EnumSet<Field> enabledFields = EnumSet.of(Field.TIMESTAMP, Field.CONTENT);

        if (ctx.getArguments().hasArgument(SHOW_NOTE_ID_ARGUMENT)) {
            enabledFields.add(Field.NOTE_ID);
        }

        if (ctx.getArguments().hasArgument(QUIET_ARGUMENT)) {
            enabledFields.clear();
            enabledFields.add(Field.NOTE_ID);
        }

        List<? extends Note> notes;
        if (args.hasArgument(VmArgument.ARGUMENT_NAME)) {
            VmInfoDAO vmInfoDao = services.getRequiredService(VmInfoDAO.class);

            VmId vmId = VmArgument.required(args).getVmId();
            checkVmExists(vmId);
            AgentId agentId = new AgentId(vmInfoDao.getVmInfo(vmId).getAgentId());
            checkAgentExists(agentId);
            notes = getVmNotes(getVmRefFromVmId(vmId));
        } else {
            AgentId agentId = AgentArgument.required(args).getAgentId();
            checkAgentExists(agentId);
            notes = getHostNotes(getHostRefFromAgentId(agentId));
        }

        Collections.sort(notes, new Comparator<Note>() {
            @Override
            public int compare(Note t1, Note t2) {
                return Long.compare(t1.getTimeStamp(), t2.getTimeStamp());
            }
        });
        printTable(ctx.getConsole().getOutput(), enabledFields, notes);
    }

    private List<VmNote> getVmNotes(VmRef vmRef) throws CommandException {
        return services.getRequiredService(VmNoteDAO.class).getFor(vmRef);
    }

    private List<HostNote> getHostNotes(HostRef hostRef) throws CommandException {
        return services.getRequiredService(HostNoteDAO.class).getFor(hostRef);
    }

    private void printTable(PrintStream printStream, Collection<Field> enabledFields, List<? extends Note> notes) {
        if (notes.isEmpty()) {
            return;
        }
        TableRenderer renderer = new TableRenderer(enabledFields.size());

        List<String> header = new ArrayList<>();
        for (Field field : enabledFields) {
            header.add(field.getHeader());
        }
        renderer.printHeader(header.toArray(new String[header.size()]));

        for (Note note : notes) {
            List<String> fields = new ArrayList<>();
            for (Field field : enabledFields) {
                fields.add(field.extractFrom(note));
            }
            renderer.printLine(fields.toArray(new String[fields.size()]));
        }

        renderer.render(printStream);
    }

    private enum Field {
        NOTE_ID(translator.localize(LocaleResources.NOTE_ID_COLUMN), new NoteIdMapper()),
        TIMESTAMP(translator.localize(LocaleResources.TIMESTAMP_COLUMN), new TimestampMapper()),
        CONTENT(translator.localize(LocaleResources.CONTENT_COLUMN), new ContentMapper()),;

        private final LocalizedString columnName;
        private final Function<Note, String> cellProvider;

        Field(LocalizedString columnName, Function<Note, String> cellProvider) {
            this.columnName = columnName;
            this.cellProvider = cellProvider;
        }

        public String getHeader() {
            return columnName.getContents();
        }

        public String extractFrom(Note note) {
            return cellProvider.apply(note);
        }
    }

    interface Function<I, O> {
        O apply(I i);
    }

    private static class NoteIdMapper implements Function<Note, String> {
        @Override
        public String apply(Note note) {
            return note.getId();
        }
    }

    private static class TimestampMapper implements Function<Note, String> {
        @Override
        public String apply(Note note) {
            return Clock.DEFAULT_DATE_FORMAT.format(new Date(note.getTimeStamp()));
        }
    }

    private static class ContentMapper implements Function<Note, String> {
        @Override
        public String apply(Note note) {
            return note.getContent();
        }
    }

}
