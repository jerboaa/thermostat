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
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Query.Criteria;

class VmInfoDAOImpl implements VmInfoDAO {

    private Storage storage;
    private VmInfoConverter converter;

    VmInfoDAOImpl(Storage storage) {
        this.storage = storage;
        this.converter = new VmInfoConverter();
    }

    @Override
    public VmInfo getVmInfo(VmRef ref) {
        Query findMatchingVm = storage.createQuery()
                .from(vmInfoCategory)
                .where(Key.AGENT_ID, Criteria.EQUALS, ref.getAgent().getAgentId())
                .where(Key.VM_ID, Criteria.EQUALS, ref.getId());
        Chunk result = storage.find(findMatchingVm);
        if (result == null) {
            throw new DAOException("Unknown VM: host:" + ref.getAgent().getAgentId() + ";vm:" + ref.getId());
        }
        return converter.fromChunk(result);
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {

        Query query = buildQuery(host);
        Cursor cursor = storage.findAll(query);
        return buildVMsFromQuery(cursor, host);
    }

    private Query buildQuery(HostRef host) {
        Query query = storage.createQuery()
                .from(vmInfoCategory)
                .where(Key.AGENT_ID, Criteria.EQUALS, host.getAgentId());
        return query;
    }

    private Collection<VmRef> buildVMsFromQuery(Cursor cursor, HostRef host) {
        List<VmRef> vmRefs = new ArrayList<VmRef>();
        while (cursor.hasNext()) {
            Chunk vmChunk = cursor.next();
            VmRef vm = buildVmRefFromChunk(vmChunk, host);
            vmRefs.add(vm);
        }

        return vmRefs;
    }

    private VmRef buildVmRefFromChunk(Chunk vmChunk, HostRef host) {
        Integer id = vmChunk.get(Key.VM_ID);
        // TODO can we do better than the main class?
        String mainClass = vmChunk.get(mainClassKey);
        VmRef ref = new VmRef(host, id, mainClass);
        return ref;
    }

    @Override
    public long getCount() {
        return storage.getCount(vmInfoCategory);
    }

    @Override
    public void putVmInfo(VmInfo info) {
        storage.putPojo(vmInfoCategory, true, info);
    }

    @Override
    public void putVmStoppedTime(int vmId, long timestamp) {
        storage.updateChunk(makeStoppedChunk(vmId, timestamp));
    }

    private Chunk makeStoppedChunk(int vmId, long stopTimeStamp) {
        Chunk chunk = new Chunk(VmInfoDAO.vmInfoCategory, false);
        chunk.put(Key.VM_ID, vmId);
        chunk.put(VmInfoDAO.stopTimeKey, stopTimeStamp);
        return chunk;
    }
}
