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

package com.redhat.thermostat.host.cpu.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

class CpuStatDAOImpl implements CpuStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(CpuStatDAOImpl.class);
    // ADD cpu-stats SET 'agentId' = ?s , \
    //                   'perProcessorUsage' = ?d[ , \
    //                   'timeStamp' = ?l
    static final String DESC_ADD_CPU_STAT = "ADD " + cpuStatCategory.getName() +
                           " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                                "'" + cpuLoadKey.getName() + "' = ?d[ , " +
                                "'" + Key.TIMESTAMP.getName() + "' = ?l";
    
    private final Storage storage;

    private final HostLatestPojoListGetter<CpuStat> getter;

    CpuStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(cpuStatCategory);
        this.getter = new HostLatestPojoListGetter<>(storage, cpuStatCategory);
    }

    @Override
    public List<CpuStat> getLatestCpuStats(HostRef ref, long lastTimeStamp) {
        return getter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public void putCpuStat(CpuStat stat) {
        StatementDescriptor<CpuStat> desc = new StatementDescriptor<>(cpuStatCategory, DESC_ADD_CPU_STAT);
        PreparedStatement<CpuStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, stat.getAgentId());
            prepared.setDoubleList(1, stat.getPerProcessorUsage());
            prepared.setLong(2, stat.getTimeStamp());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }
}

