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

package com.redhat.thermostat.thread.dao.internal;

import static com.redhat.thermostat.common.utils.IteratorUtils.head;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Id;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.experimental.statement.BeanAdapter;
import com.redhat.thermostat.storage.core.experimental.statement.BeanAdapterBuilder;
import com.redhat.thermostat.storage.core.experimental.statement.Query;
import com.redhat.thermostat.storage.core.experimental.statement.QueryValues;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.storage.dao.BaseCountable;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.dao.internal.statement.SessionQueries;
import com.redhat.thermostat.thread.dao.internal.statement.StateQueries;
import com.redhat.thermostat.thread.dao.internal.statement.SummaryQuery;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadDaoImpl extends BaseCountable implements ThreadDao {
    
    private static final Logger logger = LoggingUtils.getLogger(ThreadDaoImpl.class);

    static final BeanAdapter<ThreadSummary> ThreadSummaryAdapter = new BeanAdapterBuilder<>(ThreadSummary.class, new SummaryQuery()).build();
    static final BeanAdapter<ThreadSession> ThreadSessionAdapter = new BeanAdapterBuilder<>(ThreadSession.class, SessionQueries.asList()).build();
    static final BeanAdapter<ThreadState> ThreadStateAdapter = new BeanAdapterBuilder<>(ThreadState.class, StateQueries.asList()).build();

    static final String QUERY_LATEST_HARVESTING_STATUS = "QUERY "
            + THREAD_HARVESTING_STATUS.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String QUERY_LATEST_DEADLOCK_INFO = "QUERY "
            + DEADLOCK_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String AGGREGATE_COUNT_ALL_DEADLOCKS = "QUERY-COUNT " +
            DEADLOCK_INFO.getName();

    static final String DESC_ADD_THREAD_HARVESTING_STATUS = "ADD " + THREAD_HARVESTING_STATUS.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + HARVESTING_STATUS_KEY.getName() + "' = ?b";

    static final String DESC_ADD_THREAD_DEADLOCK_DATA = "ADD " + DEADLOCK_INFO.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + DEADLOCK_DESCRIPTION_KEY.getName() + "' = ?s";


    private final Category<AggregateCount> aggregateCategory;
    private Storage storage;
    
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;

        ThreadDaoCategories.register(storage);

        storage.registerCategory(THREAD_HARVESTING_STATUS);

        storage.registerCategory(DEADLOCK_INFO);

        CategoryAdapter<VmDeadLockData, AggregateCount> adapter = new CategoryAdapter<>(DEADLOCK_INFO);
        aggregateCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(aggregateCategory);
    }

    @Override
    public void saveSummary(ThreadSummary summary) {
        try {
            ThreadSummaryAdapter.insert(summary, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception saving summary: " + summary, e);
        }
    }

    @Override
    public void addThreadState(ThreadState thread) {
        try {
            ThreadStateAdapter.insert(thread, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception saving thread state: " + thread, e);
        }
    }

    @Override
    public void getThreadStates(VmRef ref, SessionID session,
                                final ResultHandler<ThreadState> handler,
                                Range<Long> range, int limit, Sort order)
    {
        Id id = order.equals(Sort.ASCENDING) ? StateQueries.getAscending :
                                               StateQueries.getDescending;

        Query<ThreadState> query = ThreadStateAdapter.getQuery(id);

        QueryValues values = query.createValues();
        values.set(StateQueries.CriteriaId.vmId, ref.getVmId());
        values.set(StateQueries.CriteriaId.agentId, ref.getHostRef().getAgentId());
        values.set(StateQueries.CriteriaId.sessionID, session.get());

        values.set(StateQueries.CriteriaId.timeStampGEQ, range.getMin());
        values.set(StateQueries.CriteriaId.timeStampLEQ, range.getMax());
        values.set(StateQueries.CriteriaId.limit, limit);

        try {
            ThreadStateAdapter.query(values, handler, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception retrieving thread summary", e);
        }
    }

    @Override
    public List<ThreadSummary> getSummary(VmRef ref, Range<Long> range, int limit) {
        final List<ThreadSummary> results = new ArrayList<>();

        Query<ThreadSummary> query = ThreadSummaryAdapter.getQuery(SummaryQuery.id);

        QueryValues values = query.createValues();
        values.set(SummaryQuery.CriteriaId.vmId, ref.getVmId());
        values.set(SummaryQuery.CriteriaId.agentId, ref.getHostRef().getAgentId());

        values.set(SummaryQuery.CriteriaId.timeStampGEQ, range.getMin());
        values.set(SummaryQuery.CriteriaId.timeStampLEQ, range.getMax());
        values.set(SummaryQuery.CriteriaId.limit, limit);

        try {
            ThreadSummaryAdapter.query(values, new ResultHandler<ThreadSummary>() {
                @Override
                public boolean onResult(ThreadSummary result) {
                    results.add(result);
                    return true;
                }
            }, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception retrieving thread summary", e);
        }

        return results;
    }

    @Override
    public List<ThreadSession> getSessions(VmRef ref, Range<Long> range,
                                           int limit, Sort order)
    {
        final List<ThreadSession> results = new ArrayList<>();

        Id id = order.equals(Sort.ASCENDING) ? SessionQueries.getRangeAscending :
                                               SessionQueries.getRangeDescending;

        Query<ThreadSession> query = ThreadSessionAdapter.getQuery(id);

        QueryValues values = query.createValues();
        values.set(SessionQueries.CriteriaId.vmId, ref.getVmId());
        values.set(SessionQueries.CriteriaId.agentId, ref.getHostRef().getAgentId());

        values.set(SessionQueries.CriteriaId.timeStampGEQ, range.getMin());
        values.set(SessionQueries.CriteriaId.timeStampLEQ, range.getMax());
        values.set(SessionQueries.CriteriaId.limit, limit);

        try {
            ThreadSessionAdapter.query(values, new ResultHandler<ThreadSession>() {
                @Override
                public boolean onResult(ThreadSession result) {
                    results.add(result);
                    return true;
                }
            }, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception retrieving thread session", e);
        }

        return results;
    }

    public void saveSession(ThreadSession session) {
        try {
            ThreadSessionAdapter.insert(session, storage);

        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Exception saving session: " + session, e);
        }
    }

    @Override
    public void saveHarvestingStatus(ThreadHarvestingStatus status) {
        StatementDescriptor<ThreadHarvestingStatus> desc = new StatementDescriptor<>(THREAD_HARVESTING_STATUS, DESC_ADD_THREAD_HARVESTING_STATUS);
        PreparedStatement<ThreadHarvestingStatus> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, status.getAgentId());
            prepared.setString(1, status.getVmId());
            prepared.setLong(2, status.getTimeStamp());
            prepared.setBoolean(3, status.isHarvesting());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public ThreadHarvestingStatus getLatestHarvestingStatus(VmRef vm) {
        PreparedStatement<ThreadHarvestingStatus> stmt = prepareQuery(THREAD_HARVESTING_STATUS, 
                QUERY_LATEST_HARVESTING_STATUS, vm);
        if (stmt == null) {
            return null;
        }
        
        return getFirstResult(stmt);
    }

    @Override
    public VmDeadLockData loadLatestDeadLockStatus(VmRef ref) {
        PreparedStatement<VmDeadLockData> stmt = prepareQuery(DEADLOCK_INFO, QUERY_LATEST_DEADLOCK_INFO, ref);
        if (stmt == null) {
            return null;
        }
        
        return getFirstResult(stmt);
    }

    @Override
    public void saveDeadLockStatus(VmDeadLockData deadLockInfo) {
        StatementDescriptor<VmDeadLockData> desc = new StatementDescriptor<>(DEADLOCK_INFO, DESC_ADD_THREAD_DEADLOCK_DATA);
        PreparedStatement<VmDeadLockData> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, deadLockInfo.getAgentId());
            prepared.setString(1, deadLockInfo.getVmId());
            prepared.setLong(2, deadLockInfo.getTimeStamp());
            prepared.setString(3, deadLockInfo.getDeadLockDescription());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public long getDeadLockCount() {
        return getCount(storage, aggregateCategory, AGGREGATE_COUNT_ALL_DEADLOCKS);
    }


    /**************************************************************************/

    private <T extends Pojo> PreparedStatement<T> prepareQuery(Category<T> category, String query, VmRef ref) {
        return prepareQuery(category, query, ref, null, null);
    }
    
    private <T extends Pojo> PreparedStatement<T> prepareQuery(Category<T> category, String query, VmRef ref, Long since, Long to) {
        StatementDescriptor<T> desc = new StatementDescriptor<>(category, query);
        PreparedStatement<T> stmt = null;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getHostRef().getAgentId());
            stmt.setString(1, ref.getVmId());
            // assume: the format of the query is such that 2nd and 3rd arguments (if any) are longs
            if (since != null) {
                stmt.setLong(2, since);
            }
            if (to != null) {
                stmt.setLong(3, to);
            }
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        }
        return stmt;
    }

    private <T extends Pojo> T getFirstResult(PreparedStatement<T> stmt) {
        Cursor<T> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return null;
        }

        return head(cursor);
    }
}

