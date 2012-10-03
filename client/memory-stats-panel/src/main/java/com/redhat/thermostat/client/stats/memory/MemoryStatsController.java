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

package com.redhat.thermostat.client.stats.memory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.locale.Translate;
import com.redhat.thermostat.client.osgi.service.BasicView;
import com.redhat.thermostat.client.osgi.service.VmInformationServiceController;
import com.redhat.thermostat.client.osgi.service.BasicView.Action;
import com.redhat.thermostat.client.ui.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.utils.DisplayableValues.Scale;

class MemoryStatsController implements VmInformationServiceController {

    private final MemoryStatsView view;
    private final VmMemoryStatDAO vmDao;
   
    private final VmRef ref;
    private final Timer timer;
    
    private final Map<String, Payload> regions;
    
    private VMCollector collector;
    
    class VMCollector implements Runnable {
        @Override
        public void run() {
            List<VmMemoryStat> vmInfo = vmDao.getLatestVmMemoryStats(ref, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
            for (VmMemoryStat memoryStats: vmInfo) {
                List<Generation> generations = memoryStats.getGenerations();
                
                for (Generation generation : generations) {
                    List<Space> spaces = generation.spaces;
                    for (Space space: spaces) {
                        Payload payload = regions.get(space.name);
                        if (payload == null) {
                            payload = new Payload();
                            payload.setName(space.name);
                        }

                        Scale usedScale = normalizeScale(space.used, space.capacity);
                        double used = Scale.convertTo(usedScale, space.used, 100);
                        double maxUsed = Scale.convertTo(usedScale, space.capacity, 100);
                        
                        payload.setUsed(used);
                        payload.setMaxUsed(maxUsed);
                        payload.setUsedUnit(usedScale);
                        
                        Scale maxScale = normalizeScale(space.capacity, space.maxCapacity);
                        double capacity = Scale.convertTo(maxScale, space.capacity, 100);
                        double maxCapacity = Scale.convertTo(maxScale, space.maxCapacity, 100);
                        
                        payload.setCapacity(capacity);
                        payload.setMaxCapacity(maxCapacity);
                        payload.setCapacityUnit(maxScale);
                        
                        String tooltip = space.name + ": used: " + used + " " + usedScale +
                                ", capacity: " + capacity + " " + maxScale +
                                ", max capacity: " + maxCapacity + " " + maxScale;
                        
                        payload.setTooltip(tooltip);
                        
                        StatsModel model = payload.getModel();
                        if (model == null) {
                            model = new StatsModel();
                            model.setName(space.name);
                            model.setRange(3600);
                        }
                        
                        // normalize this always in the same unit
                        model.addData(memoryStats.getTimeStamp(),
                                      Scale.convertTo(Scale.MiB, space.used, 100));
                        
                        payload.setModel(model);
                        if (regions.containsKey(space.name)) {
                            view.updateRegion(payload.clone());
                        } else {
                            view.addRegion(payload.clone());
                            regions.put(space.name, payload);
                        }
                        
                        view.requestRepaint();
                    }
                }
            }
        }
    }
    
    public MemoryStatsController(final VmMemoryStatDAO vmMemoryStatDao, final VmRef ref) {
        
        regions = new HashMap<>();
        this.ref = ref;
        view = ApplicationContext.getInstance().getViewFactory().getView(MemoryStatsView.class);
        vmDao = vmMemoryStatDao;
        
        timer = ApplicationContext.getInstance().getTimerFactory().createTimer();
        
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
    }
    
    // for testing
    VMCollector getCollector() {
        return collector;
    };
    
    Map<String, Payload> getRegions() {
        return regions;
    }
    
    private Scale normalizeScale(long min, long max) {
        // FIXME: this is very dumb and very inefficient
        // needs cleanup
        Scale minScale = Scale.getScale(min);
        Scale maxScale = Scale.getScale(max);
        
        Scale[] scales = Scale.values();
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
    public String getLocalizedName() {
        // TODO: this class should have it's own bundle...
        return Translate.localize(LocaleResources.VM_INFO_TAB_MEMORY);
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) view;
    }
}
