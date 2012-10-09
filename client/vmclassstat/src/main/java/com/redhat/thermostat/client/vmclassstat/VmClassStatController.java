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

package com.redhat.thermostat.client.vmclassstat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.VmInformationServiceController;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.vmclassstat.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.VmClassStat;

class VmClassStatController implements VmInformationServiceController {

    private class UpdateChartData implements Runnable {
        @Override
        public void run() {
            long timeStamp = lastSeenTimeStamp;
            List<VmClassStat> latestClassStats = dao.getLatestClassStats(ref, timeStamp);
            List<DiscreteTimeData<Long>> timeData = new ArrayList<>();
            for (VmClassStat stat : latestClassStats) {
                timeData.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), stat.getLoadedClasses()));
                timeStamp = Math.max(timeStamp, stat.getTimeStamp());
            }
            classesView.addClassCount(timeData);
            lastSeenTimeStamp = timeStamp;
        }
    }

    private final VmClassStatView classesView;
    private final VmRef ref;
    private final VmClassStatDAO dao;
    private final Timer timer;
    private final Translate t = LocaleResources.createLocalizer();

    private volatile long lastSeenTimeStamp = Long.MIN_VALUE;

    public VmClassStatController(VmClassStatDAO vmClassStatDao, VmRef ref, VmClassStatViewProvider viewProvider) {
        this.ref = ref;
        dao = vmClassStatDao;
        timer = ApplicationContext.getInstance().getTimerFactory().createTimer();

        timer.setAction(new UpdateChartData());
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setDelay(5);
        timer.setInitialDelay(0);

        classesView = viewProvider.createView();

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
    }

    private void start() {
        timer.start();
    }

    private void stop() {
        timer.stop();
    }

    @Override
    public String getLocalizedName() {
        return t.localize(LocaleResources.VM_INFO_TAB_CLASSES);
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) classesView;
    }

}
