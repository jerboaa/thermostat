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

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.thread.cache.RangedCache;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView.ThreadSelectionAction;
import com.redhat.thermostat.thread.client.common.view.ThreadView;
import com.redhat.thermostat.thread.client.common.view.ThreadView.ThreadAction;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadInformationController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private VmRef ref;
    private VmInfoDAO vmInfoDAO;
    private LockInfoDao lockInfoDao;

    private static final Logger logger = LoggingUtils.getLogger(ThreadInformationController.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    private ThreadView view;
    private ThreadCollector collector;

    private ApplicationService appService;

    private AppCache cache;
    private ProgressNotifier notifier;

    private CommonController threadTimeline;
    private CommonController threadTableController;
    private CommonController threadCountController;
    private VmDeadLockController deadLockController;
    private CommonController lockTableController;
    private CommonController stackTraceProfilerController;

    public ThreadInformationController(VmRef ref, ApplicationService appService,
                                       VmInfoDAO vmInfoDao,
                                       LockInfoDao lockInfoDao,
                                       ThreadCollectorFactory collectorFactory, 
                                       ThreadViewProvider viewFactory,
                                       ProgressNotifier notifier)
    {
        this.appService = appService;
        this.ref = ref;
        this.notifier = notifier;
        this.vmInfoDAO = vmInfoDao;
        this.lockInfoDao = lockInfoDao;

        setUpAppCache();

        collector = collectorFactory.getCollector(ref);

        view = viewFactory.createView();
        view.setApplicationService(appService, ref.getVmId() + "-" + ref.getHostRef().getAgentId());

        initControllers();
        
        view.setRecording(isRecording() ? ThreadView.MonitoringState.STARTED : ThreadView.MonitoringState.STOPPED, false);
        view.addThreadActionListener(new ThreadActionListener());

        view.setEnableRecordingControl(vmInfoDao.getVmInfo(ref).isAlive());
    }

    private void setUpAppCache() {
        this.cache = (AppCache) appService.getApplicationCache().getAttribute(ref.getVmId());
        Map<SessionID, RangedCache<ThreadState>> threadStatesCache = cache.retrieve(CommonController.THREAD_STATE_CACHE);
        if (threadStatesCache == null) {
            threadStatesCache = new ConcurrentHashMap<>();
            cache.save(CommonController.THREAD_STATE_CACHE, threadStatesCache);
        }
    }

    private boolean isRecording() {
        
        return collector.isHarvesterCollecting();
    }
    
    @Override
    public LocalizedString getLocalizedName() {
        return t.localize(LocaleResources.CONTROLLER_NAME);
    }

    @Override
    public UIComponent getView() {
        return view;
    }
    
    private class ThreadActionListener implements ActionListener<ThreadAction> {

        @Override
        public void actionPerformed(ActionEvent<ThreadAction> actionEvent) {
            switch (actionEvent.getActionId()) {
            case START_LIVE_RECORDING:
                view.setRecording(ThreadView.MonitoringState.STARTING, false);
                startHarvester();
                view.setRecording(ThreadView.MonitoringState.STARTED, false);
                break;
            
            case STOP_LIVE_RECORDING:
                view.setRecording(ThreadView.MonitoringState.STOPPING, false);
                stopHarvester();
                view.setRecording(ThreadView.MonitoringState.STOPPED, false);
                break;

            case REQUEST_DISPLAY_RECORDED_SESSIONS:
                loadSessionsList();
                break;

            case REQUEST_LOAD_SESSION: {
                ThreadSession session = (ThreadSession) actionEvent.getPayload();
                if (session != null) {
                    threadTimeline.setSession(session.getSessionID());
                    threadTableController.setSession(session.getSessionID());
                    stackTraceProfilerController.setSession(session.getSessionID());
                }
            } break;

            default:
                logger.log(Level.WARNING, "unknown action: " + actionEvent.getActionId());
                break;
            }
        }

        private class SessionComparator implements Comparator<ThreadSession> {
            @Override
            public int compare(ThreadSession o1, ThreadSession o2) {
                // TODO: descending order only for now, we should allow the users
                // to sort this via the UI though
                int result = Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
                return -result;
            }
        }

        private void loadSessionsList() {
            submitTask(new Runnable() {
                @Override
                public void run() {
                    Range<Long> range = new Range<>(0L, System.currentTimeMillis());
                    List<ThreadSession> threadSessions = collector.getThreadSessions(range);

                    Collections.sort(threadSessions, new SessionComparator());
                    view.displayTimelineSessionList(threadSessions);

                }
            }, translator.localize(LocaleResources.LOADING_SESSION_LIST));
        }

        private void startHarvester() {
            submitTask(new Runnable() {
                @Override
                public void run() {
                    boolean result = collector.startHarvester();
                    if (!result) {
                        view.displayWarning(t.localize(LocaleResources.WARNING_CANNOT_ENABLE));
                        view.setEnableRecordingControl(false);
                        view.setRecording(ThreadView.MonitoringState.DISABLED, false);
                    }
                }
            }, translator.localize(LocaleResources.STARTING_MONITORING));
        }

        private void stopHarvester() {
            submitTask(new Runnable() {
                @Override
                public void run() {
                    boolean result = collector.stopHarvester();
                    if (!result) {
                        view.displayWarning(t.localize(LocaleResources.WARNING_CANNOT_DISABLE));
                        view.setEnableRecordingControl(false);
                        view.setRecording(ThreadView.MonitoringState.DISABLED, false);
                    }
                }
            }, translator.localize(LocaleResources.STOPPING_MONITORING));
        }

        private void submitTask(final Runnable task, final LocalizedString taskName) {
            appService.getApplicationExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    final ProgressHandle handle = new ProgressHandle(taskName);
                    handle.setTask(taskName);
                    handle.setIndeterminate(true);
                    notifier.register(handle);
                    handle.runTask(task);
                }
            });
        }
    }
    
    private class ThreadSelectionActionListener implements ActionListener<ThreadSelectionAction> {
        @Override
        public void actionPerformed(ActionEvent<ThreadSelectionAction> actionEvent) {
            view.displayThreadDetails((ThreadTableBean) actionEvent.getPayload());
        }
    }

    void ___injectControllersForTesting(ThreadTimelineController timeline,
                                        ThreadTableController table,
                                        ThreadCountController count,
                                        LockController lock,
                                        VmDeadLockController deadLock,
                                        StackTraceProfilerController profiler)
    {
        this.threadTableController = table;
        this.threadCountController = count;
        this.deadLockController = deadLock;
        this.threadTimeline = timeline;
        this.lockTableController = lock;
        this.stackTraceProfilerController = profiler;
    }

    void ___injectCollectorForTesting(ThreadCollector collector)   {
        this.collector = collector;
    }

    private void initControllers() {

        SessionID lastThreadSession = collector.getLastThreadSession();

        TimerFactory tf = appService.getTimerFactory();

        deadLockController =
                new VmDeadLockController(vmInfoDAO, ref, view.createDeadLockView(), collector, tf.createTimer(),
                        appService.getApplicationExecutor(), notifier);
        deadLockController.initialize();

        ThreadTableView threadTableView = view.createThreadTableView();
        threadTableView.addThreadSelectionActionListener(new ThreadSelectionActionListener());
        
        threadCountController =
                new ThreadCountController(view.createThreadCountView(), collector, tf.createTimer(), cache);
        threadCountController.initialize();

        threadTableController =
                new ThreadTableController(threadTableView, collector, tf.createTimer(), cache);
        threadTableController.initialize();
        threadTableController.setSession(lastThreadSession);

        threadTimeline = new ThreadTimelineController(view.createThreadTimelineView(),
                                                      collector,
                                                      tf.createTimer(),
                                                      cache);
        threadTimeline.initialize();
        threadTimeline.setSession(lastThreadSession);

        lockTableController =
                new LockController(view.createLockView(),
                                   tf.createTimer(),
                                   lockInfoDao,
                                   ref,
                                   cache);
        lockTableController.initialize();

        stackTraceProfilerController =
                new StackTraceProfilerController(view.createStackTraceProfilerView(),
                                                 collector,
                                                 tf.createTimer(),
                                                 ref,
                                                 cache);
        stackTraceProfilerController.initialize();
        stackTraceProfilerController.setSession(lastThreadSession);
    }
}

