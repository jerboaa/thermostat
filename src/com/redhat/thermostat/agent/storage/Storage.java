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

package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;

public abstract class Storage {
    private Map<String, Backend> categoryMap;

    public Storage() {
        categoryMap = new HashMap<String, Backend>();
    }

    public abstract void connect(String uri) throws UnknownHostException;

    public abstract void setAgentId(UUID id);

    public abstract void addAgentInformation(StartupConfiguration config, BackendRegistry registry);

    public abstract void removeAgentInformation();

    /**
     * @return {@code null} if the value is invalid or missing
     */
    public abstract String getBackendConfig(String backendName, String configurationKey);

    public final void registerCategory(Category category, Backend backend) {
        if (categoryMap.containsKey(category.getName())) {
            throw new IllegalStateException("Category may only be associated with one backend.");
        }
        categoryMap.put(category.getName(), backend);
    }

    public final void putChunk(Chunk chunk, Backend backend) {
        validateChunkOrigin(chunk, backend);
        putChunkImpl(chunk);
    }

    public final void updateChunk(Chunk chunk, Backend backend) {
        validateChunkOrigin(chunk, backend);
        updateChunkImpl(chunk);
    }

    private void validateChunkOrigin(Chunk chunk, Backend origin) {
        Category category = chunk.getCategory();
        if (origin != categoryMap.get(category.getName())) { // This had better be not just equivalent, but actually the same object.
            throw new IllegalArgumentException("Invalid category-backend combination while inserting data.  Category: " + category.getName() + "  Backend: " + origin.getName());
        }
    }
    
    protected abstract void putChunkImpl(Chunk chunk);

    protected abstract void updateChunkImpl(Chunk chunk);

    /* Drop all data related to the currently running agent.
     */
    public abstract void purge();

}
