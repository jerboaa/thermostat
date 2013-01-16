/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.common.view.ThreadView;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView.ThreadSelectionAction;
import com.redhat.thermostat.thread.client.common.view.ThreadView.ThreadAction;

public class ThreadInformationController implements InformationServiceController<VmRef> {

    private static final Logger logger = LoggingUtils.getLogger(ThreadInformationController.class);
    
    private ThreadView view;
    private ThreadCollector collector;

    private ApplicationService appService;
            
    public ThreadInformationController(VmRef ref, ApplicationService appService,
                                       VmInfoDAO vmInfoDao,
                                       ThreadCollectorFactory collectorFactory, 
                                       ThreadViewProvider viewFactory)
    {
        this.appService = appService;
        view = viewFactory.createView();
        view.setApplicationService(appService, ref.getIdString() + "-" + ref.getAgent().getAgentId());
        
        collector = collectorFactory.getCollector(ref);
        
        initControllers();
        
        view.setRecording(isRecording(), false);
        view.addThreadActionListener(new ThreadActionListener());

        if (!vmInfoDao.getVmInfo(ref).isAlive()) {
            view.setEnableRecordingControl(false);
        }
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
                
        VMThreadCapabilitiesController capsController =
                new VMThreadCapabilitiesController(view.createVMThreadCapabilitiesView(), collector);
        capsController.initialize();
        
        ThreadTableView threadTableView = view.createThreadTableView();
        threadTableView.addThreadSelectionActionListener(new ThreadSelectionActionListener());
        TimerFactory tf = appService.getTimerFactory();
        
        CommonController threadCountController =
                new ThreadCountController(view.createThreadCountView(), collector, tf.createTimer());
        threadCountController.initialize();
        
        CommonController threadTableController =
                new ThreadTableController(threadTableView, collector, tf.createTimer());
        threadTableController.initialize();
        
        CommonController threadTimeline =
                new ThreadTimelineController(view.createThreadTimelineView(), collector,
                                             tf.createTimer());
        threadTimeline.initialize();
    }
}

