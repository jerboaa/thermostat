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

package com.redhat.thermostat.thread.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class ThreadDaoImpl implements ThreadDao {

    private Storage storage; 
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;
        storage.createConnectionKey(THREAD_CAPABILITIES);
        storage.createConnectionKey(THREAD_SUMMARY);
        storage.createConnectionKey(THREAD_INFO);
    }

    @Override
    public VMThreadCapabilities loadCapabilities(VmRef vm) {
        try {
        Query query = storage.createQuery()
                .from(THREAD_CAPABILITIES)
                .where(Key.VM_ID, Query.Criteria.EQUALS, vm.getId())
                .where(Key.AGENT_ID, Query.Criteria.EQUALS, vm.getAgent().getAgentId());
        
        VMThreadCapabilities caps = storage.findPojo(query, VMThreadCapabilities.class);
        return caps;
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
            return null;
        }
    }
    
    @Override
    public void saveCapabilities(VMThreadCapabilities caps) {
        storage.putPojo(THREAD_CAPABILITIES, true, caps);
    }
    
    @Override
    public void saveSummary(ThreadSummary summary) {
        storage.putPojo(THREAD_SUMMARY, false, summary);
    }
    
    @Override
    public ThreadSummary loadLastestSummary(VmRef ref) {
        ThreadSummary summary = null;

        Query query = prepareQuery(THREAD_SUMMARY, ref);
        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING).limit(1);
        if (cursor.hasNext()) {
            Chunk found = cursor.next();
            summary = new ThreadSummary();
            summary.setTimeStamp(found.get(Key.TIMESTAMP));
            summary.setCurrentLiveThreads(found.get(LIVE_THREADS_KEY));
            summary.setCurrentDaemonThreads(found.get(DAEMON_THREADS_KEY));
        }
        
        return summary;
    }
    
    @Override
    public List<ThreadSummary> loadSummary(VmRef ref, long since) {
        
        List<ThreadSummary> result = new ArrayList<>();
        
        Query query = prepareQuery(THREAD_SUMMARY, ref);
        query.where(Key.TIMESTAMP, Criteria.GREATER_THAN, since);

        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING);
        while (cursor.hasNext()) {
            ThreadSummary summary = new ThreadSummary();
            
            Chunk found = cursor.next();
            summary.setTimeStamp(found.get(Key.TIMESTAMP));
            summary.setCurrentLiveThreads(found.get(LIVE_THREADS_KEY));
            summary.setCurrentDaemonThreads(found.get(DAEMON_THREADS_KEY));
            result.add(summary);
        }
        
        return result;
    }
    
    @Override
    public void saveThreadInfo(ThreadInfoData info) {
        storage.putPojo(THREAD_INFO, false, info);
    }

    @Override
    public List<ThreadInfoData> loadThreadInfo(VmRef ref, long since) {
        List<ThreadInfoData> result = new ArrayList<>();
        
        Query query = prepareQuery(THREAD_INFO, ref)
                .where(Key.TIMESTAMP, Criteria.GREATER_THAN, since);
        
        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING);
        while (cursor.hasNext()) {
            ThreadInfoData info = new ThreadInfoData();
            
            Chunk found = cursor.next();
            info.setTimeStamp(found.get(Key.TIMESTAMP));
            
            info.setThreadId(found.get(THREAD_ID_KEY));
            info.setThreadName(found.get(THREAD_NAME_KEY));
            info.setThreadState(Thread.State.valueOf(found.get(THREAD_STATE_KEY)));

            info.setThreadBlockedCount(found.get(THREAD_BLOCKED_COUNT_KEY));
            info.setThreadWaitCount(found.get(THREAD_WAIT_COUNT_KEY));
            info.setThreadCpuTime(found.get(THREAD_CPU_TIME_KEY));
            info.setThreadUserTime(found.get(THREAD_USER_TIME_KEY));

            result.add(info);
        }
        
        return result;
    }
    
    private Query prepareQuery(Category category, VmRef vm) {
        return prepareQuery(category, vm.getIdString(), vm.getAgent().getAgentId());
    }

    private Query prepareQuery(Category category, String vmId, String agentId) {
        Query query = storage.createQuery()
                .from(category)
                .where(Key.AGENT_ID, Query.Criteria.EQUALS, agentId)
                .where(Key.VM_ID, Query.Criteria.EQUALS, Integer.valueOf(vmId));
        return query;
    }
    
    @Override
    public Storage getStorage() {
        return storage;
    }
}
