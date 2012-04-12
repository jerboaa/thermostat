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

package com.redhat.thermostat.common.storage;

import java.util.UUID;


public abstract class Storage {

    public abstract void setAgentId(UUID id);

    public final void registerCategory(Category category) {
        if (category.hasBeenRegistered()) {
            throw new IllegalStateException("Category may only be associated with one backend.");
        }
        ConnectionKey connKey = createConnectionKey(category);
        category.setConnectionKey(connKey);
    }

    public abstract Connection getConnection();

    public abstract ConnectionKey createConnectionKey(Category category);

    public abstract void putChunk(Chunk chunk);

    public abstract void updateChunk(Chunk chunk);

    /**
     * Drop all data related to the currently running agent.
     */
    public abstract void purge();
    
    public abstract Cursor findAll(Chunk query);

    public abstract Chunk find(Chunk query);

    public abstract Cursor findAllFromCategory(Category category);

    public abstract long getCount(Category category);

    // TODO these will move to appropriate DAO
    public abstract void addAgentInformation(AgentInformation agentInfo);

    public abstract void removeAgentInformation();

    public abstract void updateAgentInformation(AgentInformation agentInfo);

    /**
     * @return {@code null} if the value is invalid or missing
     */
    public abstract String getBackendConfig(String backendName, String configurationKey);

}
