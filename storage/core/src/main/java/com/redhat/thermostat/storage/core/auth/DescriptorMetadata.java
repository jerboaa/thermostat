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

package com.redhat.thermostat.storage.core.auth;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;

/**
 * Data describing a statement descriptor.
 *
 * @see StatementDescriptor
 */
public final class DescriptorMetadata {

    private final String agentId;
    private final String vmId;
    
    /**
     * Constructor.
     * 
     * @param agentId A non-null string representation of the agent UUID.
     * @param vmId A non-null string representation of the vm UUID.
     */
    public DescriptorMetadata(String agentId, String vmId) {
        this.agentId = agentId;
        this.vmId = vmId;
    }
    
    /**
     * Constructor, setting vmId null.
     * 
     * @param agentId A non-null string representation of the agent UUID.
     */
    public DescriptorMetadata(String agentId) {
        this(agentId, null);
    }
    
    /**
     * Constructor, setting agentId and vmId null.
     */
    public DescriptorMetadata() {
        this(null, null);
    }

    /**
     * @return The value of the {@link Key#AGENT_ID} parameter which was used to
     *         complete the query or null if there was none.
     */
    public final String getAgentId() {
        return agentId; 
    }
    
    /**
     * @return The value of the {@link Key#VM_ID} parameter which was used to
     *         complete the query or null if there was none.
     */
    public final String getVmId() {
        return vmId;
    }
    
    /**
     * 
     * @return true if there is a {@link Key#AGENT_ID} parameter, false otherwise.
     */
    public final boolean hasAgentId() {
        return agentId != null;
    }
    
    /**
     * 
     * @return true if there is a {@link Key#VM_ID} parameter, false otherwise.
     */
    public final boolean hasVmId() {
        return vmId != null;
    }
    
}

