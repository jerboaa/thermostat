/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.util.Map;
import java.util.WeakHashMap;

import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatement;

/**
 * A simple implementation of a cache for {@link WebPreparedStatement}
 * in order to avoid unnecessary network round-trips if a statement
 * has already been prepared and it's not out-of-sync with the server.
 */
class WebPreparedStatementCache {
    
    private final Map<StatementDescriptor<?>, WebPreparedStatementHolder> stmtCache;
    private final Map<SharedStateId, StatementDescriptor<?>> reverseLookup;
    
    WebPreparedStatementCache() {
        stmtCache = new WeakHashMap<>();
        reverseLookup = new WeakHashMap<>();
    }

    synchronized <T extends Pojo> void put(StatementDescriptor<T> desc, WebPreparedStatementHolder holder) {
        SharedStateId id = holder.getStatementId();
        stmtCache.put(desc, holder);
        reverseLookup.put(id, desc);
    }
    
    synchronized <T extends Pojo> WebPreparedStatementHolder get(StatementDescriptor<T> desc) {
        return stmtCache.get(desc);
    }
    
    @SuppressWarnings("unchecked")
    synchronized <T extends Pojo> StatementDescriptor<T> get(SharedStateId id) {
        return (StatementDescriptor<T>)reverseLookup.get(id);
    }
    
    synchronized void remove(SharedStateId id) {
        StatementDescriptor<?> desc = reverseLookup.get(id);
        if (desc != null) {
            stmtCache.remove(desc);
        }
        reverseLookup.remove(id);
    }
    
    /**
     * Creates a snapshot of the current state of this cache.
     * 
     * @return A copied snapshot of this cache.
     */
    synchronized WebPreparedStatementCache createSnapshot() {
        WebPreparedStatementCache copy = new WebPreparedStatementCache();
        for (StatementDescriptor<?> desc: stmtCache.keySet()) {
            copy.put(desc, stmtCache.get(desc));
        }
        return copy;
    }

}