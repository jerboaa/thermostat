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

package com.redhat.thermostat.thread.client.controller.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.osgi.service.ApplicationCache;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.client.common.ThreadView;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.VMThreadCapabilitiesView;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;

public class ThreadInformationControllerTest {

    private Timer timer;
    private ThreadView view;
    private ActionListener<ThreadView.Action> actionListener;
        
    private ThreadViewProvider viewFactory;
    private ThreadInformationController controller;
    
    private ApplicationService appService;
        
    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        setUpTimers();
        setUpView();
    }
    
    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    private void setUpView() {
        VMThreadCapabilitiesView commonController1 = mock(VMThreadCapabilitiesView.class);
        ThreadTableView commonController2 = mock(ThreadTableView.class);

        view = mock(ThreadView.class);
        viewFactory = mock(ThreadViewProvider.class);
        when(viewFactory.createView()).thenReturn(view);
        
        when(view.createVMThreadCapabilitiesView()).thenReturn(commonController1);
        when(view.createThreadTableView()).thenReturn(commonController2);
    }
    
    private void setUpTimers() {
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
    }
    
    private void setUpListeners() {        
        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor1.capture());
        
        createController();
        
        actionListener = viewArgumentCaptor1.getValue();
    }
    
    private void createController() {
        ApplicationCache cache = mock(ApplicationCache.class);
        appService = mock(ApplicationService.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        
        VmRef ref = mock(VmRef.class);
        HostRef agent = mock(HostRef.class);
        when(ref.getAgent()).thenReturn(agent);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        ThreadCollectorFactory collectorFactory = mock(ThreadCollectorFactory.class);
        ThreadCollector collector = mock(ThreadCollector.class);
        when(collectorFactory.getCollector(ref)).thenReturn(collector);
        
        controller = new ThreadInformationController(ref, appService, collectorFactory, viewFactory);
    }
    
    @Test
    public void verifyLiveRecording() {
        
        ActionListener<ThreadView.ThreadAction> threadActionListener;
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addThreadActionListener(viewArgumentCaptor.capture());
        
        VmRef ref = mock(VmRef.class);
        when(ref.getStringID()).thenReturn("42");
        HostRef agent = mock(HostRef.class);
        when(ref.getAgent()).thenReturn(agent);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        ThreadCollector collector = mock(ThreadCollector.class);
        when(collector.isHarvesterCollecting()).thenReturn(false).thenReturn(true);
        when(collector.startHarvester()).thenReturn(true);
        when(collector.stopHarvester()).thenReturn(true).thenReturn(false);

        ThreadCollectorFactory collectorFactory = mock(ThreadCollectorFactory.class);
        when(collectorFactory.getCollector(ref)).thenReturn(collector);
        
        ApplicationCache cache = mock(ApplicationCache.class);
        appService = mock(ApplicationService.class);
        when(appService.getApplicationCache()).thenReturn(cache);
                
        controller = new ThreadInformationController(ref, appService, collectorFactory, viewFactory);
        
        verify(collector).isHarvesterCollecting();
        verify(view, times(1)).setRecording(false, false);
        
        threadActionListener = viewArgumentCaptor.getValue();
        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.START_LIVE_RECORDING));
        
        verify(view, times(1)).setRecording(false, false);
        verify(collector).startHarvester();
        
        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.STOP_LIVE_RECORDING));
        
        verify(collector).stopHarvester();        
        verify(view, times(1)).setRecording(false, false);
        
        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.STOP_LIVE_RECORDING));
        
        verify(collector, times(2)).stopHarvester();        
        verify(view, times(1)).setRecording(true, false);
    }
    
    @Test
    public void testTimerStartOnViewVisible() {
        setUpListeners();

        actionListener.actionPerformed(new ActionEvent<>(view, ThreadView.Action.VISIBLE));
        verify(timer).start();
    }
    
    @Test
    public void testTimerStopsOnViewHidden() {
        setUpListeners();
        
        actionListener.actionPerformed(new ActionEvent<>(view, ThreadView.Action.HIDDEN));
        verify(timer).stop();
    }
}
