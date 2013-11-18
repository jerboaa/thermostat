/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.thread.dao;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public interface ThreadDao {

    /*
     * User-facing string-constants used to represent VM thread capabilities.
     */
    static final String CPU_TIME = "thread-cpu-time";
    static final String CONTENTION_MONITOR = "thread-contention-monitor";
    static final String THREAD_ALLOCATED_MEMORY = "thread-allocated-memory";
    
    /*
     * vm-thread-capabilities schema
     */
    static final Key<List<String>> SUPPORTED_FEATURES_LIST_KEY = new Key<List<String>>("supportedFeaturesList");
    static final Category<VMThreadCapabilities> THREAD_CAPABILITIES =
            new Category<>("vm-thread-capabilities", VMThreadCapabilities.class, Key.AGENT_ID, Key.VM_ID,
                         SUPPORTED_FEATURES_LIST_KEY);

    /*
     * vm-thread-summary schema
     */
    static final Key<Long> LIVE_THREADS_KEY = new Key<Long>("currentLiveThreads");
    static final Key<Long> DAEMON_THREADS_KEY = new Key<Long>("currentDaemonThreads");
    static final Category<ThreadSummary> THREAD_SUMMARY =
            new Category<>("vm-thread-summary", ThreadSummary.class, Key.AGENT_ID, Key.VM_ID,
                         Key.TIMESTAMP,
                         LIVE_THREADS_KEY, DAEMON_THREADS_KEY);
    
    /*
     * vm-thread-harvesting schema
     */
    static final Key<Boolean> HARVESTING_STATUS_KEY = new Key<Boolean>("harvesting");
    static final Category<ThreadHarvestingStatus> THREAD_HARVESTING_STATUS =
            new Category<>("vm-thread-harvesting", ThreadHarvestingStatus.class,
                    Key.AGENT_ID,
                    Key.VM_ID,
                    Key.TIMESTAMP,
                    HARVESTING_STATUS_KEY);

    /*
     * vm-thread-info schema
     */
    static final Key<String> THREAD_STATE_KEY = new Key<String>("threadState");
    static final Key<Long> THREAD_ID_KEY = new Key<Long>("threadId");
    static final Key<Long> THREAD_ALLOCATED_BYTES_KEY = new Key<Long>("allocatedBytes");
    static final Key<String> THREAD_NAME_KEY = new Key<String>("threadName");
    static final Key<Long> THREAD_CPU_TIME_KEY = new Key<Long>("threadCpuTime");
    static final Key<Long> THREAD_USER_TIME_KEY = new Key<Long>("threadUserTime");
    static final Key<Long> THREAD_BLOCKED_COUNT_KEY = new Key<Long>("threadBlockedCount");
    static final Key<Long> THREAD_WAIT_COUNT_KEY = new Key<Long>("threadWaitCount");
    static final Category<ThreadInfoData> THREAD_INFO =
            new Category<>("vm-thread-info", ThreadInfoData.class,
                         Arrays.<Key<?>>asList(
                                 Key.AGENT_ID,
                                 Key.VM_ID,
                                 Key.TIMESTAMP,
                                 THREAD_NAME_KEY,
                                 THREAD_ID_KEY,
                                 THREAD_STATE_KEY,
                                 THREAD_CPU_TIME_KEY,
                                 THREAD_ALLOCATED_BYTES_KEY,
                                 THREAD_USER_TIME_KEY,
                                 THREAD_BLOCKED_COUNT_KEY,
                                 THREAD_WAIT_COUNT_KEY),
                         Arrays.<Key<?>>asList(Key.TIMESTAMP));
    
    /*
     * vm-deadlock-data schema
     */
    static final Key<String> DEADLOCK_DESCRIPTION_KEY = new Key<>("deadLockDescription");
    static final Category<VmDeadLockData> DEADLOCK_INFO = new Category<>("vm-deadlock-data", VmDeadLockData.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            DEADLOCK_DESCRIPTION_KEY);

    /*
     * API methods
     */
    
    void saveSummary(ThreadSummary summary);
    ThreadSummary loadLastestSummary(VmRef ref);
    List<ThreadSummary> loadSummary(VmRef ref, long since);

    /** Save the specified thread info */
    void saveThreadInfo(ThreadInfoData info);

    /**
     * Get the time interval for the entire data
     *
     * @returns a {@link Range} of timestamps (in milliseconds) or null if there
     * is no valid data.
     */
    Range<Long> getThreadInfoTimeRange(VmRef ref);

    /** Get the thread info data with a timestamp greated than the one specified */
    List<ThreadInfoData> loadThreadInfo(VmRef ref, long since);
    /** Get the thread info data with a timestamp in the given time range (inclusive) */
    List<ThreadInfoData> loadThreadInfo(VmRef ref, Range<Long> time);

    ThreadHarvestingStatus getLatestHarvestingStatus(VmRef vm);
    void saveHarvestingStatus(ThreadHarvestingStatus status);

    VMThreadCapabilities loadCapabilities(VmRef ref);
    void saveCapabilities(VMThreadCapabilities caps);

    void saveDeadLockStatus(VmDeadLockData deadLockInfo);

    /**
     * Returns the latest vm deadlock data
     *
     * @return the latest data or null if there is none
     */
    VmDeadLockData loadLatestDeadLockStatus(VmRef ref);
    
}

