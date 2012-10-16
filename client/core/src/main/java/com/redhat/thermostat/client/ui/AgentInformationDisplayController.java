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

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.client.core.views.AgentInformationDisplayView;
import com.redhat.thermostat.client.core.views.AgentInformationDisplayView.ConfigurationAction;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.BackendInformation;

public class AgentInformationDisplayController implements ActionListener<ConfigurationAction> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final AgentInformationDisplayView view;
    private final AgentInformationDisplayModel model;

    private final DateFormat dateTimeFormat;

    public AgentInformationDisplayController(AgentInformationDisplayModel model, AgentInformationDisplayView view) {
        this.view = view;
        this.model = model;

        view.addConfigurationListener(this);

        dateTimeFormat = DateFormat.getDateTimeInstance();
    }

    public void showView() {
        Collection<AgentInformation> agents = model.getAgents();
        String agentId = null;
        for (AgentInformation agentInfo : agents) {
            String agentName = agentInfo.getAgentId();
            if (agentId == null) {
                agentId = agentInfo.getAgentId();
            }
            view.addAgent(agentName);
        }
        view.showDialog();

        if (agentId != null) {
            updateViewFromModel(agentId);
        }
    }

    public void hideView() {
        view.hideDialog();
    }

    @Override
    public void actionPerformed(ActionEvent<ConfigurationAction> actionEvent) {
        String agentId = view.getSelectedAgent();
        switch (actionEvent.getActionId()) {
        case SWITCH_AGENT:
            updateViewFromModel(agentId);
            break;
        case SHOW_BACKEND_DESCRIPTION:
            String backendName = (String) actionEvent.getPayload();
            view.setSelectedAgentBackendDescription(getBackendDescription(agentId, backendName));
            break;
        case CLOSE:
            hideView();
            break;
        default:
            throw new IllegalArgumentException("unknown event");
        }
    }

    private String getBackendDescription(String agentId, String backendName) {
        return getBackendInformation(agentId, backendName).getDescription();
    }

    private BackendInformation getBackendInformation(String agentId, String backendName) {
        Collection<BackendInformation> backendInfos = model.getBackends(agentId);
        for (BackendInformation backendInfo : backendInfos) {
            if (backendInfo.getName().equals(backendName)) {
                return backendInfo;
            }
        }
        return null;
    }

    private void updateViewFromModel(String agentId) {
        AgentInformation agentInfo = model.getAgentInfo(agentId);
        view.setSelectedAgentName(agentInfo.getAgentId());
        view.setSelectedAgentId(agentInfo.getAgentId());
        view.setSelectedAgentCommandAddress(agentInfo.getConfigListenAddress());
        long startTime = agentInfo.getStartTime();
        view.setSelectedAgentStartTime(dateTimeFormat.format(new Date(startTime)));
        long stopTime = agentInfo.getStopTime();
        if (stopTime >= startTime) {
            view.setSelectedAgentStopTime(dateTimeFormat.format(new Date(stopTime)));
        } else {
            view.setSelectedAgentStopTime(translator.localize(LocaleResources.AGENT_INFO_AGENT_RUNNING));
        }

        Map<String, String> map = new HashMap<>();
        for (BackendInformation backendInfo : model.getBackends(agentId)) {
            String status = backendInfo.isActive() ?
                    translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_ACTIVE)
                    : translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_INACTIVE);
            map.put(backendInfo.getName(), status);
        }
        view.setSelectedAgentBackendStatus(map);
    }

}
