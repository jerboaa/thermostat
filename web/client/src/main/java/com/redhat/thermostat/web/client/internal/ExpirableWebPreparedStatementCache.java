/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.web.client.internal;

import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;

/**
 * A {@link WebPreparedStatementCache} with an expiration time. When that time
 * has passed it will return null for all cache entries. Used as a read-only
 * transition cache in {@link WebStorage} in order to recover from server
 * component reloads.
 *
 */
class ExpirableWebPreparedStatementCache extends WebPreparedStatementCache {

    private final long timeExpires;
    private final WebPreparedStatementCache cache;
    
    ExpirableWebPreparedStatementCache(WebPreparedStatementCache cache, long timeExpires) {
        this.timeExpires = timeExpires;
        this.cache = cache;
    }
    
    @Override
    synchronized <T extends Pojo> WebPreparedStatementHolder get(StatementDescriptor<T> desc) {
        WebPreparedStatementHolder holder = cache.get(desc);
        if (holder == null) {
            return null;
        }
        // check if corresponding cache entry has expired
        long now = System.nanoTime();
        if (now > timeExpires) {
            // remove cache entry and return null
            SharedStateId id = holder.getStatementId();
            cache.remove(id);
            return null;
        }
        return holder;
    }
    
    @Override
    synchronized <T extends Pojo> StatementDescriptor<T> get(SharedStateId id) {
        StatementDescriptor<T> desc = cache.get(id);
        if (desc == null) {
            return null;
        }
        long now = System.nanoTime();
        if (now > timeExpires) {
            // remove cache entry and return null
            cache.remove(id);
            return null;
        }
        return desc;
    }
    
    boolean isExpired() {
        return System.nanoTime() > timeExpires;
    }
    
}
