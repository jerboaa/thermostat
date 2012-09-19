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

import com.redhat.thermostat.common.model.TimeStampedPojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;

class HostLatestPojoListGetter<T extends TimeStampedPojo> implements LatestPojoListGetter<T> {

    private Storage storage;
    private Category cat;
    private HostRef ref;
    private Class<T> resultClass;

    HostLatestPojoListGetter(Storage storage, Category cat, HostRef ref, Class<T> resultClass) {
        this.storage = storage;
        this.cat = cat;
        this.ref = ref;
        this.resultClass = resultClass;
    }

    @Override
    public List<T> getLatest(long since) {
        Query query = buildQuery(since);
        return getLatest(query);
    }

    private List<T> getLatest(Query query) {
        Cursor<T> cursor = storage.findAllPojos(query, resultClass);
        List<T> result = new ArrayList<>();
        while (cursor.hasNext()) {
            T pojo = cursor.next();
            result.add(pojo);
        }
        return result;
    }

    protected Query buildQuery(long since) {
        Query query = storage.createQuery()
                .from(cat)
                .where(Key.AGENT_ID, Criteria.EQUALS, ref.getAgentId())
                .where(Key.TIMESTAMP, Criteria.GREATER_THAN, since)
                .sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        
        return query;
    }
}
