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

package com.redhat.thermostat.host.memory.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class MemoryStatDAOImpl implements MemoryStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(MemoryStatDAOImpl.class);
    // ADD memory-stats SET 'agentId' = ?s , \
    //                      'timeStamp' = ?l , \
    //                      'total' = ?l , \
    //                      'free' = ?l , \
    //                      'buffers' = ?l , \
    //                      'cached' = ?l , \
    //                      'swapTotal' = ?l , \
    //                      'swapFree' = ?l , \
    //                      'commitLimit' = ?l
    static final String DESC_ADD_MEMORY_STAT = "ADD " + memoryStatCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + memoryTotalKey.getName() + "' = ?l , " +
                 "'" + memoryFreeKey.getName() + "' = ?l , " +
                 "'" + memoryBuffersKey.getName() + "' = ?l , " +
                 "'" + memoryCachedKey.getName() + "' = ?l , " +
                 "'" + memorySwapTotalKey.getName() + "' = ?l , " +
                 "'" + memorySwapFreeKey.getName() + "' = ?l , " +
                 "'" + memoryCommitLimitKey.getName() + "' = ?l";
    
    private final Storage storage;

    private final HostLatestPojoListGetter<MemoryStat> getter;

    MemoryStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(memoryStatCategory);
        this.getter = new HostLatestPojoListGetter<>(storage, memoryStatCategory);
    }

    @Override
    public List<MemoryStat> getLatestMemoryStats(HostRef ref, long lastTimeStamp) {
        return getter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public void putMemoryStat(MemoryStat stat) {
        StatementDescriptor<MemoryStat> desc = new StatementDescriptor<>(memoryStatCategory, DESC_ADD_MEMORY_STAT);
        PreparedStatement<MemoryStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, stat.getAgentId());
            prepared.setLong(1, stat.getTimeStamp());
            prepared.setLong(2, stat.getTotal());
            prepared.setLong(3, stat.getFree());
            prepared.setLong(4, stat.getBuffers());
            prepared.setLong(5, stat.getCached());
            prepared.setLong(6, stat.getSwapTotal());
            prepared.setLong(7, stat.getSwapFree());
            prepared.setLong(8, stat.getCommitLimit());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

}

