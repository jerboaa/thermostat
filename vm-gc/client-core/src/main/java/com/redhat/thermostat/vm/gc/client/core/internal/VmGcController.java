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

package com.redhat.thermostat.vm.gc.client.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.storage.model.TimeStampedPojoComparator;
import com.redhat.thermostat.storage.model.VmGcStat;
import com.redhat.thermostat.storage.model.VmMemoryStat;
import com.redhat.thermostat.storage.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.core.VmGcViewProvider;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class VmGcController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final VmRef ref;
    private final VmGcView view;

    private final VmGcStatDAO gcDao;
    private final VmMemoryStatDAO memDao;

    private final Set<String> addedCollectors = new TreeSet<>();
    // the last value seen for each collector
    private final Map<String, VmGcStat> lastValueSeen = new TreeMap<>();

    private final Timer timer;

    private long lastSeenTimeStamp = Long.MIN_VALUE;

    public VmGcController(ApplicationService appSvc, VmMemoryStatDAO vmMemoryStatDao, VmGcStatDAO vmGcStatDao, VmRef ref, VmGcViewProvider provider) {
        this.ref = ref;
        this.view = provider.createView();
        this.timer = appSvc.getTimerFactory().createTimer();

        gcDao = vmGcStatDao;
        memDao = vmMemoryStatDao;

        view.addActionListener(new ActionListener<VmGcView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        stop();
                        break;
                    case VISIBLE:
                        start();
                        break;
                    default:
                        throw new NotImplementedException("unkonwn action: " + actionEvent.getActionId());
                }
            }
        });

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                try {
                    doUpdateCollectorData();
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw t;
                }
            }
        });
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setInitialDelay(0);
        timer.setDelay(5);
        timer.setTimeUnit(TimeUnit.SECONDS);
    }

    private void start() {
        timer.start();
    }

    private void stop() {
         timer.stop();
    }

    // FIXME
    private String chartName(String collectorName, String generationName) {
        return translator.localize(LocaleResources.VM_GC_COLLECTOR_OVER_GENERATION,
                collectorName, generationName);
    }

    private void doUpdateCollectorData() {
        Map<String, List<IntervalTimeData<Double>>> dataToAdd = new HashMap<>();
        List<VmGcStat> sortedList = gcDao.getLatestVmGcStats(ref, lastSeenTimeStamp);
        Collections.sort(sortedList, new TimeStampedPojoComparator<>());

        for (VmGcStat stat : sortedList) {
            String collector = stat.getCollectorName();
            List<IntervalTimeData<Double>> data = dataToAdd.get(collector);
            if (data == null) {
                data = new ArrayList<>();
                dataToAdd.put(collector, data);
            }
            if (lastValueSeen.containsKey(collector)) {
                if (stat.getTimeStamp() <= lastValueSeen.get(collector).getTimeStamp()) {
                    System.out.println("new gc collector value is older than previous value");
                }
                VmGcStat last = lastValueSeen.get(collector);
                lastSeenTimeStamp = Math.max(lastSeenTimeStamp, stat.getTimeStamp());
                long diffInMicro = (stat.getWallTime() - last.getWallTime());
                double diffInMillis = diffInMicro / 1000.0;
                // TODO there is not much point in adding data when diff is 0,
                // but we need to make the chart scroll automatically based on
                // the current time when we do that
                //  if (diff != 0) {
                data.add(new IntervalTimeData<>(last.getTimeStamp(), stat.getTimeStamp(), diffInMillis));
                // }
            }
            lastValueSeen.put(collector, stat);
        }
        for (Map.Entry<String, List<IntervalTimeData<Double>>> entry : dataToAdd.entrySet()) {
            String name = entry.getKey();
            if (!addedCollectors.contains(name)) {
                view.addChart(name, chartName(name, getCollectorGeneration(name)), "ms");
                addedCollectors.add(name);
            }
            view.addData(entry.getKey(), entry.getValue());
        }
    }

    public String getCollectorGeneration(String collectorName) {
        VmMemoryStat info = memDao.getLatestMemoryStat(ref);

        for (Generation g: info.getGenerations()) {
            if (g.getCollector().equals(collectorName)) {
                return g.getName();
            }
        }
        return translator.localize(LocaleResources.UNKNOWN_GEN);
    }

    public UIComponent getView() {
        return (UIComponent) view;
    }

    @Override
    public String getLocalizedName() {
        return translator.localize(LocaleResources.VM_INFO_TAB_GC);
    }

}

