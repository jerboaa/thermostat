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

import com.mongodb.DBObject;
import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmGcStatConverter {

    public Chunk vmGcStatToChunk(VmGcStat vmGcStat) {
        Chunk chunk = new Chunk(VmGcStatDAO.vmGcStatsCategory, false);

        chunk.put(VmGcStatDAO.vmGcStatVmIdKey, vmGcStat.getVmId());
        chunk.put(Key.TIMESTAMP, vmGcStat.getTimeStamp());
        chunk.put(VmGcStatDAO.vmGcStatCollectorKey, vmGcStat.getCollectorName());
        chunk.put(VmGcStatDAO.vmGcStatRunCountKey, vmGcStat.getRunCount());
        chunk.put(VmGcStatDAO.vmGCstatWallTimeKey, vmGcStat.getWallTime());

        return chunk;
    }

    public VmGcStat fromDBObject(DBObject dbObj) {
        int vmId = (Integer) dbObj.get("vm-id");
        String collectorName = (String) dbObj.get("collector");
        long timestamp = (Long) dbObj.get("timestamp");
        long runCount = (Long) dbObj.get("runtime-count");
        long wallTime = (Long) dbObj.get("wall-time");

        return new VmGcStat(vmId, timestamp, collectorName, runCount, wallTime);
    }
}
