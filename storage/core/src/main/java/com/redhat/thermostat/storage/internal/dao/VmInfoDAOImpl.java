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

package com.redhat.thermostat.storage.internal.dao;

import com.redhat.thermostat.storage.core.VmId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.VmInfo;

public class VmInfoDAOImpl extends BaseCountable implements VmInfoDAO {
    
    private final Logger logger = LoggingUtils.getLogger(VmInfoDAOImpl.class);
    static final String QUERY_VM_INFO = "QUERY " 
            + vmInfoCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s LIMIT 1";
    static final String QUERY_ALL_VMS_FOR_HOST = "QUERY " 
            + vmInfoCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s";

    static final String QUERY_VM_FROM_ID = "QUERY "
            + vmInfoCategory.getName() + " WHERE '"
            + Key.VM_ID.getName() + "' = ?s";

    static final String QUERY_ALL_VMS = "QUERY " + vmInfoCategory.getName();
    static final String AGGREGATE_COUNT_ALL_VMS = "QUERY-COUNT " + vmInfoCategory.getName();
    // ADD vm-info SET 'agentId' = ?s , \
    //                 'vmId' = ?s , \
    //                 'vmPid' = ?i , \
    //                 'startTimeStamp' = ?l , \
    //                 'stopTimeStamp' = ?l , \
    //                 'javaVersion' = ?s , \
    //                 'javaHome' = ?s , \
    //                 'mainClass' = ?s , \
    //                 'javaCommandLine' = ?s , \
    //                 'vmName' = ?s , \
    //                 'vmArguments' = ?s , \
    //                 'vmInfo' = ?s , \
    //                 'vmVersion' = ?s , \
    //                 'propertiesAsArray' = ?p[ , \
    //                 'environmentAsArray' = ?p[ , \
    //                 'loadedNativeLibraries' = ?s[ , \
    //                 'uid' = ?l , \
    //                 'username' = ?s
    static final String DESC_ADD_VM_INFO = "ADD " + vmInfoCategory.getName() + " SET " +
                        "'" + Key.AGENT_ID.getName() + "' = ?s , " +
                        "'" + Key.VM_ID.getName() + "' = ?s , " +
                        "'" + vmPidKey.getName() + "' = ?i , " +
                        "'" + startTimeKey.getName() + "' = ?l , " +
                        "'" + stopTimeKey.getName() + "' = ?l , " +
                        "'" + runtimeVersionKey.getName() + "' = ?s , " +
                        "'" + javaHomeKey.getName() + "' = ?s , " +
                        "'" + mainClassKey.getName() + "' = ?s , " +
                        "'" + commandLineKey.getName() + "' = ?s , " +
                        "'" + vmNameKey.getName() + "' = ?s , " +
                        "'" + vmArgumentsKey.getName() + "' = ?s , " +
                        "'" + vmInfoKey.getName() + "' = ?s , " +
                        "'" + vmVersionKey.getName() + "' = ?s , " +
                        "'" + propertiesKey.getName() + "' = ?p[ , " +
                        "'" + environmentKey.getName() + "' = ?p[ , " +
                        "'" + librariesKey.getName() + "' = ?s[ , " +
                        "'" + uidKey.getName() + "' = ?l , " +
                        "'" + usernameKey.getName() + "' = ?s";
    // UPDATE vm-info SET 'stopTimeStamp' = ?l WHERE 'vmId' = ?s
    static final String DESC_UPDATE_VM_STOP_TIME = "UPDATE " + vmInfoCategory.getName() +
            " SET '" + VmInfoDAO.stopTimeKey.getName() + "' = ?l" +
            " WHERE '" + Key.VM_ID.getName() + "' = ?s";
    
    private final Storage storage;
    private final Category<AggregateCount> aggregateCategory;

    public VmInfoDAOImpl(Storage storage) {
        this.storage = storage;
        // Adapt category to the aggregate form
        CategoryAdapter<VmInfo, AggregateCount> adapter = new CategoryAdapter<>(vmInfoCategory);
        this.aggregateCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(vmInfoCategory);
        storage.registerCategory(aggregateCategory);
    }

    @Override
    public VmInfo getVmInfo(VmId id) {

        VmInfo result = null;

        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(vmInfoCategory, QUERY_VM_FROM_ID);
        PreparedStatement<VmInfo> stmt;

        Cursor<VmInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, id.get());

            cursor = stmt.executeQuery();
            if (cursor.hasNext()) {
                result = cursor.next();
            }

        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);

        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
        }
        return result;
    }

    @Override
    public VmInfo getVmInfo(VmRef ref) {
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(vmInfoCategory, QUERY_VM_INFO);
        PreparedStatement<VmInfo> stmt;
        Cursor<VmInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getHostRef().getAgentId());
            stmt.setString(1, ref.getVmId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        
        VmInfo result;
        if (cursor.hasNext()) {
            result = cursor.next();
        }
        else {
            // FIXME this is inconsistent with null returned elsewhere
            throw new DAOException("Unknown VM: host:" + ref.getHostRef().getAgentId() + ";vm:" + ref.getVmId());
        }
        return result;
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(vmInfoCategory, QUERY_ALL_VMS_FOR_HOST);
        PreparedStatement<VmInfo> stmt;
        Cursor<VmInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, host.getAgentId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        return buildVMsFromQuery(cursor, host);
    }

    private Collection<VmRef> buildVMsFromQuery(Cursor<VmInfo> cursor, HostRef host) {
        List<VmRef> vmRefs = new ArrayList<VmRef>();
        while (cursor.hasNext()) {
            VmInfo vmInfo = cursor.next();
            VmRef vm = buildVmRefFromChunk(vmInfo, host);
            vmRefs.add(vm);
        }

        return vmRefs;
    }

    private VmRef buildVmRefFromChunk(VmInfo vmInfo, HostRef host) {
        String id = vmInfo.getVmId();
        Integer pid = vmInfo.getVmPid();
        // TODO can we do better than the main class?
        String mainClass = vmInfo.getMainClass();
        VmRef ref = new VmRef(host, id, pid, mainClass);
        return ref;
    }

    @Override
    public long getCount() {
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(
                aggregateCategory, AGGREGATE_COUNT_ALL_VMS);
        long count = getCount(desc, storage);
        return count;
    }

    @Override
    public void putVmInfo(VmInfo info) {
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(vmInfoCategory, DESC_ADD_VM_INFO);
        PreparedStatement<VmInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, info.getAgentId());
            prepared.setString(1, info.getVmId());
            prepared.setInt(2, info.getVmPid());
            prepared.setLong(3, info.getStartTimeStamp());
            prepared.setLong(4, info.getStopTimeStamp());
            prepared.setString(5, info.getJavaVersion());
            prepared.setString(6, info.getJavaHome());
            prepared.setString(7, info.getMainClass());
            prepared.setString(8, info.getJavaCommandLine());
            prepared.setString(9, info.getVmName());
            prepared.setString(10, info.getVmArguments());
            prepared.setString(11, info.getVmInfo());
            prepared.setString(12, info.getVmVersion());
            prepared.setPojoList(13, info.getPropertiesAsArray());
            prepared.setPojoList(14, info.getEnvironmentAsArray());
            prepared.setStringList(15, info.getLoadedNativeLibraries());
            prepared.setLong(16, info.getUid());
            prepared.setString(17, info.getUsername());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public void putVmStoppedTime(String vmId, long timestamp) {
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(vmInfoCategory, DESC_UPDATE_VM_STOP_TIME);
        PreparedStatement<VmInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setLong(0, timestamp);
            prepared.setString(1, vmId);
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

}

