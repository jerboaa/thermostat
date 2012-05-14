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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.Component;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.VmOverviewView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;

class VmOverviewController {

    private final VmRef ref;
    private final VmInfoDAO dao;
    private final DateFormat vmRunningTimeFormat;

    private final Timer timer;

    private final VmOverviewView view;

    public VmOverviewController(VmRef vmRef) {
        this.ref = vmRef;
        this.view = ApplicationContext.getInstance().getViewFactory().getView(VmOverviewView.class);

        dao = ApplicationContext.getInstance().getDAOFactory().getVmInfoDAO();
        timer = ApplicationContext.getInstance().getTimerFactory().createTimer();

        vmRunningTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);

        view.addActionListener(new ActionListener<VmOverviewView.Action>() {
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

        timer.setAction(new Runnable() {

            @Override
            public void run() {
                VmInfo info = dao.getVmInfo(ref);
                view.setVmPid(((Integer) info.getVmPid()).toString());
                long actualStartTime = info.getStartTimeStamp();
                view.setVmStartTimeStamp(vmRunningTimeFormat.format(new Date(actualStartTime)));
                long actualStopTime = info.getStopTimeStamp();
                if (actualStopTime >= actualStartTime) {
                    // Only show a stop time if we have actually stopped.
                    view.setVmStopTimeStamp(vmRunningTimeFormat.format(new Date(actualStopTime)));
                } else {
                    view.setVmStopTimeStamp(localize(LocaleResources.VM_INFO_RUNNING));
                }
                view.setJavaVersion(info.getJavaVersion());
                view.setJavaHome(info.getJavaHome());
                view.setMainClass(info.getMainClass());
                view.setJavaCommandLine(info.getJavaCommandLine());
                String actualVmName = info.getVmName();
                view.setVmInfo(info.getVmInfo());
                String actualVmVersion = info.getVmInfo();
                view.setVmNameAndVersion(localize(LocaleResources.VM_INFO_VM_NAME_AND_VERSION,
                        actualVmName, actualVmVersion));
                view.setVmArguments(info.getVmArguments());
            }
        });
        timer.setInitialDelay(0);
        timer.setDelay(5);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
    }

    private void start() {
        timer.start();
    }

    private void stop() {
        timer.stop();
    }

    public Component getComponent() {
        return view.getUiComponent();
    }
}
