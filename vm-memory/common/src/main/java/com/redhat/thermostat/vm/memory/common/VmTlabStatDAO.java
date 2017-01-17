/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.common;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.memory.common.model.VmTlabStat;

@Service
public interface VmTlabStatDAO {

    static final Key<Long> KEY_TOTAL_ALLOCATING_THREADS = new Key<>("totalAllocatingThreads");
    static final Key<Long> KEY_TOTAL_ALLOCATIONS = new Key<>("totalAllocations");
    static final Key<Long> KEY_TOTAL_REFILLS = new Key<>("totalRefills");
    static final Key<Long> KEY_MAX_REFILLS = new Key<>("maxRefills");
    static final Key<Long> KEY_TOTAL_SLOW_ALLOCATIONS = new Key<>("totalSlowAllocations");
    static final Key<Long> KEY_MAX_SLOW_ALLOCATIONS = new Key<>("maxSlowAllocations");
    static final Key<Long> KEY_TOTAL_GC_WASTE = new Key<>("totalGcWaste");
    static final Key<Long> KEY_MAX_GC_WASTE = new Key<>("maxGcWaste");
    static final Key<Long> KEY_TOTAL_SLOW_WASTE = new Key<>("totalSlowWaste");
    static final Key<Long> KEY_MAX_SLOW_WASTE = new Key<>("maxSlowWaste");
    static final Key<Long> KEY_TOTAL_FAST_WASTE = new Key<>("totalFastWaste");
    static final Key<Long> KEY_MAX_FAST_WASTE = new Key<>("maxFastWaste");

    static final Category<VmTlabStat> vmTlabStatsCategory = new Category<>("vm-tlab-stats", VmTlabStat.class,
            Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
                    KEY_TOTAL_ALLOCATING_THREADS, KEY_TOTAL_ALLOCATIONS,
                    KEY_TOTAL_REFILLS, KEY_MAX_REFILLS,
                    KEY_TOTAL_SLOW_ALLOCATIONS, KEY_MAX_SLOW_ALLOCATIONS,
                    KEY_TOTAL_GC_WASTE, KEY_MAX_GC_WASTE,
                    KEY_TOTAL_SLOW_WASTE, KEY_MAX_SLOW_WASTE,
                    KEY_TOTAL_FAST_WASTE, KEY_MAX_FAST_WASTE),
            Arrays.<Key<?>>asList(Key.TIMESTAMP));

    @Deprecated
    public VmTlabStat getNewestStat(VmRef ref);

    @Deprecated
    public VmTlabStat getOldestStat(VmRef ref);

    @Deprecated
    public List<VmTlabStat> getLatestStats(VmRef vm, long since);

    public List<VmTlabStat> getLatestStats(AgentId agentId, VmId vmId, long since);

    @Deprecated
    public List<VmTlabStat> getStats(VmRef vm, long since, long to);

    public void putStat(VmTlabStat stat);

}
