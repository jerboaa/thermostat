/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView.VmDeadLockViewAction;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class VmDeadLockController {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private VmDeadLockView view;
    private ThreadCollector collector;
    private Timer timer;

    private final AtomicReference<String> descriptionRef =  new AtomicReference<>("");

    public VmDeadLockController(VmDeadLockView view, ThreadCollector collector, Timer timer) {
        this.view = view;
        this.collector = collector;
        this.timer = timer;
    }

    public void initialize() {
        view.addVmDeadLockViewActionListener(new ActionListener<VmDeadLockViewAction>() {
            @Override
            public void actionPerformed(ActionEvent<VmDeadLockViewAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                case CHECK_FOR_DEADLOCK:
                    checkForDeadLock();
                    updateView();
                    break;
                default:
                    break;
                }
            }
        });

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                checkStorageForDeadLockData();
                updateView();
            }
        });
        timer.setDelay(5);
        timer.setInitialDelay(0);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_DELAY);

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
                }
            }
        });
    }

    private void checkForDeadLock() {
        askAgentToCheckForDeadLock();
        checkStorageForDeadLockData();
    }

    private void askAgentToCheckForDeadLock() {
        collector.requestDeadLockCheck();
    }

    private void checkStorageForDeadLockData() {
        VmDeadLockData data = collector.getLatestDeadLockData();
        if (data == null) {
            // no deadlock data; so don't update anything
            return;
        }

        String description = data.getDeadLockDescription();
        if (description.equals(VmDeadLockData.NO_DEADLOCK)) {
            description = translate.localize(LocaleResources.NO_DEADLOCK_DETECTED).getContents();
        }
        this.descriptionRef.set(description);

    }

    private void updateView() {
        view.setDeadLockInformation(descriptionRef.get());
    }

}

