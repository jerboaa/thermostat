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

package com.redhat.thermostat.notes.common.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.notes.common.Notes;
import com.redhat.thermostat.notes.common.NotesDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;

class NotesDAOImpl implements NotesDAO {

    public static final Key<String> content = new Key<>("content");

    public static Category<Notes> vmNotesCategory = new Category<>("vm-notes", Notes.class,
            Key.TIMESTAMP, Key.AGENT_ID, Key.VM_ID, content);

    public static final String ADD_VM_NOTES = ""
            + "ADD " + vmNotesCategory.getName() + " "
            + "SET 'agentId' = ?s ,"
            + "    'vmId' = ?s ,"
            + "    'timeStamp' = ?l ,"
            + "    'content' = ?s";

    public static final String QUERY_VM_NOTES = ""
            + "QUERY " + vmNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s"
            + "  AND 'vmId' = ?s" + " "
            + "SORT 'timeStamp' DSC LIMIT 1";

    private static final Logger logger = LoggingUtils.getLogger(NotesDAOImpl.class);

    private Storage storage;

    public NotesDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmNotesCategory);
    }

    @Override
    public Notes get(VmRef vm) {
        StatementDescriptor<Notes> desc = new StatementDescriptor<>(vmNotesCategory, QUERY_VM_NOTES);
        PreparedStatement<Notes> stmt;
        Cursor<Notes> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vm.getHostRef().getAgentId());
            stmt.setString(1, vm.getVmId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        Notes result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }
        return result;
    }

    @Override
    public void put(Notes notes) {
        StatementDescriptor<Notes> desc = new StatementDescriptor<>(vmNotesCategory, ADD_VM_NOTES);
        PreparedStatement<Notes> stmt;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, notes.getAgentId());
            stmt.setString(1, notes.getVmId());
            stmt.setLong(2, notes.getTimeStamp());
            stmt.setString(3, notes.getContent());
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
