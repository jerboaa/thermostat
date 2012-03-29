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

import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Storage;

class VmRefDAOImpl implements VmRefDAO {

    private Storage storage;

    VmRefDAOImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {

        Chunk query = buildQuery(host);
        Cursor cursor = storage.findAll(query);
        return buildVMsFromQuery(cursor, host);
    }

    private Chunk buildQuery(HostRef host) {
        Chunk query = new Chunk(VmInfoDAO.vmInfoCategory, false);
        query.put(HostRefDAO.agentIdKey, host.getAgentId());
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
        Integer id = vmChunk.get(VmInfoDAO.vmIdKey);
        // TODO can we do better than the main class?
        String mainClass = vmChunk.get(VmInfoDAO.mainClassKey);
        VmRef ref = new VmRef(host, id, mainClass);
        return ref;
    }
}
