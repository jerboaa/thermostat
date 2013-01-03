/*
 * Copyright 2013 Red Hat, Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.BackendInformation;

public class BackendInfoDAOImpl implements BackendInfoDAO {

    private final Storage storage;

    public BackendInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(CATEGORY);
    }

    @Override
    public List<BackendInformation> getBackendInformation(HostRef host) {
        // Sort by order value
        Query query = storage.createQuery()
                .from(CATEGORY)
                .where(Key.AGENT_ID, Criteria.EQUALS, host.getAgentId());

        List<BackendInformation> results = new ArrayList<>();
        Cursor<BackendInformation> cursor = storage.findAllPojos(query, BackendInformation.class);
        while (cursor.hasNext()) {
            BackendInformation backendInfo = cursor.next();
            results.add(backendInfo);
        }
        
        // Sort before returning
        Collections.sort(results, new Comparator<BackendInformation>() {

            // TODO Use OrderedComparator when common-core
            // doesn't depend on storage-core
            @Override
            public int compare(BackendInformation o1, BackendInformation o2) {
                int result = o1.getOrderValue() - o2.getOrderValue();
                // Break ties using class name
                if (result == 0) {
                    result = o1.getClass().getName().compareTo(o2.getClass().getName());
                }
                return result;
            }
        });
        
        return results;
    }

    @Override
    public void addBackendInformation(BackendInformation info) {
        storage.putPojo(BackendInfoDAO.CATEGORY, false, info);
    }

    @Override
    public void removeBackendInformation(BackendInformation info) {
        Remove remove = storage.createRemove().from(CATEGORY).where(BACKEND_NAME, info.getName());
        storage.removePojo(remove);
    }

}
