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

package com.redhat.thermostat.thread.dao;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadContentionSample;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import java.util.Arrays;
import java.util.List;

public interface ThreadDao {

    /*
     * User-facing string-constants used to represent VM thread capabilities.
     */
    static final String CPU_TIME = "thread-cpu-time";
    static final String CONTENTION_MONITOR = "thread-contention-monitor";
    static final String THREAD_ALLOCATED_MEMORY = "thread-allocated-memory";

    /*
     * vm-thread-harvesting schema
     */
    static final Key<Boolean> HARVESTING_STATUS_KEY = new Key<Boolean>("harvesting");
    static final Category<ThreadHarvestingStatus> THREAD_HARVESTING_STATUS =
            new Category<>("vm-thread-harvesting", ThreadHarvestingStatus.class,
                    Arrays.<Key<?>>asList(
                            Key.AGENT_ID,
                            Key.VM_ID,
                            Key.TIMESTAMP,
                            HARVESTING_STATUS_KEY),
                    Arrays.<Key<?>>asList(Key.TIMESTAMP));

    /*
     * vm-thread-header schema
     */
    static final Key<Long> THREAD_ID_KEY = new Key<Long>("threadId");
    static final Key<String> THREAD_NAME_KEY = new Key<String>("threadName");
    static final Key<String> THREAD_HEADER_UUID = new Key<String>("referenceID");
    static final Category<ThreadHeader> THREAD_HEADER =
        new Category<>("vm-thread-header", ThreadHeader.class,
                       Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID,
                                             THREAD_NAME_KEY, THREAD_HEADER_UUID,
                                             THREAD_ID_KEY, Key.TIMESTAMP),
                       Arrays.<Key<?>>asList(Key.TIMESTAMP, THREAD_NAME_KEY,
                                             THREAD_ID_KEY, THREAD_HEADER_UUID));
                            
    /*
     * vm-thread-state schema
     */
    static final Key<String> THREAD_STATE_KEY = new Key<String>("state");
    static final Key<Long> THREAD_PROBE_START = new Key<Long>("probeStartTime");
    static final Key<Long> THREAD_PROBE_END = new Key<Long>("probeEndTime");
    static final Category<ThreadState> THREAD_STATE =
            new Category<>("vm-thread-state", ThreadState.class,
                           Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID, THREAD_STATE_KEY,
                                            THREAD_PROBE_START, THREAD_PROBE_END,
                                            THREAD_HEADER_UUID),
                           Arrays.<Key<?>>asList(THREAD_PROBE_START, THREAD_PROBE_END,
                                            THREAD_HEADER_UUID));

    /*
     * vm-deadlock-data schema
     */
    static final Key<String> DEADLOCK_DESCRIPTION_KEY = new Key<>("deadLockDescription");
    static final Category<VmDeadLockData> DEADLOCK_INFO = new Category<>("vm-deadlock-data", VmDeadLockData.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            DEADLOCK_DESCRIPTION_KEY);


    /*
     * THREAD_CONTENTION_SAMPLE
     */
    static final Key<String> THREAD_CONTENTION_BLOCKED_COUNT_KEY = new Key<>("blockedCount");
    static final Key<String> THREAD_CONTENTION_BLOCKED_TIME_KEY = new Key<>("blockedTime");
    static final Key<String> THREAD_CONTENTION_WAITED_COUNT_KEY = new Key<>("waitedCount");
    static final Key<String> THREAD_CONTENTION_WAITED_TIME_KEY = new Key<>("waitedTime");
    static final Category<ThreadContentionSample> THREAD_CONTENTION_SAMPLE =
            new Category<>("thread-contention-sample", ThreadContentionSample.class,
                           Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID,
                                                 THREAD_CONTENTION_BLOCKED_COUNT_KEY,
                                                 THREAD_CONTENTION_BLOCKED_TIME_KEY,
                                                 THREAD_CONTENTION_WAITED_COUNT_KEY,
                                                 THREAD_CONTENTION_WAITED_TIME_KEY,
                                                 THREAD_HEADER_UUID, Key.TIMESTAMP),
                           Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID,
                                                 THREAD_CONTENTION_BLOCKED_COUNT_KEY,
                                                 THREAD_CONTENTION_BLOCKED_TIME_KEY,
                                                 THREAD_CONTENTION_WAITED_COUNT_KEY,
                                                 THREAD_CONTENTION_WAITED_TIME_KEY,
                                                 THREAD_HEADER_UUID, Key.TIMESTAMP));

    void saveSummary(ThreadSummary summary);
    List<ThreadSummary> getSummary(VmRef ref, SessionID session, Range<Long> range, int limit);

    /**
     * Returns a list of sessions registered by thread sampling.
     */
    List<ThreadSession> getSessions(VmRef ref, Range<Long> range, int limit);

    /**
     * Save the given session to the database.
     */
    void saveSession(ThreadSession session);

    /**
     * Gets the total time interval for the entire data related to
     * {@link ThreadState}, as a {@link Range}.
     * 
     * <br /><br />
     * 
     * This {@link Range#getMin()} will return the start probe time for the
     * oldest {@link ThreadState} entry while {@link Range#getMax()} will
     * contains the end probe time for the most recent {@link ThreadState}
     * entry.
     *
     * <br /><br />
     *
     * If no data is contained related to this {@link VmRef}, {@code null}
     * will be returned.
     *
     * @returns a {@link Range} of timestamps (in milliseconds) or null if there
     * is no valid data.
     */
    Range<Long> getThreadStateTotalTimeRange(VmRef ref);

    ThreadHarvestingStatus getLatestHarvestingStatus(VmRef vm);
    void saveHarvestingStatus(ThreadHarvestingStatus status);

    void saveDeadLockStatus(VmDeadLockData deadLockInfo);

    /**
     * Returns the latest vm deadlock data
     *
     * @return the latest data or null if there is none
     */
    VmDeadLockData loadLatestDeadLockStatus(VmRef ref);
    
    /**
     * Returns the list of known {@link ThreadHeader}s for the given VM.
     * The list is sorted by timestamp.
     */
    List<ThreadHeader> getThreads(VmRef ref);

    /**
     * Returns the actual {@link ThreadHeader} in the storage based on th
     * input {@link ThreadHeader} template, {@code null} otherwise.
     */
    ThreadHeader getThread(ThreadHeader thread);
    
    /**
     * Adds the {@link ThreadHeader} into the target {@link VmRef} storage.
     * If the target vm already has such entry the
     */
    void saveThread(ThreadHeader thread);
    
    /**
     * Returns the last known {@link ThreadState} in the database for this
     * {@link ThreadHeader}.
     */
    ThreadState getLastThreadState(ThreadHeader header);

    /**
     * Returns the first known {@link ThreadState} in the database for this
     * {@link ThreadHeader}.
     */
    ThreadState getFirstThreadState(ThreadHeader thread);

    /**
     * Adds the given {@link ThreadState} to storage.
     */
    void addThreadState(ThreadState thread);

    /**
     * Updates the given {@link ThreadState}
     */
    void updateThreadState(ThreadState thread);

    /**
     * Return a list of {@link ThreadState} for the given {@link ThreadHeader}
     * in the given {@link Range}.
     */
    List<ThreadState> getThreadStates(ThreadHeader thread, Range<Long> range);

    /**
     * Adds this {@link ThreadContentionSample} to the database.
     *
     * <br /><br />
     *
     * The sample must be fully created and valid, in particular, its
     * {@link ThreadHeader} must be a valid entry in the database.
     */
    void saveContentionSample(ThreadContentionSample contentionSample);

    /**
     * Returns the latest {@link ThreadContentionSample} available for this
     * {@link ThreadHeader}.
     */
    ThreadContentionSample getLatestContentionSample(ThreadHeader thread);
}
