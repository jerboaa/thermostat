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

package com.redhat.thermostat.vm.cpu.client.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.core.experimental.TimeRangeController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuView;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuViewProvider;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuView.UserAction;
import com.redhat.thermostat.vm.cpu.client.locale.LocaleResources;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final VmRef ref;
    private final VmCpuStatDAO dao;
    private final VmCpuView view;

    private final Timer timer;

    private Duration userDesiredDuration;

    private TimeRangeController<VmCpuStat, VmRef> timeRangeController;

    public VmCpuController(ApplicationService appSvc, VmCpuStatDAO vmCpuStatDao, VmRef ref, VmCpuViewProvider provider) {
        this.ref = ref;
        dao = vmCpuStatDao;
        timer = appSvc.getTimerFactory().createTimer();

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                updateData();
            }
        });
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setDelay(5);
        timer.setInitialDelay(0);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);

        view = provider.createView();

        view.addActionListener(new ActionListener<VmCpuView.Action>() {
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
                        throw new NotImplementedException("unknown event : " + actionEvent.getActionId());
                }
            }
        });

        view.addUserActionListener(new ActionListener<VmCpuView.UserAction>() {

            @Override
            public void actionPerformed(ActionEvent<UserAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                case USER_CHANGED_TIME_RANGE:
                    Duration duration = view.getUserDesiredDuration();
                    userDesiredDuration = duration;
                    view.setVisibleDataRange(duration.value, duration.unit);
                    break;
                default:
                    throw new AssertionError("Unhandled action type");
                }
            }
        });

        userDesiredDuration = view.getUserDesiredDuration();

        timeRangeController = new TimeRangeController<>();
    }

    private void start() {
        timer.start();
    }

    private void updateData() {
        final List<DiscreteTimeData<? extends Number>> data = new ArrayList<>();

        VmCpuStat oldest = dao.getOldest(ref);
        VmCpuStat newest = dao.getNewest(ref);

        Range<Long> newAvailableRange = new Range<>(oldest.getTimeStamp(), newest.getTimeStamp());

        TimeRangeController.StatsSupplier<VmCpuStat, VmRef> singleValueSupplier = new TimeRangeController.StatsSupplier<VmCpuStat, VmRef>() {
            @Override
            public List<VmCpuStat> getStats(final VmRef ref, final long since, final long to) {
                return dao.getVmCpuStats(ref, since, to);

           }
        };

        TimeRangeController.SingleArgRunnable<VmCpuStat> runnable = new TimeRangeController.SingleArgRunnable<VmCpuStat>() {
            @Override
            public void run(VmCpuStat arg) {
                data.add(new DiscreteTimeData<>(arg.getTimeStamp(), arg.getCpuLoad()));
            }
        };

        timeRangeController.update(userDesiredDuration, newAvailableRange, singleValueSupplier, ref, runnable);
        view.setAvailableDataRange(timeRangeController.getAvailableRange());
        view.addData(data);
    }

    private void stop() {
        timer.stop();
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VM_INFO_TAB_CPU);
    }
}

