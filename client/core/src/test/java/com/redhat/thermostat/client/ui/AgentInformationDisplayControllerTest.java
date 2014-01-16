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

package com.redhat.thermostat.client.ui;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.AgentInformationDisplayView;
import com.redhat.thermostat.client.core.views.AgentInformationDisplayView.ConfigurationAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;

public class AgentInformationDisplayControllerTest {

    private AgentInformation agentInfo1;
    private BackendInformation backendInfo1;

    @Before
    public void setUp() {
        setUpAgentAndBackendInformation();
    }

    private void setUpAgentAndBackendInformation() {
        agentInfo1 = new AgentInformation("agent-1");

        backendInfo1 = new BackendInformation("agent-1");
        backendInfo1.setName("backend-1");
        backendInfo1.setDescription("backend-description1");
    }

    @Test
    public void testShowView() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        when(model.getAgents()).thenReturn(Arrays.asList(agentInfo1));
        when(model.getAgentInfo(agentInfo1.getAgentId())).thenReturn(agentInfo1);

        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);
        controller.showView();

        verify(view).showDialog();
    }

    @Test
    public void testHideView() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);
        controller.hideView();

        verify(view).hideDialog();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHidViewWhenViewFiresClose() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);

        @SuppressWarnings("unused")
        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);

        verify(view).addConfigurationListener(captor.capture());
        ActionListener<ConfigurationAction> listener = captor.getValue();
        listener.actionPerformed(new ActionEvent<ConfigurationAction>(view, ConfigurationAction.CLOSE));

        verify(view).hideDialog();
    }

    @Test
    public void testDisplayWithNoAgents() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        when(model.getAgents()).thenReturn(Arrays.<AgentInformation>asList());

        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);
        controller.showView();

        verify(view).showDialog();
    }

    @Test
    public void testAddAgentAndBackendsOnInit() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        when(model.getAgents()).thenReturn(Arrays.asList(agentInfo1));
        when(model.getAgentInfo(agentInfo1.getAgentId())).thenReturn(agentInfo1);
        when(model.getBackends(agentInfo1.getAgentId())).thenReturn(Arrays.asList(backendInfo1));

        Map<String, String> expected = new HashMap<>();
        expected.put(backendInfo1.getName(), backendInfo1.isActive() ? "Active" : "Inactive");

        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);
        controller.showView();

        verify(view).addAgent(agentInfo1.getAgentId());

        verify(view).setSelectedAgentName(agentInfo1.getAgentId());
        verify(view).setSelectedAgentId(agentInfo1.getAgentId());
        verify(view).setSelectedAgentCommandAddress(agentInfo1.getConfigListenAddress());
        verify(view).setSelectedAgentStartTime(isA(String.class));
        verify(view).setSelectedAgentStopTime(isA(String.class));

        verify(view).setSelectedAgentBackendStatus(eq(expected));
        verify(view).showDialog();
    }

    @Test
    public void testUpdateAgentInformationOnAgentSelection() {
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);

        when(model.getAgents()).thenReturn(Arrays.asList(agentInfo1));
        when(model.getAgentInfo(agentInfo1.getAgentId())).thenReturn(agentInfo1);
        when(model.getBackends(agentInfo1.getAgentId())).thenReturn(Arrays.asList(backendInfo1));

        when(view.getSelectedAgent()).thenReturn(agentInfo1.getAgentId());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);

        @SuppressWarnings("unused")
        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);

        verify(view).addConfigurationListener(captor.capture());
        ActionListener<ConfigurationAction> listener = captor.getValue();
        listener.actionPerformed(new ActionEvent<ConfigurationAction>(view, ConfigurationAction.SWITCH_AGENT));

        verify(view, never()).addAgent(agentInfo1.getAgentId());

        verify(view).setSelectedAgentName(agentInfo1.getAgentId());
        verify(view).setSelectedAgentId(agentInfo1.getAgentId());
        verify(view).setSelectedAgentCommandAddress(agentInfo1.getConfigListenAddress());
        verify(view).setSelectedAgentStartTime(isA(String.class));
        verify(view).setSelectedAgentStopTime(isA(String.class));

        Map<String, String> expected = new HashMap<>();
        expected.put(backendInfo1.getName(), backendInfo1.isActive() ? "Active" : "Inactive");

        verify(view).setSelectedAgentBackendStatus(eq(expected));
    }

    @Test
    public void testUpdateDescriptionOnRequest() {
        AgentInformationDisplayModel model = mock(AgentInformationDisplayModel.class);
        AgentInformationDisplayView view = mock(AgentInformationDisplayView.class);

        when(model.getAgents()).thenReturn(Arrays.asList(agentInfo1));
        when(model.getAgentInfo(agentInfo1.getAgentId())).thenReturn(agentInfo1);
        when(model.getBackends(agentInfo1.getAgentId())).thenReturn(Arrays.asList(backendInfo1));

        when(view.getSelectedAgent()).thenReturn(agentInfo1.getAgentId());

        Map<String, String> expected = new HashMap<>();
        expected.put(backendInfo1.getName(), backendInfo1.isActive() ? "Active" : "Inactive");

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);

        verify(view).addConfigurationListener(listenerCaptor.capture());

        ActionEvent<ConfigurationAction> showBackendDescription = new ActionEvent<>(view, ConfigurationAction.SHOW_BACKEND_DESCRIPTION);
        showBackendDescription.setPayload(backendInfo1.getName());

        listenerCaptor.getValue().actionPerformed(showBackendDescription);

        verify(view).setSelectedAgentBackendDescription(backendInfo1.getDescription());
    }

}

