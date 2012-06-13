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

package com.redhat.thermostat.client.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.client.internal.AgentConfigurationSource;

/**
 * This model sits between the current view and the remote model, and allows
 * us to make changes and later throw them away.
 */
public class AgentConfigurationModel {

    private final AgentConfigurationSource remoteConfiguration;

    private final List<String> knownAgents;
    private Map<String, Map<String, Boolean>> enabledBackends;

    public AgentConfigurationModel(AgentConfigurationSource configSource) {
        this.remoteConfiguration = configSource;

        knownAgents = new ArrayList<>(remoteConfiguration.getKnownAgents());
        enabledBackends = new HashMap<>();
        for (String agent: knownAgents) {
            enabledBackends.put(agent, new HashMap<String, Boolean>(remoteConfiguration.getAgentBackends(agent)));
        }
    }

    public Collection<String> getAgents() {
        return Collections.unmodifiableList(knownAgents);
    }

    public Collection<String> getBackends(String agentName) {
        return Collections.unmodifiableSet(enabledBackends.get(agentName).keySet());
    }

    public void setBackendEnabled(String agentName, String backendName, boolean enabled) {
        enabledBackends.get(agentName).put(backendName, enabled);
    }

    public boolean getAgentBackendEnabled(String agentName, String backendName) {
        return enabledBackends.get(agentName).get(backendName);
    }

    public void saveConfiguration() {
        for (Entry<String, Map<String, Boolean>> entry: enabledBackends.entrySet()) {
            remoteConfiguration.updateAgentConfig(entry.getKey(), entry.getValue());
        }
    }

}
