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

package com.redhat.thermostat.notes.common.internal;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.dao.SimpleDaoQuery;
import com.redhat.thermostat.storage.model.AggregateCount;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

class VmNoteDAOImpl extends AbstractDao implements VmNoteDAO {

    final Category<AggregateCount> aggregateCountCategory;

    static final String ADD_VM_NOTE = ""
            + "ADD " + vmNotesCategory.getName() + " "
            + "SET 'agentId' = ?s ,"
            + "    'vmId' = ?s ,"
            + "    'id' = ?s ,"
            + "    'timeStamp' = ?l ,"
            + "    'content' = ?s";

    static final String QUERY_ALL_VM_NOTES = "" +
            "QUERY " + vmNotesCategory.getName();

    static final String QUERY_COUNT_VM_NOTES_BY_VM_ID = ""
            + "QUERY-COUNT " + vmNotesCategory.getName() + " "
            + "WHERE 'vmId' = ?s";

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
        CategoryAdapter<VmNote, AggregateCount> adapter = new CategoryAdapter<>(vmNotesCategory);
        aggregateCountCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(aggregateCountCategory);
    }

    @Override
    public void add(final VmNote vmNote) {
        executeStatement(new AbstractDaoStatement<VmNote>(storage, vmNotesCategory, ADD_VM_NOTE) {
            @Override
            public PreparedStatement<VmNote> customize(PreparedStatement<VmNote> preparedStatement) {
                preparedStatement.setString(0, vmNote.getAgentId());
                preparedStatement.setString(1, vmNote.getVmId());
                preparedStatement.setString(2, vmNote.getId());
                preparedStatement.setLong(3, vmNote.getTimeStamp());
                preparedStatement.setString(4, vmNote.getContent());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<VmNote> getAll() {
        return executeQuery(new SimpleDaoQuery<>(storage, vmNotesCategory, QUERY_ALL_VM_NOTES)).asList();
    }

    @Override
    public long getCount(final VmRef ref) {
        return executeQuery(new AbstractDaoQuery<AggregateCount>(storage, aggregateCountCategory, QUERY_COUNT_VM_NOTES_BY_VM_ID) {
            @Override
            public PreparedStatement<AggregateCount> customize(PreparedStatement<AggregateCount> preparedStatement) {
                preparedStatement.setString(0, ref.getVmId());
                return preparedStatement;
            }
        }).head().getCount();
    }

    @Override
    public List<VmNote> getFor(final VmRef vm) {
        return executeQuery(new AbstractDaoQuery<VmNote>(storage, vmNotesCategory, QUERY_VM_NOTES_BY_VM_ID) {
            @Override
            public PreparedStatement<VmNote> customize(PreparedStatement<VmNote> preparedStatement) {
                preparedStatement.setString(0, vm.getHostRef().getAgentId());
                preparedStatement.setString(1, vm.getVmId());
                return preparedStatement;
            }
        }).asList();
    }

    @Override
    public VmNote getById(final VmRef vm, final String id) {
        return executeQuery(new AbstractDaoQuery<VmNote>(storage, vmNotesCategory, QUERY_VM_NOTE_BY_ID) {
            @Override
            public PreparedStatement<VmNote> customize(PreparedStatement<VmNote> preparedStatement) {
                preparedStatement.setString(0, vm.getHostRef().getAgentId());
                preparedStatement.setString(1, vm.getVmId());
                preparedStatement.setString(2, id);
                return preparedStatement;
            }
        }).head();
    }

    @Override
    public void update(final VmNote note) {
        Objects.requireNonNull(note.getId());
        executeStatement(new AbstractDaoStatement<VmNote>(storage, vmNotesCategory, UPDATE_VM_NOTE) {
            @Override
            public PreparedStatement<VmNote> customize(PreparedStatement<VmNote> preparedStatement) {
                preparedStatement.setLong(0, note.getTimeStamp());
                preparedStatement.setString(1, note.getContent());
                preparedStatement.setString(2, note.getAgentId());
                preparedStatement.setString(3, note.getVmId());
                preparedStatement.setString(4, note.getId());
                return preparedStatement;
            }
        });
    }

    @Override
    public void removeById(VmRef ref, String noteId) {
        remove(ref.getHostRef().getAgentId(), ref.getVmId(), noteId);
    }

    @Override
    public void remove(VmNote note) {
        remove(note.getAgentId(), note.getVmId(), note.getId());
    }

    private void remove(final String agentId, final String vmId, final String noteId) {
        executeStatement(new AbstractDaoStatement<VmNote>(storage, vmNotesCategory, REMOVE_VM_NOTE_BY_ID) {
            @Override
            public PreparedStatement<VmNote> customize(PreparedStatement<VmNote> preparedStatement) {
                preparedStatement.setString(0, agentId);
                preparedStatement.setString(1, vmId);
                preparedStatement.setString(2, noteId);
                return preparedStatement;
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
