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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.model.VmInfo.AliveStatus;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.BytemanInjectState;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.TabbedPaneAction;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;

public class VmBytemanInformationControllerTest {

    private static final String TEMPLATE_STRING = "RULE Thermostat byteman template rule for %s\n" +
                                                  "CLASS %s\n" +
                                                  "METHOD main\n" +
                                                  "HELPER org.jboss.byteman.thermostat.helper.ThermostatHelper\n" +
                                                  "AT ENTRY\n" +
                                                  "IF true\n" +
                                                  "DO\n" +
                                                  "send(\"foo-marker\", \"action\", \"%s.main() called\");\n" +
                                                  "ENDRULE\n";
    private static final String AGENT_ID = "some-agent-id";
    private static final String HOST_NAME = "some-host-name";
    private static final String VM_ID = "some-vm-id";
    
    private VmRef ref;
    private VmBytemanDAO vmBytemanDao;
    private RequestQueue requestQueue;
    
    @After
    public void tearDown() {
        ref = null;
        vmBytemanDao = null;
        requestQueue = null;
    }
    
    @Test
    public void testGenerateTemplateString() {
        String mainClass = "foo-main-class";
        String actual = VmBytemanInformationController.generateTemplateForVM(mainClass);
        String expected = String.format(TEMPLATE_STRING, mainClass, mainClass, mainClass);
        assertEquals(expected, actual);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBasicConstruction() {
        VmBytemanInformationController controller = createController();
        VmBytemanView view = (VmBytemanView)controller.getView();
        verify(view).setViewControlsEnabled(true);
        verify(view).addRuleChangeListener(any(ActionListener.class));
        verify(view).addGenerateActionListener(any(ActionListener.class));
        verify(view).addTabbedPaneChangeListener(any(ActionListener.class));
        verify(view).addActionListener(any(ActionListener.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUnloadRuleSuccess() {
        VmBytemanInformationController controller = createController();
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setListenPort(listenPort);
        when(vmBytemanDao.findBytemanStatus(eq(new VmId(VM_ID)))).thenReturn(status);
        VmBytemanView view = (VmBytemanView)controller.getView();
        
        // call the test method
        controller.unloadRule();
        
        InOrder inOrder = Mockito.inOrder(view);
        inOrder.verify(view).setInjectState(BytemanInjectState.UNLOADING);
        inOrder.verify(view).setViewControlsEnabled(false);
        inOrder.verify(view).setViewControlsEnabled(true);
        inOrder.verify(view).setInjectState(BytemanInjectState.UNLOADED);
        
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestQueue).putRequest(requestCaptor.capture());
        Request req = requestCaptor.getValue();
        String actionStr = req.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction action = RequestAction.fromIntString(actionStr);
        assertEquals(RequestAction.UNLOAD_RULES, action);
        assertEquals(VM_ID, req.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        assertEquals(Integer.toString(listenPort), req.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionEvent> actionEventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(view).contentChanged(actionEventCaptor.capture());
        ActionEvent<TabbedPaneAction> event = actionEventCaptor.getValue();
        String rule = (String)event.getPayload();
        assertEquals(VmBytemanInformationController.NO_RULES_LOADED, rule);
    }
    
    @Test
    public void testLoadRuleSuccessAgentAttached() {
        String ruleContent = "RULE some byteman rule";
        VmBytemanInformationController controller = createController();
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setListenPort(listenPort);
        when(vmBytemanDao.findBytemanStatus(eq(new VmId(VM_ID)))).thenReturn(status);
        VmBytemanView view = (VmBytemanView)controller.getView();
        when(view.getRuleContent()).thenReturn(ruleContent);
        
        // call the test method
        controller.loadRule();
        
        InOrder inOrder = Mockito.inOrder(view);
        inOrder.verify(view).setInjectState(BytemanInjectState.INJECTING);
        inOrder.verify(view).setViewControlsEnabled(false);
        inOrder.verify(view).setViewControlsEnabled(true);
        inOrder.verify(view).setInjectState(BytemanInjectState.INJECTED);
        
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestQueue).putRequest(requestCaptor.capture());
        Request req = requestCaptor.getValue();
        String actionStr = req.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction action = RequestAction.fromIntString(actionStr);
        assertEquals(RequestAction.LOAD_RULES, action);
        assertEquals(VM_ID, req.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        assertEquals(Integer.toString(listenPort), req.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        assertEquals(ruleContent, req.getParameter(BytemanRequest.RULE_PARAM_NAME));
    }
    
    @Test
    public void testLoadRuleSuccessAgentNotAttached() {
        String ruleContent = "RULE some byteman rule";
        VmBytemanInformationController controller = createController();
        // mimic no-agent-attached, by returning a null status
        when(vmBytemanDao.findBytemanStatus(eq(new VmId(VM_ID)))).thenReturn(null);
        VmBytemanView view = (VmBytemanView)controller.getView();
        when(view.getRuleContent()).thenReturn(ruleContent);
        
        // call the test method
        controller.loadRule();
        
        InOrder inOrder = Mockito.inOrder(view);
        inOrder.verify(view).setInjectState(BytemanInjectState.INJECTING);
        inOrder.verify(view).setViewControlsEnabled(false);
        inOrder.verify(view).setViewControlsEnabled(true);
        inOrder.verify(view).setInjectState(BytemanInjectState.INJECTED);
        
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestQueue).putRequest(requestCaptor.capture());
        Request req = requestCaptor.getValue();
        String actionStr = req.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction action = RequestAction.fromIntString(actionStr);
        assertEquals(RequestAction.LOAD_RULES, action);
        assertEquals(VM_ID, req.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        assertEquals(Integer.toString(BytemanRequest.NOT_ATTACHED_PORT), req.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        assertEquals(ruleContent, req.getParameter(BytemanRequest.RULE_PARAM_NAME));
    }
    
    @Test
    public void testLoadRuleEmpty() {
        VmBytemanInformationController controller = createController();
        VmBytemanView view = (VmBytemanView)controller.getView();
        when(view.getRuleContent()).thenReturn(VmBytemanInformationController.NO_RULES_LOADED);
        controller.loadRule();
        ArgumentCaptor<LocalizedString> msgCaptor = ArgumentCaptor.forClass(LocalizedString.class);
        verify(view).handleError(msgCaptor.capture());
        LocalizedString msg = msgCaptor.getValue();
        assertEquals("Rule to inject is empty.", msg.getContents());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateRule() {
        String testRule = "Some rule for this test";
        VmBytemanInformationController controller = createController();
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setListenPort(listenPort);
        status.setRule(testRule);
        VmBytemanView view = (VmBytemanView)controller.getView();
        
        // call the test method
        controller.updateRule(status);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionEvent> actionEventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(view).contentChanged(actionEventCaptor.capture());
        ActionEvent<TabbedPaneAction> event = actionEventCaptor.getValue();
        String rule = (String)event.getPayload();
        assertEquals(testRule, rule);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateRuleNullStatus() {
        VmBytemanInformationController controller = createController();
        VmBytemanView view = (VmBytemanView)controller.getView();
        
        // call the test method
        controller.updateRule(null);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionEvent> actionEventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(view).contentChanged(actionEventCaptor.capture());
        ActionEvent<TabbedPaneAction> event = actionEventCaptor.getValue();
        String rule = (String)event.getPayload();
        assertEquals(VmBytemanInformationController.NO_RULES_LOADED, rule);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateMetrics() {
        VmBytemanInformationController controller = createController();
        VmBytemanView view = (VmBytemanView)controller.getView();
        BytemanMetric metric = new BytemanMetric();
        metric.setVmId(VM_ID);
        metric.setAgentId(AGENT_ID);
        List<BytemanMetric> customList = Arrays.asList(metric);
        when(vmBytemanDao.findBytemanMetrics(any(Range.class), any(VmId.class), any(AgentId.class))).thenReturn(customList);
        
        controller.updateMetrics();
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionEvent> actionEventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(view).contentChanged(actionEventCaptor.capture());
        ActionEvent<TabbedPaneAction> event = actionEventCaptor.getValue();
        List<BytemanMetric> metrics = (List<BytemanMetric>)event.getPayload();
        assertEquals(1, metrics.size());
        assertEquals(customList, metrics);
        BytemanMetric m = metrics.get(0);
        assertEquals(VM_ID, m.getVmId());
    }

    @Test
    public void testPollingState() {
        VmBytemanInformationController controller = Mockito.spy(createController());
        VmBytemanView view = (VmBytemanView)controller.getView();
        List<BytemanMetric> metricsList = new ArrayList<>();
        String content = "{ \"foo\": \"bar\" }";
        String marker = "marker";
        long timestamp = System.currentTimeMillis();
        metricsList.add(createMetric(content, marker, timestamp));
        doNothing().when(controller).startPolling();
        doNothing().when(controller).stopPolling();

        // after successful passing of metrics, controller should be polling
        when(view.getInjectState()).thenReturn(BytemanInjectState.INJECTED);
        when(vmBytemanDao.findBytemanMetrics(any(Range.class), any(VmId.class), any(AgentId.class))).thenReturn(metricsList);
        controller.updateMetrics();
        Mockito.verify(controller).startPolling();

        // if no new metrics & the byteman state is unloaded, controller should not be polling
        when(view.getInjectState()).thenReturn(BytemanInjectState.UNLOADED);
        List<BytemanMetric> emptyList = new ArrayList<BytemanMetric>();
        when(vmBytemanDao.findBytemanMetrics(any(Range.class), any(VmId.class), any(AgentId.class))).thenReturn(emptyList);
        controller.updateMetrics();
        Mockito.verify(controller).stopPolling();

        // new metrics should resume polling
        when(view.getInjectState()).thenReturn(BytemanInjectState.INJECTED);
        metricsList = new ArrayList<>();
        timestamp = System.currentTimeMillis();
        metricsList.add(createMetric(content, marker, timestamp));
        when(vmBytemanDao.findBytemanMetrics(any(Range.class), any(VmId.class), any(AgentId.class))).thenReturn(metricsList);
        controller.updateMetrics();
        Mockito.verify(controller, times(2)).startPolling();
    }

    private BytemanMetric createMetric(String content, String marker, long timestamp) {
        BytemanMetric m = new BytemanMetric();
        m.setData(content);
        m.setMarker(marker);
        m.setTimeStamp(timestamp);
        return m;
    }

    private VmBytemanInformationController createController() {
        VmBytemanView view = mock(VmBytemanView.class);
        ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn(VM_ID);
        when(ref.getHostRef()).thenReturn(new HostRef(AGENT_ID, HOST_NAME));
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.isAlive()).thenReturn(true);
        when(agentInfoDao.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive(agentInfo)).thenReturn(AliveStatus.RUNNING);
        when(vmInfoDao.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);
        vmBytemanDao = mock(VmBytemanDAO.class);
        requestQueue = mock(RequestQueue.class);
        return new VmBytemanInformationController(view, ref, agentInfoDao, vmInfoDao, vmBytemanDao, requestQueue) {
            
            @Override
            void waitWithTimeOut(CountDownLatch latch) {
                // nothing, return immediately for tests
            }
        };
        
    }
}
