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

package com.redhat.thermostat.vm.classstat.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;

class VmClassStatDAOImpl implements VmClassStatDAO {
    
    private static final Logger logger = LoggingUtils.getLogger(VmClassStatDAOImpl.class);
    // ADD vm-class-stats SET 'agentId' = ?s , \
    //                        'vmId' = ?s , \
    //                        'timeStamp' = ?l , \ 
    //                        'loadedClasses' = ?l
    static final String DESC_ADD_VM_CLASS_STAT = "ADD " + vmClassStatsCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + loadedClassesKey.getName() + "' = ?l";

    private final Storage storage;
    private final VmLatestPojoListGetter<VmClassStat> getter;

    VmClassStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmClassStatsCategory);
        this.getter = new VmLatestPojoListGetter<>(storage, vmClassStatsCategory);
    }

    @Override
    public List<VmClassStat> getLatestClassStats(VmRef ref, long lastUpdateTime) {
        return getter.getLatest(ref, lastUpdateTime);
    }

    @Override
    public void putVmClassStat(VmClassStat stat) {
        StatementDescriptor<VmClassStat> desc = new StatementDescriptor<>(vmClassStatsCategory, DESC_ADD_VM_CLASS_STAT);
        PreparedStatement<VmClassStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, stat.getAgentId());
            prepared.setString(1, stat.getVmId());
            prepared.setLong(2, stat.getTimeStamp());
            prepared.setLong(3, stat.getLoadedClasses());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }
}

