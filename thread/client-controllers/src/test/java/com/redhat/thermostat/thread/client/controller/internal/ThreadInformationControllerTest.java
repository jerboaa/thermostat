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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationCache;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.common.view.LockView;
import com.redhat.thermostat.thread.client.common.view.StackTraceProfilerView;
import com.redhat.thermostat.thread.client.common.view.ThreadCountView;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView.ThreadSelectionAction;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.common.view.ThreadView;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadInformationControllerTest {

    private ThreadView view;

    private ActionListener<ThreadTableView.ThreadSelectionAction> threadTableActionListener;

    private ThreadViewProvider viewFactory;
    private ThreadInformationController controller;
    
    private ApplicationService appService;
    private ExecutorService appExecutor;

    private VmInfo vmInfo;
    private VmInfoDAO vmInfoDao;
    private LockInfoDao lockInfoDao;

    private ThreadTableView threadTableView;
    private VmDeadLockView deadLockView;
    private ThreadTimelineView threadTimelineView;
    private ThreadCountView threadCountView;
    private LockView lockView;
    private StackTraceProfilerView stackTraceProfilerView;

    private AppCache theCache;

    @Before
    public void setUp() {
        theCache = mock(AppCache.class);

        appService = mock(ApplicationService.class);
        vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive()).thenReturn(true);
        vmInfoDao = mock(VmInfoDAO.class);
        when(vmInfoDao.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);
        lockInfoDao = mock(LockInfoDao.class);
        setUpTimers();
        setupCache();
        setupExecutor();
        setUpView();
    }

    private void setUpView() {
        deadLockView = mock(VmDeadLockView.class);
        threadTableView = mock(ThreadTableView.class);
        threadTimelineView = mock(ThreadTimelineView.class);
        threadCountView = mock(ThreadCountView.class);
        lockView = mock(LockView.class);
        stackTraceProfilerView = mock(StackTraceProfilerView.class);

        view = mock(ThreadView.class);
        viewFactory = mock(ThreadViewProvider.class);
        when(viewFactory.createView()).thenReturn(view);
        
        when(view.createDeadLockView()).thenReturn(deadLockView);
        when(view.createThreadTableView()).thenReturn(threadTableView);
        when(view.createThreadTimelineView()).thenReturn(threadTimelineView);
        when(view.createThreadCountView()).thenReturn(threadCountView);
        when(view.createLockView()).thenReturn(lockView);
        when(view.createStackTraceProfilerView()).thenReturn(stackTraceProfilerView);

    }
    
    private void setUpTimers() {
        Timer timer = mock(Timer.class);

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appService.getTimerFactory()).thenReturn(timerFactory);
    }
    
    private void setupCache() {
        ApplicationCache cache = mock(ApplicationCache.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        when(cache.getAttribute(anyString())).thenReturn(theCache);
    }

    private void setupExecutor() {
        appExecutor = mock(ExecutorService.class);
        when(appService.getApplicationExecutor()).thenReturn(appExecutor);
    }

    private void setUpListeners() {        
        doNothing().when(view).addActionListener(any(ActionListener.class));
        
        ArgumentCaptor<ActionListener> threadTableViewCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(threadTableView).addThreadSelectionActionListener(threadTableViewCaptor.capture());
        
        createController();
        
        threadTableActionListener = threadTableViewCaptor.getValue();
    }
    
    private void createController() {

        VmRef ref = mock(VmRef.class);
        HostRef agent = mock(HostRef.class);
        when(ref.getHostRef()).thenReturn(agent);
        when(agent.getAgentId()).thenReturn("0xcafe");

        ThreadCollectorFactory collectorFactory = mock(ThreadCollectorFactory.class);
        ThreadCollector collector = mock(ThreadCollector.class);
        when(collectorFactory.getCollector(ref)).thenReturn(collector);

        ProgressNotifier notifier = mock(ProgressNotifier.class);

        controller = new ThreadInformationController(ref, appService, vmInfoDao, lockInfoDao,
                                                     collectorFactory,
                                                     viewFactory, notifier);
    }
    
    @Test
    public void verifyViewCreateSubViewCalled() {
        
        createController();
        
        verify(view).createThreadTableView();
        verify(view).createDeadLockView();
        verify(view).createThreadTimelineView();
        verify(view).createThreadCountView();
    }
    
    @Test
    public void verifyLiveRecording() {
        
        ActionListener<ThreadView.ThreadAction> threadActionListener;
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addThreadActionListener(viewArgumentCaptor.capture());

        VmRef ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn("42");
        HostRef agent = mock(HostRef.class);
        when(ref.getHostRef()).thenReturn(agent);
        when(agent.getAgentId()).thenReturn("0xcafe");

        ThreadCollector collector = mock(ThreadCollector.class);
        when(collector.isHarvesterCollecting()).thenReturn(false).thenReturn(true);
        when(collector.startHarvester()).thenReturn(true);
        when(collector.stopHarvester()).thenReturn(true).thenReturn(false);

        ThreadCollectorFactory collectorFactory = mock(ThreadCollectorFactory.class);
        when(collectorFactory.getCollector(ref)).thenReturn(collector);

        ProgressNotifier notifier = mock(ProgressNotifier.class);

        ArgumentCaptor<Runnable> longRunningTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(appExecutor).execute(longRunningTaskCaptor.capture());

        controller = new ThreadInformationController(ref, appService,
                                                     vmInfoDao, lockInfoDao,
                                                     collectorFactory,
                                                     viewFactory, notifier);

        verify(collector).isHarvesterCollecting();
        verify(view, times(1)).setRecording(ThreadView.MonitoringState.STOPPED, false);

        // each action event posts a task to the executor.
        // make sure the task is posted and execute it manually in tests to see its effects.

        threadActionListener = viewArgumentCaptor.getValue();
        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.START_LIVE_RECORDING));

        verify(appExecutor, times(1)).execute(isA(Runnable.class));
        longRunningTaskCaptor.getValue().run();

        verify(view, times(1)).setRecording(ThreadView.MonitoringState.STARTING, false);
        verify(view, times(1)).setRecording(ThreadView.MonitoringState.STARTED, false);
        verify(collector).startHarvester();

        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.STOP_LIVE_RECORDING));

        verify(appExecutor, times(2)).execute(isA(Runnable.class));
        longRunningTaskCaptor.getValue().run();

        verify(collector).stopHarvester();
        verify(view, times(1)).setRecording(ThreadView.MonitoringState.STOPPING, false);
        verify(view, times(2)).setRecording(ThreadView.MonitoringState.STOPPED, false);

        threadActionListener.actionPerformed(new ActionEvent<>(view, ThreadView.ThreadAction.STOP_LIVE_RECORDING));

        verify(appExecutor, times(3)).execute(isA(Runnable.class));
        longRunningTaskCaptor.getValue().run();

        verify(collector, times(2)).stopHarvester();
        verify(view, times(2)).setRecording(ThreadView.MonitoringState.STOPPING, false);
        verify(view, times(3)).setRecording(ThreadView.MonitoringState.STOPPED, false);
    }

    @Test
    public void verifyRecordingControlDisabledForDeadVms() {
        when(vmInfo.isAlive()).thenReturn(false);

        createController();

        verify(view).setEnableRecordingControl(false);
    }
    
    @Test
    public void verifyTableViewLinksToDetailsView() {
        setUpListeners();

        ThreadTableBean bean = mock(ThreadTableBean.class);

        ActionEvent<ThreadSelectionAction> event =
                new ActionEvent<>(threadTableView, ThreadSelectionAction.SHOW_THREAD_DETAILS);
        event.setPayload(bean);
        
        threadTableActionListener.actionPerformed(event);
        verify(view).displayThreadDetails(bean);
    }

    @Test
    public void verifySessionListDisplays() {

        ThreadCollector collector = mock(ThreadCollector.class);
        List<ThreadSession> list = new ArrayList<>();
        list.add(mock(ThreadSession.class));
        list.add(mock(ThreadSession.class));

        when(collector.getThreadSessions(any(Range.class))).thenReturn(list);

        ActionListener<ThreadView.ThreadAction> threadActionListener;
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addThreadActionListener(viewArgumentCaptor.capture());

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(appExecutor).execute(taskCaptor.capture());

        createController();

        controller.___injectCollectorForTesting(collector);

        threadActionListener = viewArgumentCaptor.getValue();
        ActionEvent<ThreadView.ThreadAction> action = new ActionEvent<>(view, ThreadView.ThreadAction.REQUEST_DISPLAY_RECORDED_SESSIONS);
        threadActionListener.actionPerformed(action);

        Runnable runnable = taskCaptor.getValue();
        runnable.run();

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(view).displayTimelineSessionList(listCaptor.capture());

        Assert.assertTrue(listCaptor.getValue().equals(list));
    }

    @Test
    public void verifySessionLoads() {

        ThreadTimelineController timeline = mock(ThreadTimelineController.class);
        ThreadTableController table = mock(ThreadTableController.class);
        ThreadCountController count = mock(ThreadCountController.class);
        LockController lock = mock(LockController.class);
        VmDeadLockController deadLock = mock(VmDeadLockController.class);
        StackTraceProfilerController profiler = mock(StackTraceProfilerController.class);

        ThreadSession session = mock(ThreadSession.class);
        SessionID id = mock(SessionID.class);
        when(session.getSessionID()).thenReturn(id);

        ActionListener<ThreadView.ThreadAction> threadActionListener;
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addThreadActionListener(viewArgumentCaptor.capture());

        createController();

        controller.___injectControllersForTesting(timeline, table, count, lock, deadLock, profiler);

        threadActionListener = viewArgumentCaptor.getValue();
        ActionEvent<ThreadView.ThreadAction> action = new ActionEvent<>(view, ThreadView.ThreadAction.REQUEST_LOAD_SESSION);
        action.setPayload(session);

        threadActionListener.actionPerformed(action);

        verify(timeline).setSession(id);
        verify(table).setSession(id);
        verify(profiler).setSession(id);
    }
}

