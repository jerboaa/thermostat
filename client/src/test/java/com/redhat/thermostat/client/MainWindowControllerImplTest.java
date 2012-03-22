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

package com.redhat.thermostat.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.mongodb.DB;
import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.client.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;

public class MainWindowControllerImplTest {

    private PropertyChangeListener l;

    private MainWindowControllerImpl controller;

    private MainView view;

    private Timer mainWindowTimer;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        mainWindowTimer = mock(Timer.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(mainWindowTimer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        DB db = mock(DB.class);
        view = mock(MainView.class);
        ArgumentCaptor<PropertyChangeListener> grabListener = ArgumentCaptor.forClass(PropertyChangeListener.class);
        doNothing().when(view).addViewPropertyListener(grabListener.capture());
        controller = new MainWindowControllerImpl(db, view);
        l = grabListener.getValue();
    }

    @After
    public void tearDown() {
        view = null;
        controller = null;
        l = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyThatShutdownEventStopsController() {

        l.propertyChange(new PropertyChangeEvent(view, MainView.ViewProperty.SHUTDOWN.toString(), false, true));

        verify(mainWindowTimer).stop();

    }

    @Test
    public void verifyThatHostsVmsFilterChangeUpdatesTree() {

        l.propertyChange(new PropertyChangeEvent(view, MainView.ViewProperty.HOST_VM_TREE_FILTER.toString(), null, "test"));

        verify(view).updateTree(eq("test"), any(HostsVMsLoader.class));

    }

    @Test
    public void verifyTimerGetsStartedOnConstruction() {
        verify(mainWindowTimer).start();
    }

    @Test
    public void verifyShowMainWindowActuallyCallsView() {
        controller.showMainMainWindow();
        verify(view).showMainWindow();
    }
}
