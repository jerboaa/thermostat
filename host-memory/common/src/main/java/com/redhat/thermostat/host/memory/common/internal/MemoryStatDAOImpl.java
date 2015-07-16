/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.storage.core.HostBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;

public class MemoryStatDAOImpl extends AbstractDao implements MemoryStatDAO {

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

    private final HostLatestPojoListGetter<MemoryStat> latestGetter;
    private final HostTimeIntervalPojoListGetter<MemoryStat> intervalGetter;
    private final HostBoundaryPojoGetter<MemoryStat> boundaryGetter;

    MemoryStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(memoryStatCategory);
        this.latestGetter = new HostLatestPojoListGetter<>(storage, memoryStatCategory);
        this.intervalGetter = new HostTimeIntervalPojoListGetter<>(storage, memoryStatCategory);
        this.boundaryGetter = new HostBoundaryPojoGetter<>(storage, memoryStatCategory);
    }

    @Override
    public List<MemoryStat> getLatestMemoryStats(HostRef ref, long lastTimeStamp) {
        return latestGetter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public List<MemoryStat> getMemoryStats(HostRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    public MemoryStat getNewest(HostRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public MemoryStat getOldest(HostRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public void putMemoryStat(final MemoryStat stat) {
        executeStatement(new AbstractDaoStatement<MemoryStat>(storage, memoryStatCategory, DESC_ADD_MEMORY_STAT) {
            @Override
            public PreparedStatement<MemoryStat> customize(PreparedStatement<MemoryStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setLong(1, stat.getTimeStamp());
                preparedStatement.setLong(2, stat.getTotal());
                preparedStatement.setLong(3, stat.getFree());
                preparedStatement.setLong(4, stat.getBuffers());
                preparedStatement.setLong(5, stat.getCached());
                preparedStatement.setLong(6, stat.getSwapTotal());
                preparedStatement.setLong(7, stat.getSwapFree());
                preparedStatement.setLong(8, stat.getCommitLimit());
                return preparedStatement;
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

