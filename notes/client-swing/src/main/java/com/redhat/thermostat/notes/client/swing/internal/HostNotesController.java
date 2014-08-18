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

package com.redhat.thermostat.notes.client.swing.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.storage.core.HostRef;

public class HostNotesController extends NotesController<HostRef> {

    private HostRef host;
    private HostNoteDAO dao;
    private List<NoteViewModel> viewModels;

    public HostNotesController(Clock clock, HostNoteDAO dao, HostRef host, NotesView view) {
        super(clock, view);
        this.host = host;
        this.dao = dao;

        viewModels = new ArrayList<>();
    }

    @Override
    protected void localSaveNote(String noteId) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void remoteGetNotesFromStorage() {
        List<HostNote> hostNotes = dao.getFor(host);
        // TODO only apply diff of notes to reduce UI glitches/changes
        viewModels.clear();
        view.clearAll();

        for (int i = 0; i < hostNotes.size(); i++) {
            HostNote hostNote = hostNotes.get(i);
            NoteViewModel viewModel = new NoteViewModel(hostNote.getId(), hostNote.getTimeStamp(), hostNote.getContent());
            viewModels.add(viewModel);
            view.add(viewModel);
        }
    }

    @Override
    protected void remoteSaveNotes() {
        System.out.println("saving notes");
        // TODO check if we need any synchronization

        long timeStamp = clock.getRealTimeMillis();

        for (NoteViewModel viewModel : viewModels) {
            String oldContent = viewModel.text;
            String newContent = view.getContent(viewModel.tag);
            if (oldContent.equals(newContent)) {
                continue;
            }

            HostNote toUpdate = dao.getById(host, viewModel.tag);
            toUpdate.setTimeStamp(timeStamp);
            toUpdate.setContent(newContent);

            dao.update(toUpdate);
            System.out.println("saved a note");
        }
    }

    @Override
    protected void localAddNewNote() {
        System.out.println("adding new note");
        long timeStamp = clock.getRealTimeMillis();
        String content = "";
        HostNote note = createNewHostNote(timeStamp, content);

        NoteViewModel model = new NoteViewModel(note.getId(), timeStamp, content);
        viewModels.add(model);
        view.add(model);

        dao.add(note);
    }

    private HostNote createNewHostNote(long timeStamp, String text) {
        HostNote hostNote = new HostNote();
        hostNote.setAgentId(host.getAgentId());
        hostNote.setId(UUID.randomUUID().toString());
        hostNote.setTimeStamp(timeStamp);
        hostNote.setContent(text);
        return hostNote;
    }

    @Override
    protected void localUpdateNotesInView() {
        System.out.println("loading notes from storage");
        List<HostNote> hostNotes = dao.getFor(host);

        System.out.println("got " + hostNotes.size() + " notes");

        // TODO only apply diff of notes to reduce UI glitches/changes
        viewModels.clear();
        view.clearAll();

        for (int i = 0; i < hostNotes.size(); i++) {
            HostNote hostNote = hostNotes.get(i);
            NoteViewModel viewModel = new NoteViewModel(hostNote.getId(), hostNote.getTimeStamp(), hostNote.getContent());
            viewModels.add(viewModel);
            view.add(viewModel);
        }
    }

    // FIXME @Override
    protected void updateNotesInStorage() {
        System.out.println("saving notes");
        // TODO check if we need any synchronization

        long timeStamp = clock.getRealTimeMillis();

        for (NoteViewModel viewModel : viewModels) {
            String oldContent = viewModel.text;
            String newContent = view.getContent(viewModel.tag);
            if (oldContent.equals(newContent)) {
                continue;
            }

            HostNote toUpdate = dao.getById(host, viewModel.tag);
            toUpdate.setTimeStamp(timeStamp);
            toUpdate.setContent(newContent);

            dao.update(toUpdate);
            System.out.println("saved a note");
        }
    }

    @Override
    protected void localDeleteNote(String noteId) {
        System.out.println("deleting note");

        dao.removeById(host, noteId);

        localUpdateNotesInView();
    }

}
