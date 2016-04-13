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

package com.redhat.thermostat.notes.common.internal;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.model.AggregateCount;

public class HostNoteDAOImpl extends AbstractDao implements HostNoteDAO {

    final Category<AggregateCount> aggregateCountCategory;

    static final String ADD_HOST_NOTE = ""
            + "ADD " + hostNotesCategory.getName() + " "
            + "SET 'agentId' = ?s ,"
            + "    'id' = ?s ,"
            + "    'timeStamp' = ?l ,"
            + "    'content' = ?s";

    static final String QUERY_COUNT_HOST_NOTES_BY_AGENT_ID = ""
            + "QUERY-COUNT " + hostNotesCategory.getName() + " "
            + "WHERE 'agentId' = ?s";

    static final String QUERY_HOST_NOTES_BY_AGENT_ID = ""
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
        CategoryAdapter<HostNote, AggregateCount> adapter = new CategoryAdapter<>(hostNotesCategory);
        aggregateCountCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(aggregateCountCategory);
    }

    @Override
    public void add(final HostNote hostNote) {
        executeStatement(new AbstractDaoStatement<HostNote>(storage, hostNotesCategory, ADD_HOST_NOTE) {
            @Override
            public PreparedStatement<HostNote> customize(PreparedStatement<HostNote> preparedStatement) {
                preparedStatement.setString(0, hostNote.getAgentId());
                preparedStatement.setString(1, hostNote.getId());
                preparedStatement.setLong(2, hostNote.getTimeStamp());
                preparedStatement.setString(3, hostNote.getContent());
                return preparedStatement;
            }
        });
    }

    @Override
    public long getCount(final HostRef ref) {
        return executeQuery(new AbstractDaoQuery<AggregateCount>(storage, aggregateCountCategory, QUERY_COUNT_HOST_NOTES_BY_AGENT_ID) {
            @Override
            public PreparedStatement<AggregateCount> customize(PreparedStatement<AggregateCount> preparedStatement) {
                preparedStatement.setString(0, ref.getAgentId());
                return preparedStatement;
            }
        }).head().getCount();
    }

    @Override
    public List<HostNote> getFor(final HostRef host) {
        return executeQuery(new AbstractDaoQuery<HostNote>(storage, hostNotesCategory, QUERY_HOST_NOTES_BY_AGENT_ID) {
            @Override
            public PreparedStatement<HostNote> customize(PreparedStatement<HostNote> preparedStatement) {
                preparedStatement.setString(0, host.getAgentId());
                return preparedStatement;
            }
        }).asList();
    }

    @Override
    public HostNote getById(final HostRef host, final String id) {
        return executeQuery(new AbstractDaoQuery<HostNote>(storage, hostNotesCategory, QUERY_HOST_NOTE_BY_ID) {
            @Override
            public PreparedStatement<HostNote> customize(PreparedStatement<HostNote> preparedStatement) {
                preparedStatement.setString(0, host.getAgentId());
                preparedStatement.setString(1, id);
                return preparedStatement;
            }
        }).head();
    }

    @Override
    public void update(final HostNote note) {
        Objects.requireNonNull(note.getId());
        executeStatement(new AbstractDaoStatement<HostNote>(storage, hostNotesCategory, UPDATE_HOST_NOTE) {
            @Override
            public PreparedStatement<HostNote> customize(PreparedStatement<HostNote> preparedStatement) {
                preparedStatement.setLong(0, note.getTimeStamp());
                preparedStatement.setString(1, note.getContent());
                preparedStatement.setString(2, note.getAgentId());
                preparedStatement.setString(3, note.getId());
                return preparedStatement;
            }
        });
    }

    @Override
    public void removeById(HostRef host, String noteId) {
        remove(host.getAgentId(), noteId);
    }

    @Override
    public void remove(HostNote note) {
        remove(note.getAgentId(), note.getId());
    }

    private void remove(final String agentId, final String noteId) {
        executeStatement(new AbstractDaoStatement<HostNote>(storage, hostNotesCategory, REMOVE_HOST_NOTE_BY_ID) {
            @Override
            public PreparedStatement<HostNote> customize(PreparedStatement<HostNote> preparedStatement) {
                preparedStatement.setString(0, agentId);
                preparedStatement.setString(1, noteId);
                return preparedStatement;
            }
        });
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}
