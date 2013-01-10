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

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.VmInfo;

class VmInfoDAOImpl implements VmInfoDAO {

    private final Storage storage;

    VmInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmInfoCategory);
    }

    @Override
    public VmInfo getVmInfo(VmRef ref) {
        Query<VmInfo> findMatchingVm = storage.createQuery(vmInfoCategory, VmInfo.class);
        findMatchingVm.where(Key.AGENT_ID, Criteria.EQUALS, ref.getAgent().getAgentId());
        findMatchingVm.where(Key.VM_ID, Criteria.EQUALS, ref.getId());
        findMatchingVm.limit(1);
        VmInfo result = findMatchingVm.execute().next();
        if (result == null) {
            throw new DAOException("Unknown VM: host:" + ref.getAgent().getAgentId() + ";vm:" + ref.getId());
        }
        return result;
    }

    @Override
    public Collection<VmRef> getVMs(HostRef host) {

        Query<VmInfo> query = buildQuery(host);
        Cursor<VmInfo> cursor = query.execute();
        return buildVMsFromQuery(cursor, host);
    }

    private Query<VmInfo> buildQuery(HostRef host) {
        Query<VmInfo> query = storage.createQuery(vmInfoCategory, VmInfo.class);
        query.where(Key.AGENT_ID, Criteria.EQUALS, host.getAgentId());
        return query;
    }

    private Collection<VmRef> buildVMsFromQuery(Cursor<VmInfo> cursor, HostRef host) {
        List<VmRef> vmRefs = new ArrayList<VmRef>();
        while (cursor.hasNext()) {
            VmInfo vmInfo = cursor.next();
            VmRef vm = buildVmRefFromChunk(vmInfo, host);
            vmRefs.add(vm);
        }

        return vmRefs;
    }

    private VmRef buildVmRefFromChunk(VmInfo vmInfo, HostRef host) {
        Integer id = vmInfo.getVmId();
        // TODO can we do better than the main class?
        String mainClass = vmInfo.getMainClass();
        VmRef ref = new VmRef(host, id, mainClass);
        return ref;
    }

    @Override
    public long getCount() {
        return storage.getCount(vmInfoCategory);
    }

    @Override
    public void putVmInfo(VmInfo info) {
        Put replace = storage.createReplace(vmInfoCategory);
        replace.setPojo(info);
        replace.apply();
    }

    @Override
    public void putVmStoppedTime(int vmId, long timestamp) {
        Update update = storage.createUpdate(vmInfoCategory);
        update.where(Key.VM_ID, vmId);
        update.set(VmInfoDAO.stopTimeKey, timestamp);
        update.apply();
    }

}
