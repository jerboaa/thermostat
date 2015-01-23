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
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class HostNoteDAOImpl implements HostNoteDAO {

    static final Key<String> KEY_CONTENT = new Key<>("content");
    static final Key<String> KEY_ID = new Key<>("id");

    static Category<HostNote> hostNotesCategory = new Category<>("host-notes", HostNote.class,
            Key.AGENT_ID, KEY_ID, Key.TIMESTAMP, KEY_CONTENT);

    static final String ADD_HOST_NOTE = ""
            + "ADD " + hostNotesCategory.getName() + " "
            + "SET 'agentId' = ?s ,"
            + "    'id' = ?s ,"
            + "    'timeStamp' = ?l ,"
            + "    'content' = ?s";

    static final String QUERY_HOST_NOTES_BY_VM_ID = ""
            + "QUERY " + hostNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s";

    static final String QUERY_HOST_NOTE_BY_ID = ""
            + "QUERY " + hostNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s"
            + "  AND 'id' = ?s";

    static final String UPDATE_HOST_NOTE = ""
            + "UPDATE " + hostNotesCategory.getName() + " "
            + "   SET 'timeStamp' = ?l ,"
            + "       'content' = ?s "
            + " WHERE 'agentId' = ?s "
            + "   AND 'id' = ?s";

    static final String REMOVE_HOST_NOTE_BY_ID = ""
            + "REMOVE " + hostNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s "
            + "  AND 'id' = ?s";

    private static final Logger logger = LoggingUtils.getLogger(VmNoteDAOImpl.class);

    private Storage storage;

    public HostNoteDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(hostNotesCategory);
    }

    @Override
    public void add(HostNote vmNote) {
        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(hostNotesCategory, ADD_HOST_NOTE);
        PreparedStatement<HostNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vmNote.getAgentId());
            stmt.setString(1, vmNote.getId());
            stmt.setLong(2, vmNote.getTimeStamp());
            stmt.setString(3, vmNote.getContent());
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
    public List<HostNote> getFor(HostRef host) {
        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(hostNotesCategory, QUERY_HOST_NOTES_BY_VM_ID);
        PreparedStatement<HostNote> stmt;
        Cursor<HostNote> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, host.getAgentId());
            cursor = stmt.executeQuery();
            List<HostNote> results = new ArrayList<>();
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
            return results;
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
    public HostNote getById(HostRef host, String id) {
        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(hostNotesCategory, QUERY_HOST_NOTE_BY_ID);
        PreparedStatement<HostNote> stmt;
        Cursor<HostNote> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, host.getAgentId());
            stmt.setString(1, id);
            cursor = stmt.executeQuery();
            if (cursor.hasNext()) {
                return cursor.next();
            }
            return null;
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
    public void update(HostNote note) {
        Objects.requireNonNull(note.getId());

        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(hostNotesCategory, UPDATE_HOST_NOTE);
        PreparedStatement<HostNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setLong(0, note.getTimeStamp());
            stmt.setString(1, note.getContent());
            stmt.setString(2, note.getAgentId());
            stmt.setString(3, note.getId());
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
    public void removeById(HostRef host, String noteId) {
        remove(host.getAgentId(), noteId);
    }

    @Override
    public void remove(HostNote note) {
        remove(note.getAgentId(), note.getId());
    }

    private void remove(String agentId, String noteId) {
        StatementDescriptor<HostNote> desc = new StatementDescriptor<>(hostNotesCategory, REMOVE_HOST_NOTE_BY_ID);
        PreparedStatement<HostNote> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, agentId);
            stmt.setString(1, noteId);
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
