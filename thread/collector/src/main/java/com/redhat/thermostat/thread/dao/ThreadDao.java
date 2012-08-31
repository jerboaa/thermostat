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
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public interface ThreadDao {

    static final String CPU_TIME = "thread-cpu-time";
    static final String CONTENTION_MONITOR = "thread-contention-monitor";
    static final String THREAD_ALLOCATED_MEMORY = "thread-allocated-memory";

    static final Key<Boolean> CPU_TIME_KEY = new Key<Boolean>(CPU_TIME, false);
    static final Key<Boolean> CONTENTION_MONITOR_KEY = new Key<Boolean>(CONTENTION_MONITOR, false);
    static final Key<Boolean> THREAD_ALLOCATED_MEMORY_KEY = new Key<Boolean>(THREAD_ALLOCATED_MEMORY, false);

    static final Category THREAD_CAPABILITIES =
            new Category("vm-thread-capabilities", Key.AGENT_ID, Key.VM_ID,
                         CPU_TIME_KEY, CONTENTION_MONITOR_KEY,
                         THREAD_ALLOCATED_MEMORY_KEY);


    VMThreadCapabilities loadCapabilities(VmRef ref);
    void saveCapabilities(String vmId, String agentId, VMThreadCapabilities caps);

    static final String LIVE_THREADS = "thread-living";
    static final Key<Long> LIVE_THREADS_KEY = new Key<Long>(LIVE_THREADS, false);
    static final String DAEMON_THREADS = "thread-daemons";
    static final Key<Long> DAEMON_THREADS_KEY = new Key<Long>(DAEMON_THREADS, false);
    
    static final Category THREAD_SUMMARY =
            new Category("vm-thread-summary", Key.AGENT_ID, Key.VM_ID,
                         Key.TIMESTAMP,
                         LIVE_THREADS_KEY, DAEMON_THREADS_KEY);
    
    void saveSummary(String vmId, String agentId, ThreadSummary summary);
    ThreadSummary loadLastestSummary(VmRef ref);
    List<ThreadSummary> loadSummary(VmRef ref, long since);

    static final String THREAD_STATE = "thread-state";
    static final Key<String> THREAD_STATE_KEY = new Key<String>(THREAD_STATE, false);
    static final String THREAD_ID = "thread-id";
    static final Key<Long> THREAD_ID_KEY = new Key<Long>(THREAD_ID, false);
    static final String THREAD_NAME = "thread-name";
    static final Key<String> THREAD_NAME_KEY = new Key<String>(THREAD_NAME, false);
    static final String THREAD_HEAP = "thread-id";
    static final Key<Long> THREAD_HEAP_KEY = new Key<Long>(THREAD_HEAP, false);
    static final String THREAD_CPU_TIME = "thread-cpu-time";
    static final Key<Long> THREAD_CPU_TIME_KEY = new Key<Long>(THREAD_CPU_TIME, false);
    static final String THREAD_USER_TIME = "thread-user-time";
    static final Key<Long> THREAD_USER_TIME_KEY = new Key<Long>(THREAD_USER_TIME, false);
    static final String THREAD_BLOCKED_COUNT = "thread-blocked-count";
    static final Key<Long> THREAD_BLOCKED_COUNT_KEY = new Key<Long>(THREAD_BLOCKED_COUNT, false);
    static final String THREAD_WAIT_COUNT = "thread-wait-count";
    static final Key<Long> THREAD_WAIT_COUNT_KEY = new Key<Long>(THREAD_WAIT_COUNT, false);
    static final String THREAD_STACK_TRACE_ID = "thread-stacktrace-id";
    static final Key<Long> THREAD_STACK_TRACE_ID_KEY = new Key<Long>(THREAD_STACK_TRACE_ID, false);
            
    static final Category THREAD_INFO =
            new Category("vm-thread-info", Key.AGENT_ID, Key.VM_ID,
                         Key.TIMESTAMP, THREAD_NAME_KEY, THREAD_ID_KEY,
                         THREAD_STATE_KEY, THREAD_HEAP_KEY, THREAD_CPU_TIME_KEY,
                         THREAD_USER_TIME_KEY, THREAD_BLOCKED_COUNT_KEY,
                         THREAD_WAIT_COUNT_KEY, THREAD_STACK_TRACE_ID_KEY);
    
    void saveThreadInfo(String vmId, String agentId, ThreadInfoData info);
    List<ThreadInfoData> loadThreadInfo(VmRef ref, long since);
    
    Storage getStorage();
}
