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

import com.redhat.thermostat.common.VmCpuStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class VmCpuStatDAOImpl extends VmCpuStatDAO {

    private final Storage storage;
    private final VmRef vmRef;

    private long lastUpdate = Long.MIN_VALUE;;

    public VmCpuStatDAOImpl(Storage storage, VmRef vmRef) {
        this.storage = storage;
        this.vmRef = vmRef;
    }

    @Override
    public List<VmCpuStat> getLatestVmCpuStats() {
        ArrayList<VmCpuStat> result = new ArrayList<>();
        Chunk query = new Chunk(VmCpuStatDAO.vmCpuStatCategory, false);
        query.put(Key.AGENT_ID, vmRef.getAgent().getAgentId());
        query.put(VmCpuStatDAO.vmIdKey, Integer.valueOf(vmRef.getId()));
        if (lastUpdate != Long.MIN_VALUE) {
            // TODO once we have an index and the 'column' is of type long, use
            // a query which can utilize an index. this one doesn't
            query.put(Key.WHERE, "this.timestamp > " + lastUpdate);
        }
        Cursor cursor = storage.findAll(query);
        while (cursor.hasNext()) {
            Chunk current = cursor.next();
            VmCpuStat stat = new VmCpuStatConverter().chunkToVmCpuStat(current);
            result.add(stat);
            lastUpdate = Math.max(stat.getTimeStamp(), lastUpdate);
        }

        return result;

    }
}
