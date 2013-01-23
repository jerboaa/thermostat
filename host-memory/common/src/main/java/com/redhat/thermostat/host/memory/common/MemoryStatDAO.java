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

package com.redhat.thermostat.host.memory.common;

import java.util.List;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.common.dao.Countable;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.MemoryStat;

@Service
public interface MemoryStatDAO extends Countable {

    static Key<Long> memoryTotalKey = new Key<>("total", false);
    static Key<Long> memoryFreeKey = new Key<>("free", false);
    static Key<Long> memoryBuffersKey = new Key<>("buffers", false);
    static Key<Long> memoryCachedKey = new Key<>("cached", false);
    static Key<Long> memorySwapTotalKey = new Key<>("swapTotal", false);
    static Key<Long> memorySwapFreeKey = new Key<>("swapFree", false);
    static Key<Long> memoryCommitLimitKey = new Key<>("commitLimit", false);

    static final Category<MemoryStat> memoryStatCategory = new Category<>("memory-stats", MemoryStat.class,
            Key.AGENT_ID, Key.TIMESTAMP, memoryTotalKey, memoryFreeKey, memoryBuffersKey,
            memoryCachedKey, memorySwapTotalKey, memorySwapFreeKey, memoryCommitLimitKey);

    public List<MemoryStat> getLatestMemoryStats(HostRef ref, long since);

    void putMemoryStat(MemoryStat stat);
}

