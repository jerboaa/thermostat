/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
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
    public VmInfo getVmInfo(final VmId id) {
        return executeQuery(
                new AbstractDaoQuery<VmInfo>(storage, vmInfoCategory, QUERY_VM_FROM_ID) {
                    @Override
                    public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                        preparedStatement.setString(0, id.get());
                        return preparedStatement;
                    }
                }).head();
    }

    @Override
    public VmInfo getVmInfo(final VmRef ref) {
        return executeQuery(
                new AbstractDaoQuery<VmInfo>(storage, vmInfoCategory, QUERY_VM_INFO) {
                    @Override
                    public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                        preparedStatement.setString(0, ref.getHostRef().getAgentId());
                        preparedStatement.setString(1, ref.getVmId());
                        return preparedStatement;
                    }
                }).head();
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {
        AgentId agentId = new AgentId(host.getAgentId());

        Collection<VmInfo> vmInfos = getAllVmInfosForHost(agentId);
        if (vmInfos.equals(Collections.emptyList())) {
            return Collections.emptyList();
        }

        return buildVMsFromQuery(vmInfos, host);
    }

    @Deprecated
    private Collection<VmRef> buildVMsFromQuery(Collection<VmInfo> vmInfos, HostRef host) {
        List<VmRef> vmRefs = new ArrayList<>();
        for (VmInfo vmInfo : vmInfos) {
            VmRef vm = buildVmRefFromChunk(vmInfo, host);
            vmRefs.add(vm);
        }

        return vmRefs;
    }

    @Deprecated
    private VmRef buildVmRefFromChunk(VmInfo vmInfo, HostRef host) {
        String id = vmInfo.getVmId();
        Integer pid = vmInfo.getVmPid();
        // TODO can we do better than the main class?
        String mainClass = vmInfo.getMainClass();
        return new VmRef(host, id, pid, mainClass);
    }

    @Override
    public Set<VmId> getVmIds(AgentId agentId) {
        Set<VmId> vmIds = new HashSet<>();
        Collection<VmInfo> vmInfos = getAllVmInfosForHost(agentId);
        for (VmInfo vmInfo : vmInfos) {
            vmIds.add(new VmId(vmInfo.getVmId()));
        }

        return vmIds;
    }

    private Collection<VmInfo> getAllVmInfosForHost(final AgentId agentId) {
        return executeQuery(
                new AbstractDaoQuery<VmInfo>(storage, vmInfoCategory, QUERY_ALL_VMS_FOR_HOST) {
                    @Override
                    public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                        preparedStatement.setString(0, agentId.get());
                        return preparedStatement;
                    }
                }).asList();
    }

    @Override
    public long getCount() {
        return getCount(storage, aggregateCategory, AGGREGATE_COUNT_ALL_VMS);
    }

    @Override
    public void putVmInfo(final VmInfo info) {
        executeStatement(
                new AbstractDaoStatement<VmInfo>(storage, vmInfoCategory, DESC_ADD_VM_INFO) {
                    @Override
                    public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                        preparedStatement.setString(0, info.getAgentId());
                        preparedStatement.setString(1, info.getVmId());
                        preparedStatement.setInt(2, info.getVmPid());
                        preparedStatement.setLong(3, info.getStartTimeStamp());
                        preparedStatement.setLong(4, info.getStopTimeStamp());
                        preparedStatement.setString(5, info.getJavaVersion());
                        preparedStatement.setString(6, info.getJavaHome());
                        preparedStatement.setString(7, info.getMainClass());
                        preparedStatement.setString(8, info.getJavaCommandLine());
                        preparedStatement.setString(9, info.getVmName());
                        preparedStatement.setString(10, info.getVmArguments());
                        preparedStatement.setString(11, info.getVmInfo());
                        preparedStatement.setString(12, info.getVmVersion());
                        preparedStatement.setPojoList(13, info.getPropertiesAsArray());
                        preparedStatement.setPojoList(14, info.getEnvironmentAsArray());
                        preparedStatement.setStringList(15, info.getLoadedNativeLibraries());
                        preparedStatement.setLong(16, info.getUid());
                        preparedStatement.setString(17, info.getUsername());
                        return preparedStatement;
                    }
                });
    }

    @Override
    public void putVmStoppedTime(final String vmId, final long timestamp) {
        executeStatement(
                new AbstractDaoStatement<VmInfo>(storage, vmInfoCategory, DESC_UPDATE_VM_STOP_TIME) {
                    @Override
                    public PreparedStatement<VmInfo> customize(PreparedStatement<VmInfo> preparedStatement) {
                        preparedStatement.setLong(0, timestamp);
                        preparedStatement.setString(1, vmId);
                        return preparedStatement;
                    }
                });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

