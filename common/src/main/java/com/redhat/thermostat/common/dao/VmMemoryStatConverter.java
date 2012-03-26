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

import com.mongodb.DBObject;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmMemoryStatConverter {

    public Chunk vmMemoryStatToChunk(VmMemoryStat vmMemStat) {
        Chunk chunk = new Chunk(VmMemoryStatDAO.vmMemoryStatsCategory, false);

        chunk.put(VmMemoryStatDAO.vmIdKey, vmMemStat.getVmId());
        chunk.put(Key.TIMESTAMP, vmMemStat.getTimeStamp());

        Generation newGen = vmMemStat.getGeneration("new");
        Space eden = newGen.getSpace("eden");

        chunk.put(VmMemoryStatDAO.vmMemoryStatEdenGenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.vmMemoryStatEdenCollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.vmMemoryStatEdenCapacityKey, eden.capacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatEdenMaxCapacityKey, eden.maxCapacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatEdenUsedKey, eden.used);

        Space s0 = newGen.getSpace("s0");
        chunk.put(VmMemoryStatDAO.vmMemoryStatS0GenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS0CollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS0CapacityKey, s0.capacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS0MaxCapacityKey, s0.maxCapacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS0UsedKey, s0.used);

        Space s1 = newGen.getSpace("s1");
        chunk.put(VmMemoryStatDAO.vmMemoryStatS1GenKey, newGen.name);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS1CollectorKey, newGen.collector);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS1CapacityKey, s1.capacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS1MaxCapacityKey, s1.maxCapacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatS1UsedKey, s1.used);

        Generation oldGen = vmMemStat.getGeneration("old");
        Space old = oldGen.getSpace("old");

        chunk.put(VmMemoryStatDAO.vmMemoryStatOldGenKey, oldGen.name);
        chunk.put(VmMemoryStatDAO.vmMemoryStatOldCollectorKey, oldGen.collector);
        chunk.put(VmMemoryStatDAO.vmMemoryStatOldCapacityKey, old.capacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatOldMaxCapacityKey, old.maxCapacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatOldUsedKey, old.used);

        Generation permGen = vmMemStat.getGeneration("perm");
        Space perm = permGen.getSpace("perm");

        chunk.put(VmMemoryStatDAO.vmMemoryStatPermGenKey, permGen.name);
        chunk.put(VmMemoryStatDAO.vmMemoryStatPermCollectorKey, permGen.collector);
        chunk.put(VmMemoryStatDAO.vmMemoryStatPermCapacityKey, perm.capacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatPermMaxCapacityKey, perm.maxCapacity);
        chunk.put(VmMemoryStatDAO.vmMemoryStatPermUsedKey, perm.used);

        return chunk;
    }

    public VmMemoryStat createVmMemoryStatFromDBObject(DBObject dbObj) {
        // FIXME so much hardcoding :'(

        String[] spaceNames = new String[] { "eden", "s0", "s1", "old", "perm" };
        List<Generation> generations = new ArrayList<Generation>();

        long timestamp = (Long) dbObj.get("timestamp");
        int vmId = (Integer) dbObj.get("vm-id");
        for (String spaceName: spaceNames) {
            DBObject info = (DBObject) dbObj.get(spaceName);
            String generationName = (String) info.get("gen");
            Generation target = null;
            for (Generation generation: generations) {
                if (generation.name.equals(generationName)) {
                    target = generation;
                }
            }
            if (target == null) {
                target = new Generation();
                target.name = generationName;
                generations.add(target);
            }
            if (target.collector == null) {
                target.collector = (String) info.get("collector");
            }
            Space space = new Space();
            space.name = spaceName;
            space.capacity = (Long) info.get("capacity");
            space.maxCapacity = (Long) info.get("max-capacity");
            space.used = (Long) info.get("used");
            if (target.spaces == null) {
                target.spaces = new ArrayList<Space>();
            }
            target.spaces.add(space);
        }

        return new VmMemoryStat(timestamp, vmId, generations);
    }

}
