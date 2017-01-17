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

package com.redhat.thermostat.notes.client.core;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.VmRef;

public class VmNotesController extends NotesController<VmRef, VmNote, VmNoteDAO> {

    public VmNotesController(Clock clock, ApplicationService appSvc, VmNoteDAO vmNoteDao, VmRef vm, NotesViewProvider notesViewProvider) {
        super(clock, appSvc, vm, vmNoteDao, notesViewProvider);
    }

    @Override
    protected VmNote createNewNote(long timeStamp, String text) {
        VmNote vmNote = new VmNote();
        vmNote.setAgentId(ref.getHostRef().getAgentId());
        vmNote.setVmId(ref.getVmId());
        vmNote.setId(createNoteId());
        vmNote.setTimeStamp(timeStamp);
        vmNote.setContent(text);
        return vmNote;
    }

    @Override
    protected VmNote copyNote(VmNote note) {
        VmNote copy = new VmNote();
        copy.setAgentId(note.getAgentId());
        copy.setVmId(note.getVmId());
        copy.setTimeStamp(note.getTimeStamp());
        copy.setContent(note.getContent());
        copy.setId(createNoteId());
        return copy;
    }

}
