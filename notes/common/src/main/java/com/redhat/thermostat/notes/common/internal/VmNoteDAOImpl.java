/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.notes.common.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;

import static com.redhat.thermostat.common.utils.IteratorUtils.asList;
import static com.redhat.thermostat.common.utils.IteratorUtils.head;

class VmNoteDAOImpl implements VmNoteDAO {

    static final Key<String> KEY_CONTENT = new Key<>("content");
    static final Key<String> KEY_ID = new Key<>("id");

    static Category<VmNote> vmNotesCategory = new Category<>("vm-notes", VmNote.class,
            Key.AGENT_ID, Key.VM_ID, KEY_ID, Key.TIMESTAMP, KEY_CONTENT);

    static final String ADD_VM_NOTE = ""
            + "ADD " + vmNotesCategory.getName() + " "
            + "SET 'agentId' = ?s ,"
            + "    'vmId' = ?s ,"
            + "    'id' = ?s ,"
            + "    'timeStamp' = ?l ,"
            + "    'content' = ?s";

    static final String QUERY_VM_NOTES_BY_VM_ID = ""
            + "QUERY " + vmNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s"
            + "  AND 'vmId' = ?s";

    static final String QUERY_VM_NOTE_BY_ID = ""
            + "QUERY " + vmNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s"
            + "  AND 'vmId' = ?s"
            + "  AND 'id' = ?s";

    static final String UPDATE_VM_NOTE = ""
            + "UPDATE " + vmNotesCategory.getName() + " "
            + "   SET 'timeStamp' = ?l ,"
            + "       'content' = ?s "
            + " WHERE 'agentId' = ?s "
            + "   AND 'vmId' = ?s "
            + "   AND 'id' = ?s";

    static final String REMOVE_VM_NOTE_BY_ID = ""
            + "REMOVE " + vmNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s "
            + "  AND 'vmId' = ?s "
            + "  AND 'id' = ?s";

    private static final Logger logger = LoggingUtils.getLogger(VmNoteDAOImpl.class);

    private Storage storage;

    public VmNoteDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmNotesCategory);
    }

    @Override
    public void add(VmNote vmNote) {
        StatementDescriptor<VmNote> desc = new StatementDescriptor<>(vmNotesCategory, ADD_VM_NOTE);
        PreparedStatement<VmNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vmNote.getAgentId());
            stmt.setString(1, vmNote.getVmId());
            stmt.setString(2, vmNote.getId());
            stmt.setLong(3, vmNote.getTimeStamp());
            stmt.setString(4, vmNote.getContent());
            stmt.execute();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
        }
    }

    @Override
    public List<VmNote> getFor(VmRef vm) {
        StatementDescriptor<VmNote> desc = new StatementDescriptor<>(vmNotesCategory, QUERY_VM_NOTES_BY_VM_ID);
        PreparedStatement<VmNote> stmt;
        Cursor<VmNote> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vm.getHostRef().getAgentId());
            stmt.setString(1, vm.getVmId());
            cursor = stmt.executeQuery();
            return asList(cursor);
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
    }

    @Override
    public VmNote getById(VmRef vm, String id) {
        StatementDescriptor<VmNote> desc = new StatementDescriptor<>(vmNotesCategory, QUERY_VM_NOTE_BY_ID);
        PreparedStatement<VmNote> stmt;
        Cursor<VmNote> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vm.getHostRef().getAgentId());
            stmt.setString(1, vm.getVmId());
            stmt.setString(2, id);
            cursor = stmt.executeQuery();
            return head(cursor);
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
    }

    @Override
    public void update(VmNote note) {
        Objects.requireNonNull(note.getId());

        StatementDescriptor<VmNote> desc = new StatementDescriptor<>(vmNotesCategory, UPDATE_VM_NOTE);
        PreparedStatement<VmNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setLong(0, note.getTimeStamp());
            stmt.setString(1, note.getContent());
            stmt.setString(2, note.getAgentId());
            stmt.setString(3, note.getVmId());
            stmt.setString(4, note.getId());
            stmt.execute();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
        }
    }

    @Override
    public void removeById(VmRef ref, String noteId) {
        remove(ref.getHostRef().getAgentId(), ref.getVmId(), noteId);
    }

    @Override
    public void remove(VmNote note) {
        remove(note.getAgentId(), note.getVmId(), note.getId());
    }

    private void remove(String agentId, String vmId, String noteId) {
        StatementDescriptor<VmNote> desc = new StatementDescriptor<>(vmNotesCategory, REMOVE_VM_NOTE_BY_ID);
        PreparedStatement<VmNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, agentId);
            stmt.setString(1, vmId);
            stmt.setString(2, noteId);
            stmt.execute();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
        }
    }
}
