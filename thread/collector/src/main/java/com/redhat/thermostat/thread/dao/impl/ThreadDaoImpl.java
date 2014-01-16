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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadDaoImpl implements ThreadDao {
    
    private static final Logger logger = LoggingUtils.getLogger(ThreadDaoImpl.class);
    
    // Queries
    
    static final String QUERY_THREAD_CAPS = "QUERY "
            + THREAD_CAPABILITIES.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s LIMIT 1";
    static final String QUERY_LATEST_SUMMARY = "QUERY "
            + THREAD_SUMMARY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    static final String QUERY_SUMMARY_SINCE = "QUERY "
            + THREAD_SUMMARY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";
    static final String QUERY_LATEST_HARVESTING_STATUS = "QUERY "
            + THREAD_HARVESTING_STATUS.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    static final String QUERY_THREAD_INFO_SINCE = "QUERY "
            + THREAD_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";
    static final String QUERY_THREAD_INFO_INTERVAL = "QUERY "
            + THREAD_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l AND '"
            + Key.TIMESTAMP.getName() + "' < ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";
    static final String QUERY_OLDEST_THREAD_INFO = "QUERY "
            + THREAD_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' ASC LIMIT 1";
    static final String QUERY_LATEST_THREAD_INFO = "QUERY "
            + THREAD_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s SORT '"
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    static final String QUERY_LATEST_DEADLOCK_INFO = "QUERY "
            + DEADLOCK_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    
    // Data modifying descriptors
    
    // ADD vm-thread-summary SET 'agentId' = ?s , \
    //                           'vmId' = ?s , \
    //                           'currentLiveThreads' = ?l , \
    //                           'currentDaemonThreads' = ?l , \
    //                           'timeStamp' = ?l
    static final String DESC_ADD_THREAD_SUMMARY = "ADD " + THREAD_SUMMARY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + LIVE_THREADS_KEY.getName() + "' = ?l , " +
                 "'" + DAEMON_THREADS_KEY.getName() + "' = ?l , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l";
    // ADD vm-thread-harvesting SET 'agentId' = ?s , \
    //                              'vmId' = ?s , \
    //                              'timeStamp' = ?l , \
    //                              'harvesting' = ?b
    static final String DESC_ADD_THREAD_HARVESTING_STATUS = "ADD " + THREAD_HARVESTING_STATUS.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + HARVESTING_STATUS_KEY.getName() + "' = ?b";
    // ADD vm-thread-info SET 'agentId' = ?s , \
    //                        'vmId' = ?s , \
    //                        'threadName' = ?s , \
    //                        'threadId' = ?l , \
    //                        'threadState' = ?s , \
    //                        'allocatedBytes' = ?l , \
    //                        'timeStamp' = ?l , \
    //                        'threadCpuTime' = ?l , \
    //                        'threadUserTime' = ?l , \
    //                        'threadBlockedCount' = ?l , \
    //                        'threadWaitCount' = ?l
    static final String DESC_ADD_THREAD_INFO = "ADD " + THREAD_INFO.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + THREAD_NAME_KEY.getName() + "' = ?s , " +
                 "'" + THREAD_ID_KEY.getName() + "' = ?l , " +
                 "'" + THREAD_STATE_KEY.getName() + "' = ?s , " +
                 "'" + THREAD_ALLOCATED_BYTES_KEY.getName() + "' = ?l , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + THREAD_CPU_TIME_KEY.getName() + "' = ?l , " +
                 "'" + THREAD_USER_TIME_KEY.getName() + "' = ?l , " +
                 "'" + THREAD_BLOCKED_COUNT_KEY.getName() + "' = ?l , " +
                 "'" + THREAD_WAIT_COUNT_KEY.getName() + "' = ?l";
    // ADD vm-deadlock-data SET 'agentId' = ?s , \
    //                          'vmId' = ?s , \
    //                          'timeStamp' = ?l , \
    //                          'deadLockDescription' = ?s
    static final String DESC_ADD_THREAD_DEADLOCK_DATA = "ADD " + DEADLOCK_INFO.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + DEADLOCK_DESCRIPTION_KEY.getName() + "' = ?s";
    // REPLACE vm-thread-capabilities SET 'agentId' = ?s , \
    //                                    'vmId' = ?s , \
    //                                    'supportedFeaturesList' = ?s[
    //                                WHERE 'agentId' = ?s AND 'vmId' = ?s
    static final String DESC_REPLACE_THREAD_CAPS = "REPLACE " + THREAD_CAPABILITIES.getName() + 
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + SUPPORTED_FEATURES_LIST_KEY.getName() + "' = ?s[" +
            " WHERE '" + Key.AGENT_ID.getName() + "' = ?s AND " +
                   "'" + Key.VM_ID.getName() + "' = ?s";
    
    private Storage storage;
    
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(THREAD_CAPABILITIES);
        storage.registerCategory(THREAD_SUMMARY);
        storage.registerCategory(THREAD_HARVESTING_STATUS);
        storage.registerCategory(THREAD_INFO);
        storage.registerCategory(DEADLOCK_INFO);
    }

    @Override
    public VMThreadCapabilities loadCapabilities(VmRef vm) {
        PreparedStatement<VMThreadCapabilities> stmt = prepareQuery(THREAD_CAPABILITIES, QUERY_THREAD_CAPS, vm);
        if (stmt == null) {
            return null;
        }
        
        return getFirstResult(stmt);
    }
    
    @Override
    public void saveCapabilities(VMThreadCapabilities caps) {
        StatementDescriptor<VMThreadCapabilities> desc = new StatementDescriptor<>(THREAD_CAPABILITIES, DESC_REPLACE_THREAD_CAPS);
        PreparedStatement<VMThreadCapabilities> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, caps.getAgentId());
            prepared.setString(1, caps.getVmId());
            prepared.setStringList(2, caps.getSupportedFeaturesList());
            prepared.setString(3, caps.getAgentId());
            prepared.setString(4, caps.getVmId());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }
    
    @Override
    public void saveSummary(ThreadSummary summary) {
        StatementDescriptor<ThreadSummary> desc = new StatementDescriptor<>(THREAD_SUMMARY, DESC_ADD_THREAD_SUMMARY);
        PreparedStatement<ThreadSummary> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, summary.getAgentId());
            prepared.setString(1, summary.getVmId());
            prepared.setLong(2, summary.getCurrentLiveThreads());
            prepared.setLong(3, summary.getCurrentDaemonThreads());
            prepared.setLong(4, summary.getTimeStamp());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }
    
    @Override
    public ThreadSummary loadLastestSummary(VmRef ref) {
        PreparedStatement<ThreadSummary> stmt = prepareQuery(THREAD_SUMMARY, QUERY_LATEST_SUMMARY, ref);
        if (stmt == null) {
            return null;
        }
        
        return getFirstResult(stmt);
    }
    
    @Override
    public List<ThreadSummary> loadSummary(VmRef ref, long since) {
        PreparedStatement<ThreadSummary> stmt = prepareQuery(THREAD_SUMMARY, QUERY_SUMMARY_SINCE, ref, since, null);
        if (stmt == null) {
            return Collections.emptyList();
        }

        return getAllResults(stmt);
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
    public void saveThreadInfo(ThreadInfoData info) {
        StatementDescriptor<ThreadInfoData> desc = new StatementDescriptor<>(THREAD_INFO, DESC_ADD_THREAD_INFO);
        PreparedStatement<ThreadInfoData> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, info.getAgentId());
            prepared.setString(1, info.getVmId());
            prepared.setString(2, info.getThreadName());
            prepared.setLong(3, info.getThreadId());
            prepared.setString(4, info.getThreadState());
            prepared.setLong(5, info.getAllocatedBytes());
            prepared.setLong(6, info.getTimeStamp());
            prepared.setLong(7, info.getThreadCpuTime());
            prepared.setLong(8, info.getThreadUserTime());
            prepared.setLong(9, info.getThreadBlockedCount());
            prepared.setLong(10, info.getThreadWaitCount());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public Range<Long> getThreadInfoTimeRange(VmRef ref) {
        PreparedStatement<ThreadInfoData> stmt;

        stmt = prepareQuery(THREAD_INFO, QUERY_OLDEST_THREAD_INFO, ref);
        ThreadInfoData oldestData = getFirstResult(stmt);
        if (oldestData == null) {
            return null;
        }

        long oldestTimeStamp = oldestData.getTimeStamp();

        stmt = prepareQuery(THREAD_INFO, QUERY_LATEST_THREAD_INFO, ref);
        ThreadInfoData latestData = getFirstResult(stmt);
        long latestTimeStamp = latestData.getTimeStamp();

        return new Range<Long>(oldestTimeStamp, latestTimeStamp);
    }

    @Override
    public List<ThreadInfoData> loadThreadInfo(VmRef ref, long since) {
        PreparedStatement<ThreadInfoData> stmt = prepareQuery(THREAD_INFO, QUERY_THREAD_INFO_SINCE, ref, since, null);
        if (stmt == null) {
            return Collections.emptyList();
        }

        return getAllResults(stmt);
    }

    @Override
    public List<ThreadInfoData> loadThreadInfo(VmRef ref, Range<Long> time) {
        PreparedStatement<ThreadInfoData> stmt = prepareQuery(THREAD_INFO, QUERY_THREAD_INFO_INTERVAL, ref, time.getMin(), time.getMax());
        if (stmt == null) {
            return Collections.emptyList();
        }

        return getAllResults(stmt);
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

