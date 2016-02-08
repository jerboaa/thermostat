/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.classstat.client.core.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.experimental.TimeRangeController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Size.Unit;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.classstat.client.core.VmClassStatView;
import com.redhat.thermostat.vm.classstat.client.core.VmClassStatViewProvider;
import com.redhat.thermostat.vm.classstat.client.locale.LocaleResources;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;

public class VmClassStatController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private TimeRangeController<VmClassStat, VmRef> timeRangeController;

    private class UpdateChartData implements Runnable {
        @Override
        public void run() {
            VmClassStat oldest = dao.getOldest(ref);
            VmClassStat newest = dao.getNewest(ref);
            // Do nothing when there is no data
            if (oldest == null || newest == null) {
                return;
            }
            
            final List<DiscreteTimeData<? extends Number>> loadedClasses = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> loadedBytes = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> unloadedClasses = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> unloadedBytes = new LinkedList<>();

            Range<Long> newAvailableRange = new Range<>(oldest.getTimeStamp(), newest.getTimeStamp());

            TimeRangeController.StatsSupplier<VmClassStat, VmRef> singleValueSupplier = new TimeRangeController.StatsSupplier<VmClassStat, VmRef>() {
                @Override
                public List<VmClassStat> getStats(final VmRef ref, final long since, final long to) {
                    return dao.getClassStats(ref, since, to);
                }
            };

            TimeRangeController.SingleArgRunnable<VmClassStat> runnable = new TimeRangeController.SingleArgRunnable<VmClassStat>() {
                @Override
                public void run(VmClassStat stat) {
                    long timeStamp = stat.getTimeStamp();

                    loadedClasses.add(new DiscreteTimeData<Number>(timeStamp, stat.getLoadedClasses()));
                    loadedBytes.add(new DiscreteTimeData<Number>(timeStamp, bytesToMegaBytes(stat.getLoadedBytes())));
                    unloadedClasses.add(new DiscreteTimeData<Number>(timeStamp, stat.getUnloadedClasses()));
                    unloadedBytes.add(new DiscreteTimeData<Number>(timeStamp, bytesToMegaBytes(stat.getUnloadedBytes())));
                }

                private long bytesToMegaBytes(long bytes) {
                    return (long) Size.bytes(bytes).convertTo(Unit.MiB).getValue();
                }
            };

            timeRangeController.update(userDesiredDuration, newAvailableRange, singleValueSupplier, ref, runnable);
            classesView.setAvailableDataRange(timeRangeController.getAvailableRange());

            classesView.addClassData(VmClassStatView.TAG_LOADED_CLASSES, loadedClasses);
            classesView.addClassData(VmClassStatView.TAG_LOADED_BYTES, loadedBytes);
            classesView.addClassData(VmClassStatView.TAG_UNLOADED_CLASSES, unloadedClasses);
            classesView.addClassData(VmClassStatView.TAG_UNLOADED_BYTES, unloadedBytes);
        }
    }

    private final VmClassStatView classesView;
    private final VmRef ref;
    private final VmClassStatDAO dao;
    private final Timer timer;

    private Duration userDesiredDuration;

    public VmClassStatController(ApplicationService appSvc, VmClassStatDAO vmClassStatDao, VmRef ref, VmClassStatViewProvider viewProvider) {
        this.ref = ref;
        dao = vmClassStatDao;
        timer = appSvc.getTimerFactory().createTimer();

        timer.setAction(new UpdateChartData());
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setDelay(5);
        timer.setInitialDelay(0);

        classesView = viewProvider.createView();
        classesView.addClassChart(VmClassStatView.Group.NUMBER, VmClassStatView.TAG_LOADED_CLASSES,
                translator.localize(LocaleResources.VM_CLASSES_CHART_LOADED_CLASSES_LENGEND));
        classesView.addClassChart(VmClassStatView.Group.SIZE, VmClassStatView.TAG_LOADED_BYTES,
                translator.localize(LocaleResources.VM_CLASSES_CHART_LOADED_BYTES_LENGEND));
        classesView.addClassChart(VmClassStatView.Group.NUMBER, VmClassStatView.TAG_UNLOADED_CLASSES,
                translator.localize(LocaleResources.VM_CLASSES_CHART_UNLOADED_CLASSES_LENGEND));
        classesView.addClassChart(VmClassStatView.Group.SIZE, VmClassStatView.TAG_UNLOADED_BYTES,
                translator.localize(LocaleResources.VM_CLASSES_CHART_UNLOADED_BYTES_LENGEND));

        classesView.addActionListener(new ActionListener<VmClassStatView.Action>() {
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
                        throw new NotImplementedException("unknown action: " + actionEvent.getActionId());
                }
            }
        });

        userDesiredDuration = classesView.getUserDesiredDuration();

        timeRangeController = new TimeRangeController<>();
    }

    private void start() {
        timer.start();
    }

    private void stop() {
        timer.stop();
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VM_INFO_TAB_CLASSES);
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) classesView;
    }

}

