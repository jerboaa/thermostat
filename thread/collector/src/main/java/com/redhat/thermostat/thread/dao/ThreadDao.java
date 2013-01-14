/*
 * Copyright 2012 Red Hat, Inc.
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

import java.util.List;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public interface ThreadDao {

    static final String CPU_TIME = "thread-cpu-time";
    static final String CONTENTION_MONITOR = "thread-contention-monitor";
    static final String THREAD_ALLOCATED_MEMORY = "thread-allocated-memory";
    static final String SUPPORTED_FEATURES_LIST = "supportedFeaturesList";

    static final Key<Boolean> CPU_TIME_KEY = new Key<Boolean>(CPU_TIME, false);
    static final Key<Boolean> CONTENTION_MONITOR_KEY = new Key<Boolean>(CONTENTION_MONITOR, false);
    static final Key<Boolean> THREAD_ALLOCATED_MEMORY_KEY = new Key<Boolean>(THREAD_ALLOCATED_MEMORY, false);
    static final Key<List<String>> SUPPORTED_FEATURES_LIST_KEY = new Key<List<String>>(SUPPORTED_FEATURES_LIST, false);

    static final Category<VMThreadCapabilities> THREAD_CAPABILITIES =
            new Category<>("vm-thread-capabilities", VMThreadCapabilities.class, Key.AGENT_ID, Key.VM_ID,
                         SUPPORTED_FEATURES_LIST_KEY);


    VMThreadCapabilities loadCapabilities(VmRef ref);
    void saveCapabilities(VMThreadCapabilities caps);

    static final String LIVE_THREADS = "currentLiveThreads";
    static final Key<Long> LIVE_THREADS_KEY = new Key<Long>(LIVE_THREADS, false);
    static final String DAEMON_THREADS = "currentDaemonThreads";
    static final Key<Long> DAEMON_THREADS_KEY = new Key<Long>(DAEMON_THREADS, false);
    
    static final Category<ThreadSummary> THREAD_SUMMARY =
            new Category<>("vm-thread-summary", ThreadSummary.class, Key.AGENT_ID, Key.VM_ID,
                         Key.TIMESTAMP,
                         LIVE_THREADS_KEY, DAEMON_THREADS_KEY);
    
    void saveSummary(ThreadSummary summary);
    ThreadSummary loadLastestSummary(VmRef ref);
    List<ThreadSummary> loadSummary(VmRef ref, long since);

    static final String THREAD_STATE = "threadState";
    static final Key<String> THREAD_STATE_KEY = new Key<String>(THREAD_STATE, false);
    static final String THREAD_ID = "threadId";
    static final Key<Long> THREAD_ID_KEY = new Key<Long>(THREAD_ID, false);
    static final String THREAD_NAME = "threadName";
    static final Key<String> THREAD_NAME_KEY = new Key<String>(THREAD_NAME, false);
    static final String THREAD_CPU_TIME = "threadCpuTime";
    static final Key<Long> THREAD_CPU_TIME_KEY = new Key<Long>(THREAD_CPU_TIME, false);
    static final String THREAD_USER_TIME = "threadUserTime";
    static final Key<Long> THREAD_USER_TIME_KEY = new Key<Long>(THREAD_USER_TIME, false);
    static final String THREAD_BLOCKED_COUNT = "threadBlockedCount";
    static final Key<Long> THREAD_BLOCKED_COUNT_KEY = new Key<Long>(THREAD_BLOCKED_COUNT, false);
    static final String THREAD_WAIT_COUNT = "threadWaitCount";
    static final Key<Long> THREAD_WAIT_COUNT_KEY = new Key<Long>(THREAD_WAIT_COUNT, false);
    
    static final Category<ThreadInfoData> THREAD_INFO =
            new Category<>("vm-thread-info", ThreadInfoData.class, Key.AGENT_ID, Key.VM_ID,
                         Key.TIMESTAMP, THREAD_NAME_KEY, THREAD_ID_KEY,
                         THREAD_STATE_KEY,
                         THREAD_CPU_TIME_KEY,
                         THREAD_USER_TIME_KEY, THREAD_BLOCKED_COUNT_KEY,
                         THREAD_WAIT_COUNT_KEY);
    
    void saveThreadInfo(ThreadInfoData info);
    List<ThreadInfoData> loadThreadInfo(VmRef ref, long since);
    
    Storage getStorage();
}
