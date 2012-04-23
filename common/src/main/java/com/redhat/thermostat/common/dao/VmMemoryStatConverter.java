/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmMemoryStatConverter implements Converter<VmMemoryStat> {

    @Override
    public Chunk toChunk(VmMemoryStat vmMemStat) {
        Chunk chunk = new Chunk(VmMemoryStatDAO.vmMemoryStatsCategory, false);

        chunk.put(Key.VM_ID, vmMemStat.getVmId());
        chunk.put(Key.TIMESTAMP, vmMemStat.getTimeStamp());

        Generation newGen = vmMemStat.getGeneration("new");

        Space eden = newGen.getSpace("eden");
        chunk.put(VmMemoryStatDAO.edenGenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.edenCollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.edenCapacityKey, eden.capacity);
        chunk.put(VmMemoryStatDAO.edenMaxCapacityKey, eden.maxCapacity);
        chunk.put(VmMemoryStatDAO.edenUsedKey, eden.used);

        Space s0 = newGen.getSpace("s0");
        chunk.put(VmMemoryStatDAO.s0GenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.s0CollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.s0CapacityKey, s0.capacity);
        chunk.put(VmMemoryStatDAO.s0MaxCapacityKey, s0.maxCapacity);
        chunk.put(VmMemoryStatDAO.s0UsedKey, s0.used);

        Space s1 = newGen.getSpace("s1");
        chunk.put(VmMemoryStatDAO.s1GenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.s1CollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.s1CapacityKey, s1.capacity);
        chunk.put(VmMemoryStatDAO.s1MaxCapacityKey, s1.maxCapacity);
        chunk.put(VmMemoryStatDAO.s1UsedKey, s1.used);

        Generation oldGen = vmMemStat.getGeneration("old");

        Space old = oldGen.getSpace("old");
        chunk.put(VmMemoryStatDAO.oldGenKey, oldGen.name);
        chunk.put(VmMemoryStatDAO.oldCollectorKey, oldGen.collector);
        chunk.put(VmMemoryStatDAO.oldCapacityKey, old.capacity);
        chunk.put(VmMemoryStatDAO.oldMaxCapacityKey, old.maxCapacity);
        chunk.put(VmMemoryStatDAO.oldUsedKey, old.used);

        Generation permGen = vmMemStat.getGeneration("perm");

        Space perm = permGen.getSpace("perm");
        chunk.put(VmMemoryStatDAO.permGenKey, permGen.name);
        chunk.put(VmMemoryStatDAO.permCollectorKey, permGen.collector);
        chunk.put(VmMemoryStatDAO.permCapacityKey, perm.capacity);
        chunk.put(VmMemoryStatDAO.permMaxCapacityKey, perm.maxCapacity);
        chunk.put(VmMemoryStatDAO.permUsedKey, perm.used);

        return chunk;
    }

    @Override
    public VmMemoryStat fromChunk(Chunk chunk) {
        Space space = null;
        List<Space> spaces = null;

        List<Generation> gens = new ArrayList<>();
        Generation newGen = new Generation();
        spaces = new ArrayList<>();
        newGen.spaces = spaces;
        newGen.name = "new";
        newGen.capacity = 0;
        newGen.maxCapacity = 0;
        // FIXME Something is wrong here when we have the collector stored
        // as part of 3 spaces in Chunk but is only one thing in Stat
        newGen.collector = chunk.get(VmMemoryStatDAO.edenCollectorKey);

        space = new Space();
        space.name = VmMemoryStatDAO.edenKey.getName();
        space.capacity = chunk.get(VmMemoryStatDAO.edenCapacityKey);
        space.maxCapacity = chunk.get(VmMemoryStatDAO.edenMaxCapacityKey);
        space.used = chunk.get(VmMemoryStatDAO.edenUsedKey);
        spaces.add(space);
        newGen.capacity += space.capacity;
        newGen.maxCapacity += space.maxCapacity;

        space = new Space();
        space.name = VmMemoryStatDAO.s0Key.getName();
        space.capacity = chunk.get(VmMemoryStatDAO.s0CapacityKey);
        space.maxCapacity = chunk.get(VmMemoryStatDAO.s0MaxCapacityKey);
        space.used = chunk.get(VmMemoryStatDAO.s0UsedKey);
        spaces.add(space);
        newGen.capacity += space.capacity;
        newGen.maxCapacity += space.maxCapacity;

        space = new Space();
        space.name = VmMemoryStatDAO.s1Key.getName();
        space.capacity = chunk.get(VmMemoryStatDAO.s1CapacityKey);
        space.maxCapacity = chunk.get(VmMemoryStatDAO.s1MaxCapacityKey);
        space.used = chunk.get(VmMemoryStatDAO.s1UsedKey);
        spaces.add(space);
        newGen.capacity += space.capacity;
        newGen.maxCapacity += space.maxCapacity;

        gens.add(newGen);

        Generation oldGen = new Generation();
        spaces = new ArrayList<>();
        oldGen.spaces = spaces;
        oldGen.name = "old";
        oldGen.collector = chunk.get(VmMemoryStatDAO.oldCollectorKey);

        space = new Space();
        space.name = VmMemoryStatDAO.oldKey.getName();
        space.capacity = chunk.get(VmMemoryStatDAO.oldCapacityKey);
        space.maxCapacity = chunk.get(VmMemoryStatDAO.oldMaxCapacityKey);
        space.used = chunk.get(VmMemoryStatDAO.oldUsedKey);
        spaces.add(space);
        oldGen.capacity = space.capacity;
        oldGen.maxCapacity = space.capacity;

        gens.add(oldGen);

        Generation permGen = new Generation();
        spaces = new ArrayList<>();
        permGen.spaces = spaces;
        permGen.name = "perm";
        permGen.collector = chunk.get(VmMemoryStatDAO.permCollectorKey);

        space = new Space();
        space.name = VmMemoryStatDAO.permKey.getName();
        space.capacity = chunk.get(VmMemoryStatDAO.permCapacityKey);
        space.maxCapacity = chunk.get(VmMemoryStatDAO.permMaxCapacityKey);
        space.used = chunk.get(VmMemoryStatDAO.permUsedKey);
        spaces.add(space);
        permGen.capacity = space.capacity;
        permGen.maxCapacity = space.capacity;

        gens.add(permGen);

        return new VmMemoryStat(chunk.get(Key.TIMESTAMP), chunk.get(Key.VM_ID), gens);
    }
}
