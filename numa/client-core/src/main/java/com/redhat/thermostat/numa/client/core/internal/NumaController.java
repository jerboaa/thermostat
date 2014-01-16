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

package com.redhat.thermostat.numa.client.core.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.numa.client.core.NumaView;
import com.redhat.thermostat.numa.client.core.NumaView.GraphVisibilityChangeListener;
import com.redhat.thermostat.numa.client.core.NumaViewProvider;
import com.redhat.thermostat.numa.client.locale.LocaleResources;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class NumaController implements InformationServiceController<HostRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final NumaView view;

    private final NumaDAO numaDAO;
    private final HostRef ref;

    private final Timer backgroundUpdateTimer;
    private final GraphVisibilityChangeListener listener = new ShowHideGraph();

    private long lastSeenTimeStamp = Long.MIN_VALUE;

    private int numberOfNumaNodes;

    public NumaController(ApplicationService appSvc, NumaDAO numaDAO, final HostRef ref, NumaViewProvider provider) {
        this.ref = ref;
        this.numaDAO = numaDAO;

        numberOfNumaNodes = numaDAO.getNumberOfNumaNodes(ref);

        view = provider.createView();

        for (int i = 0; i < numberOfNumaNodes; i++) {
            view.addNumaChart("node" + i, translator.localize(LocaleResources.NUMA_NODE, String.valueOf(i)));
        }
        view.addGraphVisibilityListener(listener);
        view.addActionListener(new ActionListener<NumaView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        stopBackgroundUpdates();
                        break;
                    case VISIBLE:
                        startBackgroundUpdates();
                        break;
                    default:
                        assert false; // Cannot happen: null is caught in ActionEvent constructor, everything else by javac.
                }
            }
        });

        backgroundUpdateTimer = appSvc.getTimerFactory().createTimer();
        backgroundUpdateTimer.setAction(new Runnable() {
            @Override
            public void run() {
                doNumaChartUpdate();
            }
        });
        backgroundUpdateTimer.setSchedulingType(SchedulingType.FIXED_RATE);
        backgroundUpdateTimer.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdateTimer.setInitialDelay(0);
        backgroundUpdateTimer.setDelay(5);
    }

    private void startBackgroundUpdates() {
        for (int i = 0; i < numberOfNumaNodes; i++) {
            view.showNumaChart("node" + i);
        }

        backgroundUpdateTimer.start();
    }

    private void stopBackgroundUpdates() {
        backgroundUpdateTimer.stop();
        for (int i = 0; i < numberOfNumaNodes; i++) {
            view.hideNumaChart("node" + i);
        }
    }

    public UIComponent getView() {
        return view;
    }

    private void doNumaChartUpdate() {
        List<NumaStat> stats = numaDAO.getLatestNumaStats(ref, lastSeenTimeStamp);
        for (int i = 0; i < numberOfNumaNodes; i++) {
            List<DiscreteTimeData<? extends Number>> numaHitRatio = new LinkedList<>();

            for (NumaStat stat : stats) {
                long timeStamp = stat.getTimeStamp();
                long numaHitVal = stat.getNodeStats()[i].getNumaHit();
                long numaMissVal = stat.getNodeStats()[i].getNumaMiss();
                double hitRatio = 100 * numaHitVal / (numaHitVal + numaMissVal);
                numaHitRatio.add(new DiscreteTimeData<Double>(timeStamp, hitRatio));
                lastSeenTimeStamp = Math.max(lastSeenTimeStamp, stat.getTimeStamp());
            }

            view.addNumaData("node" + i, numaHitRatio);
        }
    }

    private class ShowHideGraph implements GraphVisibilityChangeListener {
        @Override
        public void show(String tag) {
            view.showNumaChart(tag);
        }
        @Override
        public void hide(String tag) {
            view.hideNumaChart(tag);
        }
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.NUMA_TAB);
    }
}

