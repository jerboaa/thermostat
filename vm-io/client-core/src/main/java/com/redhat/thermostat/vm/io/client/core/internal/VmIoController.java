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

package com.redhat.thermostat.vm.io.client.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.core.experimental.TimeRangeController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
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
import com.redhat.thermostat.vm.io.client.core.LocaleResources;
import com.redhat.thermostat.vm.io.client.core.VmIoView;
import com.redhat.thermostat.vm.io.client.core.VmIoViewProvider;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final VmRef ref;
    private final VmIoStatDAO dao;
    private final VmIoView view;

    private final Timer timer;

    private TimeRangeController<VmIoStat, VmRef> timeRangeController;

    public VmIoController(ApplicationService appSvc, VmIoStatDAO vmIoStatDao, VmRef ref, VmIoViewProvider provider) {
        this.ref = ref;
        dao = vmIoStatDao;
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

        view.addActionListener(new ActionListener<VmIoView.Action>() {
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

        timeRangeController = new TimeRangeController<>();
    }

    private void start() {
        timer.start();
    }

    private void updateData() {
        VmIoStat oldest = dao.getOldest(ref);
        VmIoStat newest = dao.getNewest(ref);
        // Do nothing if there is no data
        if (oldest == null || newest == null) {
            return;
        }
        
        final List<VmIoStat> data = new ArrayList<>();


        Range<Long> newAvailableRange = new Range<>(oldest.getTimeStamp(), newest.getTimeStamp());

        TimeRangeController.StatsSupplier<VmIoStat, VmRef> singleValueSupplier = new TimeRangeController.StatsSupplier<VmIoStat, VmRef>() {
            @Override
            public List<VmIoStat> getStats(final VmRef ref, final long since, final long to) {
                return dao.getVmIoStats(ref, since, to);
           }
        };

        TimeRangeController.SingleArgRunnable<VmIoStat> runnable = new TimeRangeController.SingleArgRunnable<VmIoStat>() {
            @Override
            public void run(VmIoStat ioData) {
                data.add(ioData);
            }
        };

        Duration duration = view.getUserDesiredDuration();
        if (duration != null) { // only update view if it is working correctly
            timeRangeController.update(view.getUserDesiredDuration(), newAvailableRange, singleValueSupplier, ref, runnable);
            view.setAvailableDataRange(timeRangeController.getAvailableRange());
            view.addData(data);
        }
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
        return translator.localize(LocaleResources.VM_INFO_TAB_IO);
    }
}

