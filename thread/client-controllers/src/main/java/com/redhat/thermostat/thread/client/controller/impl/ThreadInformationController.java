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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.osgi.service.BasicView.Action;
import com.redhat.thermostat.client.osgi.service.VmInformationServiceController;
import com.redhat.thermostat.client.ui.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.client.common.ThreadTableView.ThreadSelectionAction;
import com.redhat.thermostat.thread.client.common.ThreadView;
import com.redhat.thermostat.thread.client.common.ThreadView.ThreadAction;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.chart.LivingDaemonThreadDifferenceChart;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.model.ThreadSummary;

public class ThreadInformationController implements VmInformationServiceController {

    private static final Logger logger = Logger.getLogger(ThreadInformationController.class.getSimpleName());
    
    private ThreadView view;
    private ThreadCollector collector;
    
    private Timer timer;
    
    private LivingDaemonThreadDifferenceChart model;
        
    public ThreadInformationController(VmRef ref, ApplicationService appService,
                                       ThreadCollectorFactory collectorFactory, 
                                       ThreadViewProvider viewFactory)
    {
        view = viewFactory.createView();
        view.setApplicationService(appService, ref.getIdString() + "-" + ref.getAgent().getAgentId());
        
        collector = collectorFactory.getCollector(ref);
        
        initControllers();
        
        timer = ApplicationContext.getInstance().getTimerFactory().createTimer();
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        
        timer.setAction(new ThreadInformationDataCollector());
        
        model = new LivingDaemonThreadDifferenceChart("Living Threads vs. Daemon Threads",
                                                      "time", "threads", "Living Threads",
                                                      "Daemon Threads");
        model.setMaximumItemCount(3600);
        
        view.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case HIDDEN:
                    timer.stop();
                    break;
                
                case VISIBLE:
                    timer.start();
                    break;

                default:
                    throw new NotImplementedException("unknown event: " + actionEvent.getActionId());
                }
            }
        });
        
        view.setRecording(isRecording(), false);
        view.addThreadActionListener(new ThreadActionListener());
    }
    
    private boolean isRecording() {
        
        return collector.isHarvesterCollecting();
    }
    
    @Override
    public String getLocalizedName() {
        return "Threads";
    }

    @Override
    public UIComponent getView() {
        return view;
    }
    
    private class ThreadInformationDataCollector implements Runnable {
        @Override
        public void run() {

            // load the very latest thread summary
            ThreadSummary latestSummary = collector.getLatestThreadSummary();
            if (latestSummary.getTimeStamp() != 0) {
                view.setLiveThreads(Long.toString(latestSummary.currentLiveThreads()));
                view.setDaemonThreads(Long.toString(latestSummary.currentDaemonThreads()));
            }
            
            long lastHour = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
            List<ThreadSummary> summaries = collector.getThreadSummary(lastHour);
            if (summaries.size() != 0) {
                for (ThreadSummary summary : summaries) {
                    model.addData(summary.getTimeStamp(), summary.currentLiveThreads(), summary.currentDaemonThreads());
                }
                view.updateLivingDaemonTimeline(model);
            }
        }
    }
    
    private class ThreadActionListener implements ActionListener<ThreadAction> {

        @Override
        public void actionPerformed(ActionEvent<ThreadAction> actionEvent) {

            boolean result = false;
            
            switch (actionEvent.getActionId()) {
            case START_LIVE_RECORDING:
                result = collector.startHarvester();
                if (!result) {
                    view.displayWarning("Cannot enable Thread recording");
                    view.setRecording(false, false);
                }
                break;
            
            case STOP_LIVE_RECORDING:
                result = collector.stopHarvester();
                if (!result) {
                    view.displayWarning("Cannot disable Thread recording");
                    view.setRecording(true, false);
                }
                break;
                
            default:
                logger.log(Level.WARNING, "unkown action: " + actionEvent.getActionId());
                break;
            }
        }
    }
    
    private class ThreadSelectionActionListener implements ActionListener<ThreadSelectionAction> {
        @Override
        public void actionPerformed(ActionEvent<ThreadSelectionAction> actionEvent) {
            view.displayThreadDetails((ThreadTableBean) actionEvent.getPayload());
        }
    }
    
    private void initControllers() {
        CommonController capsController =
                new VMThreadCapabilitiesController(view.createVMThreadCapabilitiesView(), collector);
        capsController.initialize();
        
        ThreadTableView threadTableView = view.createThreadTableView();
        threadTableView.addThreadSelectionActionListener(new ThreadSelectionActionListener());
        CommonController threadTableController =
                new ThreadTableController(threadTableView, collector,
                                          ApplicationContext.getInstance().getTimerFactory().createTimer());
        threadTableController.initialize();
    }
}
