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

package com.redhat.thermostat.vm.memory.client.core.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Size.Unit;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.locale.LocalizedString;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.gc.remote.common.command.GCCommand;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsViewProvider;
import com.redhat.thermostat.vm.memory.client.core.Payload;
import com.redhat.thermostat.vm.memory.client.core.StatsModel;
import com.redhat.thermostat.vm.memory.client.locale.LocaleResources;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class MemoryStatsController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private final MemoryStatsView view;
    private final VmMemoryStatDAO vmDao;
   
    private final VmRef ref;
    private final Timer timer;
    
    private final Map<String, Payload> regions;
    
    private VMCollector collector;
    
    class VMCollector implements Runnable {

        private long desiredUpdateTimeStamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

        @Override
        public void run() {
            List<VmMemoryStat> vmInfo = vmDao.getLatestVmMemoryStats(ref, desiredUpdateTimeStamp);
            for (VmMemoryStat memoryStats: vmInfo) {
                Generation[] generations = memoryStats.getGenerations();
                
                for (Generation generation : generations) {
                    Space[] spaces = generation.getSpaces();
                    for (Space space: spaces) {
                        Payload payload = regions.get(space.getName());
                        if (payload == null) {
                            payload = new Payload();
                            payload.setName(space.getName());
                        }

                        Size.Unit usedScale = bestUnitForRange(space.getUsed(), space.getCapacity());
                        double used = Size.bytes(space.getUsed()).convertTo(usedScale).getValue();
                        double maxUsed = Size.bytes(space.getCapacity()).convertTo(usedScale).getValue();
                        
                        payload.setUsed(used);
                        payload.setMaxUsed(maxUsed);
                        payload.setUsedUnit(usedScale);
                        
                        Size.Unit maxScale = bestUnitForRange(space.getCapacity(), space.getMaxCapacity());
                        double capacity = Size.bytes(space.getCapacity()).convertTo(maxScale).getValue();
                        double maxCapacity = Size.bytes(space.getMaxCapacity()).convertTo(maxScale).getValue();
                        
                        payload.setCapacity(capacity);
                        payload.setMaxCapacity(maxCapacity);
                        payload.setCapacityUnit(maxScale);
                        
                        String tooltip = space.getName() + ": used: " + String.format("%.2f", used) + " " + usedScale +
                                ", capacity: " + String.format("%.2f", capacity) + " " + maxScale +
                                ", max capacity: " + String.format("%.2f", maxCapacity) + " " + maxScale;
                        
                        payload.setTooltip(tooltip);
                        
                        StatsModel model = payload.getModel();
                        if (model == null) {
                            model = new StatsModel();
                            model.setName(space.getName());
                            model.setRange(3600);
                        }
                        
                        // normalize this always in the same unit
                        model.addData(memoryStats.getTimeStamp(), Size.bytes(space.getUsed()).convertTo(Unit.MiB).getValue());
                        
                        payload.setModel(model);
                        if (regions.containsKey(space.getName())) {
                            view.updateRegion(payload.clone());
                        } else {
                            view.addRegion(payload.clone());
                            regions.put(space.getName(), payload);
                        }
                        
                        view.requestRepaint();
                        desiredUpdateTimeStamp = Math.max(desiredUpdateTimeStamp, memoryStats.getTimeStamp());
                    }
                }
            }
        }
    }
    
    public MemoryStatsController(ApplicationService appSvc, final VmInfoDAO vmInfoDao,
                                 final VmMemoryStatDAO vmMemoryStatDao,
                                 final VmRef ref, MemoryStatsViewProvider viewProvider,
                                 final AgentInfoDAO agentDAO, final GCRequest gcRequest) {
        
        regions = new HashMap<>();
        this.ref = ref;
        vmDao = vmMemoryStatDao;
        view = viewProvider.createView();
        
        timer = appSvc.getTimerFactory().createTimer();
        
        collector = new VMCollector();
        timer.setAction(collector);
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);

        view.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch(actionEvent.getActionId()) {
                    case HIDDEN:
                        stop();
                        break;
                        
                    case VISIBLE:
                        start();
                        break;
                        
                    default:
                        throw new NotImplementedException("unknown event: " + actionEvent.getActionId());
                }
            }
        });
        
        view.addGCActionListener(new ActionListener<GCCommand>() {
            @Override
            public void actionPerformed(ActionEvent<GCCommand> actionEvent) {
                RequestResponseListener listener = new RequestResponseListener() {
                    @Override
                    public void fireComplete(Request request, Response response) {
                        if (response.getType() == ResponseType.ERROR) {
                            view.displayWarning(translate.localize(
                                    LocaleResources.ERROR_PERFORMING_GC,
                                    ref.getAgent().getAgentId(),
                                    ref.getIdString()));
                        }
                    }
                };
                gcRequest.sendGCRequestToAgent(ref, agentDAO, listener);
            }
        });

        if (!vmInfoDao.getVmInfo(ref).isAlive()) {
            view.setEnableGCAction(false);
        }

    }
    
    // for testing
    VMCollector getCollector() {
        return collector;
    };
    
    Map<String, Payload> getRegions() {
        return regions;
    }
    
    private Size.Unit bestUnitForRange(long min, long max) {
        // FIXME: this is very dumb and very inefficient
        // needs cleanup
        Size.Unit minScale = Size.Unit.getBestUnit(min);
        Size.Unit maxScale = Size.Unit.getBestUnit(max);
        
        Size.Unit[] scales = Size.Unit.values();
        int maxID = 0;
        int minID = 0;
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] == minScale) {
                minID = i;
            }
            if (scales[i] == maxScale) {
                maxID = i;
            }
        }
        while (maxID - minID >= 2) {
            minID++;
        }
        return scales[minID];
    }
    
    private void start() {
        timer.start();
    }

    private void stop() {
        timer.stop();
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translate.localize(LocaleResources.VM_INFO_TAB_MEMORY);
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) view;
    }
}

