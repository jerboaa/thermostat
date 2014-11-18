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

package com.redhat.thermostat.thread.dao.impl;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.dao.impl.descriptor.SummaryDescriptor;
import com.redhat.thermostat.thread.dao.impl.descriptor.SummaryDescriptorBuilder;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadContentionSample;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadDaoImpl implements ThreadDao {
    
    private static final Logger logger = LoggingUtils.getLogger(ThreadDaoImpl.class);

    static final SummaryDescriptor SUMMARY = new SummaryDescriptorBuilder().build();

    // Queries

    static final String QUERY_LATEST_HARVESTING_STATUS = "QUERY "
            + THREAD_HARVESTING_STATUS.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String QUERY_LATEST_THREAD_STATE_FOR_THREAD = "QUERY "
            + THREAD_STATE.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + THREAD_HEADER_UUID.getName() + "' = ?s SORT '"
            + THREAD_PROBE_END.getName() + "' DSC LIMIT 1";

    static final String QUERY_FIRST_THREAD_STATE_FOR_THREAD = "QUERY "
            + THREAD_STATE.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + THREAD_HEADER_UUID.getName() + "' = ?s SORT '"
            + THREAD_PROBE_START.getName() + "' ASC LIMIT 1";

    static final String QUERY_OLDEST_THREAD_STATE = "QUERY "
            + THREAD_STATE.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + THREAD_PROBE_START.getName() + "' ASC LIMIT 1";
    
    static final String QUERY_LATEST_THREAD_STATE= "QUERY "
            + THREAD_STATE.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + THREAD_PROBE_END.getName() + "' DSC LIMIT 1";
    
    static final String QUERY_LATEST_DEADLOCK_INFO = "QUERY "
            + DEADLOCK_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    static final String QUERY_THREAD_HEADER = "QUERY "
            + THREAD_HEADER.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + THREAD_NAME_KEY.getName() + "' = ?s AND '"
            + THREAD_ID_KEY.getName() + "' = ?l LIMIT 1";
    static final String QUERY_ALL_THREAD_HEADERS = "QUERY "
            + THREAD_HEADER.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";

    static final String QUERY_THREAD_STATE_PER_THREAD = "QUERY "
            + THREAD_STATE.getName() + " WHERE '"
            + THREAD_HEADER_UUID.getName() + "' = ?s AND '"
            + THREAD_PROBE_END.getName() + "' >= ?l AND '"
            + THREAD_PROBE_START.getName() + "' <= ?l SORT '"
            + THREAD_PROBE_START.getName() + "' ASC";

    // Data modifying descriptors

    // ADD vm-thread-harvesting SET 'agentId' = ?s , \
    //                              'vmId' = ?s , \
    //                              'timeStamp' = ?l , \
    //                              'harvesting' = ?b
    static final String DESC_ADD_THREAD_HARVESTING_STATUS = "ADD " + THREAD_HARVESTING_STATUS.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + HARVESTING_STATUS_KEY.getName() + "' = ?b";

    // ADD vm-deadlock-data SET 'agentId' = ?s , \
    //                          'vmId' = ?s , \
    //                          'timeStamp' = ?l , \
    //                          'deadLockDescription' = ?s
    static final String DESC_ADD_THREAD_DEADLOCK_DATA = "ADD " + DEADLOCK_INFO.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + DEADLOCK_DESCRIPTION_KEY.getName() + "' = ?s";

    static final String ADD_THREAD_HEADER =
            "ADD " + THREAD_HEADER.getName() + " " +
            "SET '" + Key.AGENT_ID.getName() + "' = ?s , "    +
                "'" + Key.VM_ID.getName() + "' = ?s , "       +
                "'" + THREAD_NAME_KEY.getName() + "' = ?s , " +
                "'" + THREAD_ID_KEY.getName() + "' = ?l , "   +
                "'" + Key.TIMESTAMP.getName() + "' = ?l , "   +
                "'" + THREAD_HEADER_UUID.getName() + "' = ?s";

    static final String ADD_THREAD_STATE =
            "ADD "  + THREAD_STATE.getName() + " "               +
            "SET '" + Key.AGENT_ID.getName() + "' = ?s , "       +
                "'" + Key.VM_ID.getName() + "' = ?s , "          +
                "'" + THREAD_STATE_KEY.getName() + "' = ?s , "   +
                "'" + THREAD_PROBE_START.getName() + "' = ?l , " +
                "'" + THREAD_PROBE_END.getName() + "' = ?l , "   +
                "'" + THREAD_HEADER_UUID.getName() + "' = ?s";

    static final String DESC_UPDATE_THREAD_STATE =
            "UPDATE "  + THREAD_STATE.getName() + " "                 +
            "SET '"    + THREAD_PROBE_END.getName() + "' = ?l "       +
            "WHERE '"  + THREAD_HEADER_UUID.getName() + "' = ?s AND " +
                  "'"  + THREAD_PROBE_START.getName() + "' = ?l";

    static final String ADD_CONTENTION_SAMPLE =
            "ADD "  + THREAD_CONTENTION_SAMPLE.getName() + " "               +
                    "SET '" + Key.AGENT_ID.getName() + "' = ?s , "       +
                    "'" + Key.VM_ID.getName() + "' = ?s , "          +
                    "'" + THREAD_CONTENTION_BLOCKED_COUNT_KEY.getName() + "' = ?l , " +
                    "'" + THREAD_CONTENTION_BLOCKED_TIME_KEY.getName() + "' = ?l , "  +
                    "'" + THREAD_CONTENTION_WAITED_COUNT_KEY.getName() + "' = ?l , "  +
                    "'" + THREAD_CONTENTION_WAITED_TIME_KEY.getName() + "' = ?l , "  +
                    "'" + THREAD_HEADER_UUID.getName() + "' = ?s , " +
                    "'" + Key.TIMESTAMP.getName() + "' = ?l";

    static final String GET_LATEST_CONTENTION_SAMPLE= "QUERY "
            + THREAD_CONTENTION_SAMPLE.getName() + " WHERE '"
            + THREAD_HEADER_UUID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";

    private Storage storage;
    
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;

        storage.registerCategory(SUMMARY.getCategory());

        storage.registerCategory(THREAD_HARVESTING_STATUS);
        storage.registerCategory(THREAD_HEADER);
        storage.registerCategory(THREAD_STATE);
        storage.registerCategory(THREAD_CONTENTION_SAMPLE);

        storage.registerCategory(DEADLOCK_INFO);
    }

    @Override
    public List<ThreadHeader> getThreads(VmRef ref) {
        
        List<ThreadHeader> result = null;
        
        StatementDescriptor<ThreadHeader> desc =
                new StatementDescriptor<>(THREAD_HEADER, QUERY_ALL_THREAD_HEADERS);

        PreparedStatement<ThreadHeader> stmt;
        try {
            
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getHostRef().getAgentId());
            stmt.setString(1, ref.getVmId());

            result = getAllResults(stmt);
            
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        }
        
        return result;
    }
    
    @Override
    public ThreadHeader getThread(ThreadHeader thread) {

        ThreadHeader result = null;
        
        StatementDescriptor<ThreadHeader> desc =
                new StatementDescriptor<>(THREAD_HEADER, QUERY_THREAD_HEADER);

        PreparedStatement<ThreadHeader> prepared;

        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, thread.getAgentId());
            prepared.setString(1, thread.getVmId());
            prepared.setString(2, thread.getThreadName());
            prepared.setLong(3, thread.getThreadId());

            result = getFirstResult(prepared);

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        }

        return result;
    }
    
    @Override
    public void saveThread(ThreadHeader thread) {
        StatementDescriptor<ThreadHeader> desc =
                new StatementDescriptor<>(THREAD_HEADER, ADD_THREAD_HEADER);

        PreparedStatement<ThreadHeader> prepared;

        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, thread.getAgentId());
            prepared.setString(1, thread.getVmId());
            prepared.setString(2, thread.getThreadName());
            prepared.setLong(3, thread.getThreadId());
            prepared.setLong(4, thread.getTimeStamp());
            prepared.setString(5, thread.getReferenceID());

            prepared.execute();

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public ThreadState getLastThreadState(ThreadHeader header) {

        ThreadState result = null;
        StatementDescriptor<ThreadState> desc =
                new StatementDescriptor<>(THREAD_STATE,
                                          QUERY_LATEST_THREAD_STATE_FOR_THREAD);

        PreparedStatement<ThreadState> prepared;
        try {

            prepared = storage.prepareStatement(desc);
            String refId = header.getReferenceID();
            if (refId == null) {
                throw new IllegalArgumentException("header.getReferenceID() can't be null");
            }

            prepared.setString(0, header.getAgentId());
            prepared.setString(1, header.getReferenceID());

            result = getFirstResult(prepared);
            if (result != null) {
                result.setHeader(header);
            }

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        }

        return result;
    }

    public ThreadState getFirstThreadState(ThreadHeader header) {

        ThreadState result = null;
        StatementDescriptor<ThreadState> desc =
                new StatementDescriptor<>(THREAD_STATE,
                                          QUERY_FIRST_THREAD_STATE_FOR_THREAD);

        PreparedStatement<ThreadState> prepared;
        try {

            prepared = storage.prepareStatement(desc);
            String refId = header.getReferenceID();
            if (refId == null) {
                throw new IllegalArgumentException("header.getReferenceID() can't be null");
            }

            prepared.setString(0, header.getAgentId());
            prepared.setString(1, header.getReferenceID());

            result = getFirstResult(prepared);
            if (result != null) {
                result.setHeader(header);
            }

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        }

        return result;
    }

    @Override
    public void addThreadState(ThreadState thread) {
        StatementDescriptor<ThreadState> desc =
                new StatementDescriptor<>(THREAD_STATE, ADD_THREAD_STATE);

        PreparedStatement<ThreadState> prepared;
        try {

            prepared = storage.prepareStatement(desc);

            ThreadHeader header = thread.getHeader();

            prepared.setString(0, header.getAgentId());
            prepared.setString(1, header.getVmId());

            prepared.setString(2, thread.getState());

            prepared.setLong(3, thread.getProbeStartTime());
            prepared.setLong(4, thread.getProbeEndTime());

            prepared.setString(5, header.getReferenceID());

            prepared.execute();

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public void updateThreadState(ThreadState thread) {

        StatementDescriptor<ThreadState> desc =
                new StatementDescriptor<>(THREAD_STATE, DESC_UPDATE_THREAD_STATE);

        PreparedStatement<ThreadState> prepared;
        try {

            prepared = storage.prepareStatement(desc);

            ThreadHeader header = thread.getHeader();

            prepared.setLong(0, thread.getProbeEndTime());
            prepared.setString(1, header.getReferenceID());
            prepared.setLong(2, thread.getProbeStartTime());

            prepared.execute();

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public List<ThreadState> getThreadStates(ThreadHeader thread, Range<Long> range) {

        List<ThreadState> result = new ArrayList<>();

        StatementDescriptor<ThreadState> desc =
                new StatementDescriptor<>(THREAD_STATE, QUERY_THREAD_STATE_PER_THREAD);

        PreparedStatement<ThreadState> prepared;
        try {

            prepared = storage.prepareStatement(desc);

            prepared.setString(0, thread.getReferenceID());
            prepared.setLong(1, range.getMin());
            prepared.setLong(2, range.getMax());

            Cursor<ThreadState> cursor = prepared.executeQuery();
            while (cursor.hasNext()) {
                ThreadState state = cursor.next();
                state.setHeader(thread);
                result.add(state);
            }

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
        }
        return result;
    }

    @Override
    public Range<Long> getThreadStateTotalTimeRange(VmRef ref) {

        PreparedStatement<ThreadState> stmt;

        stmt = prepareQuery(THREAD_STATE, QUERY_OLDEST_THREAD_STATE, ref);
        ThreadState oldestData = getFirstResult(stmt);
        if (oldestData == null) {
            return null;
        }

        long oldestTimeStamp = oldestData.getRange().getMin();

        stmt = prepareQuery(THREAD_STATE, QUERY_LATEST_THREAD_STATE, ref);
        ThreadState latestData = getFirstResult(stmt);
        long latestTimeStamp = latestData.getRange().getMax();

        return new Range<Long>(oldestTimeStamp, latestTimeStamp);
    }

    @Override
    public void saveSummary(ThreadSummary summary) {

        try {
            SUMMARY.statementAdd(summary, storage);

        } catch (Exception ignore) { ignore.printStackTrace(); }
    }

    @Override
    public List<ThreadSummary> getSummary(VmRef ref, SessionID session, Range<Long> range, int limit) {

        List<ThreadSummary> result = new ArrayList<>();
        try {
            Cursor<ThreadSummary> cursor = SUMMARY.queryGet(ref, session, range, limit, storage);
            while (cursor.hasNext()) {
                ThreadSummary summary = cursor.next();
                result.add(summary);
            }
        } catch (Exception ignore) { ignore.printStackTrace(); }

        return result;
    }

    @Override
    public List<SessionID> getAvailableThreadSummarySessions(VmRef ref, Range<Long> range, int limit) {
        List<SessionID> result = new ArrayList<>();

        Cursor<ThreadSummary> cursor = null;

        try {
            cursor = SUMMARY.queryGet(ref, range, limit, storage);
            while (cursor.hasNext()) {
                ThreadSummary summary = cursor.next();
                result.add(new SessionID(summary.getSession()));
            }
        } catch (Exception ignore) { ignore.printStackTrace(); }

        return result;
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
    public void saveContentionSample(ThreadContentionSample contentionSample) {

        StatementDescriptor<ThreadContentionSample> desc =
                new StatementDescriptor<>(THREAD_CONTENTION_SAMPLE,
                                          ADD_CONTENTION_SAMPLE);
        PreparedStatement<ThreadContentionSample> prepared;
        ThreadHeader header = contentionSample.getHeader();
        if (header == null || header.getReferenceID() == null) {
            throw new IllegalArgumentException("header or header.getReferenceID() can't be null");
        }

        try {
            prepared = storage.prepareStatement(desc);

            prepared.setString(0, header.getAgentId());
            prepared.setString(1, header.getVmId());

            prepared.setLong(2, contentionSample.getBlockedCount());
            prepared.setLong(3, contentionSample.getBlockedTime());
            prepared.setLong(4, contentionSample.getWaitedCount());
            prepared.setLong(5, contentionSample.getWaitedTime());

            prepared.setString(6, header.getReferenceID());

            prepared.setLong(7, contentionSample.getTimeStamp());

            prepared.execute();

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public ThreadContentionSample getLatestContentionSample(ThreadHeader thread) {

        ThreadContentionSample sample = null;

        StatementDescriptor<ThreadContentionSample> desc =
                new StatementDescriptor<>(THREAD_CONTENTION_SAMPLE,
                                          GET_LATEST_CONTENTION_SAMPLE);
        PreparedStatement<ThreadContentionSample> prepared;

        if (thread == null || thread.getReferenceID() == null) {
            throw new IllegalArgumentException("header or header.getReferenceID() can't be null");
        }

        try {
            prepared = storage.prepareStatement(desc);

            prepared.setString(0, thread.getReferenceID());
            Cursor<ThreadContentionSample> cursor = prepared.executeQuery();
            if (cursor.hasNext()) {
                sample = cursor.next();
                sample.setHeader(thread);
            }

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }

        return sample;
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

    private <T extends Pojo> List<T> getAllResults(PreparedStatement<T> stmt) {
        Cursor<T> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        while (cursor.hasNext()) {
            T info = cursor.next();
            result.add(info);
        }

        return result;
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

        T result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }

        return result;
    }
}

