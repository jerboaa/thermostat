/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.core.views;

import java.util.Map;

import com.redhat.thermostat.common.ActionListener;

/**
 * A view that displays an agent's information
 */
public abstract class AgentInformationDisplayView extends BasicView implements UIComponent {

    public enum ConfigurationAction {
        SWITCH_AGENT,
        SHOW_BACKEND_DESCRIPTION,
        CLOSE,
    }

    public abstract void showDialog();

    public abstract void hideDialog();

    public abstract void addConfigurationListener(ActionListener<ConfigurationAction> listener);

    public abstract void removeConfigurationListener(ActionListener<ConfigurationAction> listener);

    public abstract void addAgent(String agentName);

    public abstract String getSelectedAgent();

    public abstract void clearAllAgents();

    public abstract void setSelectedAgentName(String agentName);
    public abstract void setSelectedAgentId(String agentId);
    public abstract void setSelectedAgentCommandAddress(String address);
    public abstract void setSelectedAgentStartTime(String startTime);
    public abstract void setSelectedAgentStopTime(String stopTime);

    public abstract void setSelectedAgentBackendStatus(Map<String, String> agentConfiguration);
    public abstract void setSelectedAgentBackendDescription(String description);

}

