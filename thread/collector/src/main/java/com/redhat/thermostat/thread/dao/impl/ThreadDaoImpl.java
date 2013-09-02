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

package com.redhat.thermostat.thread.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
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
    static final String QUERY_THREAD_INFO = "QUERY "
            + THREAD_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";
    static final String QUERY_LATEST_DEADLOCK_INFO = "QUERY "
            + DEADLOCK_INFO.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '" 
            + Key.VM_ID.getName() + "' = ?s SORT '" 
            + Key.TIMESTAMP.getName() + "' DSC LIMIT 1";
    
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
        
        Cursor<VMThreadCapabilities> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return null;
        }
        
        VMThreadCapabilities caps = null;
        if (cursor.hasNext()) {
            caps = cursor.next();
        }
        
        return caps;
    }
    
    @Override
    public void saveCapabilities(VMThreadCapabilities caps) {
        @SuppressWarnings("unchecked")
        Replace<VMThreadCapabilities> replace = storage.createReplace(THREAD_CAPABILITIES);
        ExpressionFactory factory = new ExpressionFactory();
        String agentId = caps.getAgentId();
        Expression agentKey = factory.equalTo(Key.AGENT_ID, agentId);
        Expression vmKey = factory.equalTo(Key.VM_ID, caps.getVmId());
        Expression and = factory.and(agentKey, vmKey);
        replace.setPojo(caps);
        replace.where(and);
        replace.apply();
    }
    
    @Override
    public void saveSummary(ThreadSummary summary) {
        @SuppressWarnings("unchecked")
        Add<ThreadSummary> add = storage.createAdd(THREAD_SUMMARY);
        add.setPojo(summary);
        add.apply();
    }
    
    @Override
    public ThreadSummary loadLastestSummary(VmRef ref) {
        PreparedStatement<ThreadSummary> stmt = prepareQuery(THREAD_SUMMARY, QUERY_LATEST_SUMMARY, ref);
        if (stmt == null) {
            return null;
        }
        
        Cursor<ThreadSummary> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return null;
        }
        
        ThreadSummary summary = null;
        if (cursor.hasNext()) {
            summary = cursor.next();
        }
        
        return summary;
    }
    
    @Override
    public List<ThreadSummary> loadSummary(VmRef ref, long since) {
        PreparedStatement<ThreadSummary> stmt = prepareQuery(THREAD_SUMMARY, QUERY_SUMMARY_SINCE, ref, since);
        if (stmt == null) {
            return Collections.emptyList();
        }

        Cursor<ThreadSummary> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return Collections.emptyList();
        }
        
        List<ThreadSummary> result = new ArrayList<>();
        while (cursor.hasNext()) {
            ThreadSummary summary = cursor.next();
            result.add(summary);
        }
        
        return result;
    }

    @Override
    public void saveHarvestingStatus(ThreadHarvestingStatus status) {
        @SuppressWarnings("unchecked")
        Add<ThreadHarvestingStatus> add = storage.createAdd(THREAD_HARVESTING_STATUS);
        add.setPojo(status);
        add.apply();
    }

    @Override
    public ThreadHarvestingStatus getLatestHarvestingStatus(VmRef vm) {
        PreparedStatement<ThreadHarvestingStatus> stmt = prepareQuery(THREAD_HARVESTING_STATUS, 
                QUERY_LATEST_HARVESTING_STATUS, vm);
        if (stmt == null) {
            return null;
        }
        
        Cursor<ThreadHarvestingStatus> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return null;
        }
        
        ThreadHarvestingStatus result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }
        return result;
    }

    @Override
    public void saveThreadInfo(ThreadInfoData info) {
        @SuppressWarnings("unchecked")
        Add<ThreadInfoData> add = storage.createAdd(THREAD_INFO);
        add.setPojo(info);
        add.apply();
    }

    @Override
    public List<ThreadInfoData> loadThreadInfo(VmRef ref, long since) {
        PreparedStatement<ThreadInfoData> stmt = prepareQuery(THREAD_INFO, QUERY_THREAD_INFO, ref, since);
        if (stmt == null) {
            return Collections.emptyList();
        }
        
        Cursor<ThreadInfoData> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return Collections.emptyList();
        }
        
        List<ThreadInfoData> result = new ArrayList<>();
        while (cursor.hasNext()) {
            ThreadInfoData info = cursor.next();
            result.add(info);
        }
        
        return result;
    }

    @Override
    public VmDeadLockData loadLatestDeadLockStatus(VmRef ref) {
        PreparedStatement<VmDeadLockData> stmt = prepareQuery(DEADLOCK_INFO, QUERY_LATEST_DEADLOCK_INFO, ref);
        if (stmt == null) {
            return null;
        }
        
        Cursor<VmDeadLockData> cursor;
        try {
            cursor = stmt.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + stmt + "' failed!", e);
            return null;
        }
        
        VmDeadLockData result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }

        return result;
    }

    @Override
    public void saveDeadLockStatus(VmDeadLockData deadLockInfo) {
        @SuppressWarnings("unchecked")
        Add<VmDeadLockData> add = storage.createAdd(DEADLOCK_INFO);
        add.setPojo(deadLockInfo);
        add.apply();
    }
    
    private <T extends Pojo> PreparedStatement<T> prepareQuery(Category<T> category, String query, VmRef ref) {
        return prepareQuery(category, query, ref, null);
    }
    
    private <T extends Pojo> PreparedStatement<T> prepareQuery(Category<T> category, String query, VmRef ref, Long since) {
        StatementDescriptor<T> desc = new StatementDescriptor<>(category, query);
        PreparedStatement<T> stmt = null;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getHostRef().getAgentId());
            stmt.setString(1, ref.getVmId());
            if (since != null) {
                stmt.setLong(2, since);
            }
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        }
        return stmt;
    }
    
    @Override
    public Storage getStorage() {
        return storage;
    }
}

