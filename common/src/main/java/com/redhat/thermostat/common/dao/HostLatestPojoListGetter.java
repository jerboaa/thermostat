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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.common.model.TimeStampedPojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

class HostLatestPojoListGetter<T extends TimeStampedPojo> implements LatestPojoListGetter<T> {

    private Storage storage;
    private Category cat;
    private Converter<T> converter;
    private HostRef ref;

    private Map<HostRef, Long> lastUpdateTimes = new HashMap<>();

    HostLatestPojoListGetter(Storage storage, Category cat, Converter<T> converter, HostRef ref) {
        this.storage = storage;
        this.cat = cat;
        this.converter = converter;
        this.ref = ref;
    }

    @Override
    public List<T> getLatest() {
        Chunk query = buildQuery();
        return getLatest(query);
    }

    private List<T> getLatest(Chunk query) {
        // TODO if multiple threads will be using this utility class, there may be some issues
        // with the updateTimes
        Long lastUpdate = lastUpdateTimes.get(ref);
        Cursor cursor = storage.findAll(query);
        List<T> result = new ArrayList<>();
        while (cursor.hasNext()) {
            Chunk chunk = cursor.next();
            T pojo = converter.fromChunk(chunk);
            result.add(pojo);
            lastUpdateTimes.put(ref, Math.max(pojo.getTimeStamp(), lastUpdate));
        }
        return result;
    }

    protected Chunk buildQuery() {
        Chunk query = new Chunk(cat, false);
        query.put(Key.AGENT_ID, ref.getAgentId());
        Long lastUpdate = lastUpdateTimes.get(ref);
        if (lastUpdate != null) {
            // TODO once we have an index and the 'column' is of type long, use
            // a query which can utilize an index. this one doesn't
            query.put(Key.WHERE, "this.timestamp > " + lastUpdate);
        } else {
            lastUpdateTimes.put(ref, Long.MIN_VALUE);
        }
        return query;
    }
}
