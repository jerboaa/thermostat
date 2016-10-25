/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.ControllerLifeCycleState;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.LifeCycle;
import com.redhat.thermostat.platform.mvc.Controller;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.Model;
import com.redhat.thermostat.platform.mvc.View;
import com.redhat.thermostat.platform.mvc.Workbench;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 */
public class LifeCycleStateHandlerTest {

    private BooleanProperty showing;

    private TestPlatform platform;
    private PlatformServiceRegistrar serviceRegistrar;
    private LifeCycleTransitionDispatcher dispatcher;

    private Workbench workbenchProvider;
    private MVCProvider provider;
    private View view;
    private Model model;
    private Controller controller;

    private ActionEvent<LifeCycle> actionEvent;

    @Before
    public void setUp() {
        provider =  mock(MVCProvider.class);
        workbenchProvider = mock(Workbench.class);

        view = mock(View.class);
        when(provider.getView()).thenReturn(view);
        when(workbenchProvider.getView()).thenReturn(view);

        showing = mock(BooleanProperty.class);
        when(view.showingProperty()).thenReturn(showing);

        model = mock(Model.class);
        when(provider.getModel()).thenReturn(model);
        when(workbenchProvider.getModel()).thenReturn(model);

        controller = mock(Controller.class);
        when(provider.getController()).thenReturn(controller);
        when(workbenchProvider.getController()).thenReturn(controller);

        platform = new TestPlatform();
        serviceRegistrar =  mock(PlatformServiceRegistrar.class);
        actionEvent = mock(ActionEvent.class);
        dispatcher = mock(LifeCycleTransitionDispatcher.class);
    }

    private LifeCycleStateHandler createHandler(MVCProvider provider) {
        LifeCycleStateHandler handler =
                new LifeCycleStateHandler(provider, platform, serviceRegistrar);
        handler.setDispatcher(dispatcher);
        return handler;
    }

    @Test
    public void test_CREATE_VIEW() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.CREATE_VIEW);

        LifeCycleStateHandler handler = createHandler(provider);
        handler.actionPerformed(actionEvent);

        platform.viewRunnable.run();
        verify(view).create();
        verify(dispatcher).requestLifeCycleTransition(LifeCycle.VIEW_CREATED);
    }

    @Test
    public void test_VIEW_CREATED() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.VIEW_CREATED);

        LifeCycleStateHandler handler = createHandler(provider);
        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(model).create();
        verify(controller).create();
        verify(dispatcher).requestLifeCycleTransition(LifeCycle.INIT_VIEW);
    }

    @Test
    public void test_INIT_VIEW() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.INIT_VIEW);

        LifeCycleStateHandler handler = createHandler(provider);
        handler.actionPerformed(actionEvent);

        platform.viewRunnable.run();
        verify(view).init(platform);

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.VIEW_INITIALIZED);
    }

    @Test
    public void test_VIEW_INITIALIZED() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.VIEW_INITIALIZED);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        assertEquals(ControllerLifeCycleState.PRE_INIT, handler.getCurrentControllerState());

        platform.applicationRunnable.run();
        verify(model).init(platform);
        verify(controller).init(platform, model, view);

        assertEquals(ControllerLifeCycleState.STOPPED, handler.getCurrentControllerState());

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.REGISTER_MVC);
    }

    @Test
    public void test_REGISTER_MVC() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.REGISTER_MVC);

        LifeCycleStateHandler handler = createHandler(provider);
        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(serviceRegistrar).checkAndRegister(provider);

        // this is not true for a Workbench provider, it is tested later
        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void test_REGISTER_MVC_Workbench() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.REGISTER_MVC);

        LifeCycleStateHandler handler = createHandler(workbenchProvider);
        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(serviceRegistrar).checkAndRegister(workbenchProvider);

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.START_CONTROLLER);
    }

    @Test
    public void test_REGISTER_MVC_StartOnVisible() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.REGISTER_MVC);
        when(showing.get()).thenReturn(true);

        LifeCycleStateHandler handler = createHandler(provider);
        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.START_CONTROLLER);
    }

    @Test
    public void test_START_CONTROLLER() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.START_CONTROLLER);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        assertFalse(handler.getCurrentControllerState().equals(ControllerLifeCycleState.STARTED));

        platform.applicationRunnable.run();
        verify(controller).start();

        assertEquals(ControllerLifeCycleState.STARTED, handler.getCurrentControllerState());

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.START_VIEW);
    }

    @Test
    public void test_START_VIEW() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.START_VIEW);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.viewRunnable.run();
        verify(view).start();

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.STARTED);
    }

    @Test
    public void test_STARTED() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.STARTED);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(controller).viewStarted();

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void test_STOP_CONTROLLER() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.STOP_CONTROLLER);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        // in a real lifecycle this should not be PRE_INIT but STARTED
        assertEquals(ControllerLifeCycleState.PRE_INIT, handler.getCurrentControllerState());

        platform.applicationRunnable.run();
        verify(controller).stop();

        assertEquals(ControllerLifeCycleState.STOPPED, handler.getCurrentControllerState());

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.STOP_VIEW);
    }

    @Test
    public void test_STOP_VIEW() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.STOP_VIEW);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.viewRunnable.run();
        verify(view).stop();

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.STOPPED);
    }

    @Test
    public void test_STOPPED() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.STOPPED);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(controller).viewStopped();

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void test_DESTROY_VIEW() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.DESTROY_VIEW);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.viewRunnable.run();
        verify(view).destroy();

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.DESTROY);
    }

    @Test
    public void test_DESTROY() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.DESTROY);

        LifeCycleStateHandler handler = createHandler(provider);

        handler.actionPerformed(actionEvent);

        platform.applicationRunnable.run();
        verify(controller).destroy();
        verify(model).destroy();

        verify(dispatcher).requestLifeCycleTransition(LifeCycle.DESTROYED);
    }

    @Test
    public void test_DESTROYED() {
        when(actionEvent.getActionId()).thenReturn(LifeCycle.DESTROYED);

        LifeCycleStateHandler handler = createHandler(provider);

        assertFalse(handler.shutdownProperty().get());

        handler.actionPerformed(actionEvent);

        assertNull(platform.applicationRunnable);
        assertNull(platform.viewRunnable);

        assertTrue(handler.shutdownProperty().get());

        verifyNoMoreInteractions(dispatcher);
    }

    private class TestPlatform implements Platform {

        Runnable applicationRunnable;
        Runnable viewRunnable;

        @Override
        public void queueOnApplicationThread(Runnable runnable) {
            applicationRunnable = runnable;
        }

        @Override
        public void queueOnViewThread(Runnable runnable) {
            viewRunnable = runnable;
        }

        @Override
        public boolean isViewThread() {
            return true;
        }

        @Override
        public boolean isApplicationThread() {
            return true;
        }

        @Override
        public ApplicationService getAppService() {
            return null;
        }
    }
}
