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

package com.redhat.thermostat.vm.memory.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;

class VmMemoryStatDAOImpl implements VmMemoryStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(VmMemoryStatDAOImpl.class);
    // QUERY vm-cpu-stats WHERE 'agentId' = ?s AND \
    //                        'vmId' = ?s \
    //                        SORT 'timeStamp' ASC  \
    //                        LIMIT 1
    static final String DESC_OLDEST_VM_MEMORY_STAT = "QUERY " + vmMemoryStatsCategory.getName() + " " +
            "WHERE '" + Key.AGENT_ID.getName() + "' = ?s " +
            "AND '" + Key.VM_ID.getName() + "' = ?s " +
            "SORT '" + Key.TIMESTAMP.getName() + "' ASC " +
            "LIMIT 1";

    // QUERY vm-cpu-stats WHERE 'agentId' = ?s AND \
    //                        'vmId' = ?s \
    //                        SORT 'timeStamp' DSC  \
    //                        LIMIT 1
    static final String DESC_LATEST_VM_MEMORY_STAT = "QUERY " + vmMemoryStatsCategory.getName() + " " +
            "WHERE '" + Key.AGENT_ID.getName() + "' = ?s " +
            "AND '" + Key.VM_ID.getName() + "' = ?s " +
            "SORT '" + Key.TIMESTAMP.getName() + "' DSC " +
            "LIMIT 1";
    // ADD vm-memory-stats SET 'agentId' = ?s , \
    //                         'vmId' = ?s , \
    //                         'timeStamp' = ?s , \
    //                         'generations' = ?p[
    static final String DESC_ADD_VM_MEMORY_STAT = "ADD " + vmMemoryStatsCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + generationsKey.getName() + "' = ?p[";
    
    private final Storage storage;
    private final VmLatestPojoListGetter<VmMemoryStat> getter;
    private final VmTimeIntervalPojoListGetter<VmMemoryStat> otherGetter;

    VmMemoryStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmMemoryStatsCategory);
        getter = new VmLatestPojoListGetter<>(storage, vmMemoryStatsCategory);
        otherGetter = new VmTimeIntervalPojoListGetter<>(storage, vmMemoryStatsCategory);
    }

    @Override
    public VmMemoryStat getLatestMemoryStat(VmRef ref) {
        return runAgentAndVmIdQuery(ref, DESC_LATEST_VM_MEMORY_STAT);
    }

    @Override
    public VmMemoryStat getOldestMemoryStat(VmRef ref) {
        return runAgentAndVmIdQuery(ref, DESC_OLDEST_VM_MEMORY_STAT);
    }

    private VmMemoryStat runAgentAndVmIdQuery(VmRef ref, String descriptor) {
        StatementDescriptor<VmMemoryStat> desc = new StatementDescriptor<>(vmMemoryStatsCategory, descriptor);
        PreparedStatement<VmMemoryStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, ref.getHostRef().getAgentId());
            prepared.setString(1, ref.getVmId());
            Cursor<VmMemoryStat> cursor = prepared.executeQuery();
            if (cursor.hasNext()) {
                return cursor.next();
            }
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
        return null;
    }

    @Override
    public void putVmMemoryStat(VmMemoryStat stat) {
        StatementDescriptor<VmMemoryStat> desc = new StatementDescriptor<>(vmMemoryStatsCategory, DESC_ADD_VM_MEMORY_STAT);
        PreparedStatement<VmMemoryStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, stat.getAgentId());
            prepared.setString(1, stat.getVmId());
            prepared.setLong(2, stat.getTimeStamp());
            prepared.setPojoList(3, stat.getGenerations());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public List<VmMemoryStat> getLatestVmMemoryStats(VmRef ref, long since) {
        return getter.getLatest(ref, since);
    }

    @Override
    public List<VmMemoryStat> getVmMemoryStats(VmRef ref, long since, long to) {
        return otherGetter.getLatest(ref, since, to);
    }
}

