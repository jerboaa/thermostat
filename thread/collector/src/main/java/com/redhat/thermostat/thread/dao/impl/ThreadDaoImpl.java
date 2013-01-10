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
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class ThreadDaoImpl implements ThreadDao {

    private Storage storage; 
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(THREAD_CAPABILITIES);
        storage.registerCategory(THREAD_SUMMARY);
        storage.registerCategory(THREAD_INFO);
    }

    @Override
    public VMThreadCapabilities loadCapabilities(VmRef vm) {
        Query<VMThreadCapabilities> query = storage.createQuery(THREAD_CAPABILITIES, VMThreadCapabilities.class);
        query.where(Key.VM_ID, Query.Criteria.EQUALS, vm.getId());
        query.where(Key.AGENT_ID, Query.Criteria.EQUALS, vm.getAgent().getAgentId());
        query.limit(1);
        VMThreadCapabilities caps = query.execute().next();
        return caps;
    }
    
    @Override
    public void saveCapabilities(VMThreadCapabilities caps) {
        Put replace = storage.createReplace(THREAD_CAPABILITIES);
        replace.setPojo(caps);
        replace.apply();
    }
    
    @Override
    public void saveSummary(ThreadSummary summary) {
        Put add = storage.createAdd(THREAD_SUMMARY);
        add.setPojo(summary);
        add.apply();
    }
    
    @Override
    public ThreadSummary loadLastestSummary(VmRef ref) {
        ThreadSummary summary = null;

        Query<ThreadSummary> query = prepareQuery(THREAD_SUMMARY, ThreadSummary.class, ref);
        query.sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        query.limit(1);
        Cursor<ThreadSummary> cursor = query.execute();
        if (cursor.hasNext()) {
            summary = cursor.next();
        }
        
        return summary;
    }
    
    @Override
    public List<ThreadSummary> loadSummary(VmRef ref, long since) {
        
        List<ThreadSummary> result = new ArrayList<>();
        
        Query<ThreadSummary> query = prepareQuery(THREAD_SUMMARY, ThreadSummary.class, ref);
        query.sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        query.where(Key.TIMESTAMP, Criteria.GREATER_THAN, since);

        Cursor<ThreadSummary> cursor = query.execute();
        while (cursor.hasNext()) {
            ThreadSummary summary = cursor.next();
            result.add(summary);
        }
        
        return result;
    }
    
    @Override
    public void saveThreadInfo(ThreadInfoData info) {
        Put add = storage.createAdd(THREAD_INFO);
        add.setPojo(info);
        add.apply();
    }

    @Override
    public List<ThreadInfoData> loadThreadInfo(VmRef ref, long since) {
        List<ThreadInfoData> result = new ArrayList<>();
        
        Query<ThreadInfoData> query = prepareQuery(THREAD_INFO, ThreadInfoData.class, ref);
        query.where(Key.TIMESTAMP, Criteria.GREATER_THAN, since);
        query.sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        
        Cursor<ThreadInfoData> cursor = query.execute();
        while (cursor.hasNext()) {
            ThreadInfoData info = cursor.next();
            result.add(info);
        }
        
        return result;
    }
    
    private <T extends Pojo> Query<T> prepareQuery(Category category, Class<T> resultClass, VmRef vm) {
        return prepareQuery(category, resultClass, vm.getIdString(), vm.getAgent().getAgentId());
    }

    private <T extends Pojo> Query<T> prepareQuery(Category category, Class<T> resultClass, String vmId, String agentId) {
        Query<T> query = storage.createQuery(category, resultClass);
        query.where(Key.AGENT_ID, Query.Criteria.EQUALS, agentId);
        query.where(Key.VM_ID, Query.Criteria.EQUALS, Integer.valueOf(vmId));
        return query;
    }
    
    @Override
    public Storage getStorage() {
        return storage;
    }
}
