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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.AgentConfigurationSource;
import com.redhat.thermostat.client.ui.AgentConfigurationView.ConfigurationAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;

public class AgentConfigurationControllerTest {

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        /*
         * Set up a mock timer factory that always executes actions synchronously on start();
         */
        TimerFactory tf = mock(TimerFactory.class);
        Timer timer = mock(Timer.class);
        final Runnable[] runnable = new Runnable[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                runnable[0] = (Runnable) invocation.getArguments()[0];
                return null;
            }

        }).when(timer).setAction(any(Runnable.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                runnable[0].run();
                return null;
            }

        }).when(timer).start();
        when(tf.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(tf);

    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testAddingEnabledBackends() {
        AgentConfigurationView view = mock(AgentConfigurationView.class);
        AgentConfigurationModel model = mock(AgentConfigurationModel.class);
        when(model.getAgents()).thenReturn(Arrays.asList(new String[]{"agent1"}));
        List<String> backends = Arrays.asList(new String[] {"backend1", "backend2"});
        when(model.getBackends(any(String.class))).thenReturn(backends);
        when(model.getAgentBackendEnabled(any(String.class), any(String.class))).thenReturn(true);

        Map<String,Boolean> expected = new HashMap<>();
        expected.put("backend1", true);
        expected.put("backend2", true);

        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();
        controller.hideView();

        verify(view).addAgent("agent1");
        verify(view).setBackendStatus(eq(expected));
        verify(view).showDialog();
        verify(view).hideDialog();
    }

    @Test
    public void testAddingDisabledBackends() {
        AgentConfigurationView view = mock(AgentConfigurationView.class);
        AgentConfigurationModel model = mock(AgentConfigurationModel.class);
        when(model.getAgents()).thenReturn(Arrays.asList(new String[]{"agent1"}));
        List<String> backends = Arrays.asList(new String[] {"backend1",});
        when(model.getBackends(any(String.class))).thenReturn(backends);
        when(model.getAgentBackendEnabled(any(String.class), any(String.class))).thenReturn(false);

        Map<String,Boolean> expected = new HashMap<>();
        expected.put("backend1", false);

        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();
        controller.hideView();

        verify(view).addAgent("agent1");
        verify(view).setBackendStatus(eq(expected));
        verify(view).showDialog();
        verify(view).hideDialog();
    }

    /**
     * Verify that the accepting the changes signals the controller
     */
    @Test
    public void testViewEditedBackends() {
        final ActionListener<ConfigurationAction>[] listeners = (ActionListener<ConfigurationAction>[]) new ActionListener<?>[1];
        AgentConfigurationView view = mock(AgentConfigurationView.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                listeners[0] = (ActionListener<ConfigurationAction>) invocation.getArguments()[0];
                return null;
            }
        }).when(view).addActionListener(any(ActionListener.class));


        AgentConfigurationModel model = mock(AgentConfigurationModel.class);
        when(model.getAgents()).thenReturn(Arrays.asList(new String[]{"agent1"}));
        List<String> backends = Arrays.asList(new String[] {"backend1"});
        when(model.getBackends(any(String.class))).thenReturn(backends);
        when(model.getAgentBackendEnabled(any(String.class), any(String.class))).thenReturn(true);

        Map<String,Boolean> expected = new HashMap<>();
        expected.put("backend1", true);

        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();

        listeners[0].actionPerformed(new ActionEvent<ConfigurationAction>(view, ConfigurationAction.CLOSE_ACCEPT));

        controller.hideView();

        InOrder inOrder = inOrder(view);

        inOrder.verify(view).addAgent("agent1");
        inOrder.verify(view).setBackendStatus(eq(expected));
        inOrder.verify(view).getBackendStatus();

        verify(model).saveConfiguration();
    }

    /**
     * Verify that controller handles cancel properly
     */
    @Test
    public void testViewCancelEditingBackends() {
        final ActionListener<ConfigurationAction>[] listeners = (ActionListener<ConfigurationAction>[]) new ActionListener<?>[1];
        AgentConfigurationView view = mock(AgentConfigurationView.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                listeners[0] = (ActionListener<ConfigurationAction>) invocation.getArguments()[0];
                return null;
            }
        }).when(view).addActionListener(any(ActionListener.class));


        AgentConfigurationModel model = mock(AgentConfigurationModel.class);
        when(model.getAgents()).thenReturn(Arrays.asList(new String[]{"agent1"}));
        List<String> backends = Arrays.asList(new String[] {"backend1"});
        when(model.getBackends(any(String.class))).thenReturn(backends);
        when(model.getAgentBackendEnabled(any(String.class), any(String.class))).thenReturn(true);

        Map<String,Boolean> expectedConfig = new HashMap<>();
        expectedConfig.put("backend1", true);

        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();

        listeners[0].actionPerformed(new ActionEvent<ConfigurationAction>(view, ConfigurationAction.CLOSE_CANCEL));

        controller.hideView();

        verify(view).addAgent("agent1");
        verify(view).setBackendStatus(eq(expectedConfig));
        verify(view, never()).getBackendStatus();

        verify(model, never()).saveConfiguration();
    }
}
