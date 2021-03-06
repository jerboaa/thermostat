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
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;

@Service
public interface VmMemoryStatDAO {

    static final Key<Generation[]> generationsKey = new Key<>("generations");
    static final Key<Long> KEY_METASPACE_MAX_CAPACITY = new Key<>("metaspaceMaxCapacity");
    static final Key<Long> KEY_METASPACE_MIN_CAPACITY = new Key<>("metaspaceMinCapacity");
    static final Key<Long> KEY_METASPACE_CAPACITY = new Key<>("metaspaceCapacity");
    static final Key<Long> KEY_METASPACE_USED = new Key<>("metaspaceUsed");

    static final Category<VmMemoryStat> vmMemoryStatsCategory = new Category<>("vm-memory-stats", VmMemoryStat.class,
            Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
                    KEY_METASPACE_MAX_CAPACITY, KEY_METASPACE_MIN_CAPACITY, KEY_METASPACE_CAPACITY, KEY_METASPACE_USED,
                    generationsKey),
            Arrays.<Key<?>>asList(Key.TIMESTAMP));

    public VmMemoryStat getNewestMemoryStat(VmRef ref);

    public VmMemoryStat getOldestMemoryStat(VmRef ref);

    @Deprecated
    public List<VmMemoryStat> getLatestVmMemoryStats(VmRef vm, long since);

    public List<VmMemoryStat> getLatestVmMemoryStats(AgentId agentId, VmId vmId, long since);

    public List<VmMemoryStat> getVmMemoryStats(VmRef vm, long since, long to);

    public void putVmMemoryStat(VmMemoryStat stat);

}

