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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.client.ui.AgentConfigurationView.ConfigurationAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

public class AgentConfigurationController implements ActionListener<ConfigurationAction> {

    private final AgentConfigurationView view;
    private final AgentConfigurationModel model;
    private String agentName = null;

    public AgentConfigurationController(AgentConfigurationModel model, AgentConfigurationView view) {
        this.view = view;
        this.model = model;

        view.addActionListener(this);

    }

    public void showView() {
        Collection<String> agents = model.getAgents();
        agentName = null;
        for (String agentName: agents) {
            if (this.agentName == null) {
                this.agentName = agentName;
            }
            view.addAgent(agentName);
        }
        view.showDialog();
        updateViewFromModel();
    }

    public void hideView() {
        view.hideDialog();
    }

    @Override
    public void actionPerformed(ActionEvent<ConfigurationAction> actionEvent) {
        switch (actionEvent.getActionId()) {
            case SWITCH_AGENT:
                updateModelFromCurrentView();
                String agentName = view.getSelectedAgent();
                this.agentName = agentName;
                updateViewFromModel();
                break;
            case CLOSE_ACCEPT:
                updateModelFromCurrentView();
                model.saveConfiguration();
                /* fall through */
            case CLOSE_CANCEL:
                hideView();
                break;
            default:
                throw new IllegalArgumentException("unknown event");
        }
    }

    private void updateModelFromCurrentView() {
        Map<String, Boolean> map = view.getBackendStatus();
        for (Entry<String, Boolean> entry: map.entrySet()) {
            model.setBackendEnabled(agentName, entry.getKey(), entry.getValue());
        }

    }

    private void updateViewFromModel() {
        Map<String, Boolean> map = new HashMap<>();
        for (String backendName: model.getBackends(agentName)) {
            map.put(backendName, model.getAgentBackendEnabled(agentName, backendName));
        }
        view.setBackendStatus(map);

    }
}
