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

package com.redhat.thermostat.numa.client.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.numa.client.core.NumaView;
import com.redhat.thermostat.numa.client.core.NumaViewProvider;
import com.redhat.thermostat.numa.client.locale.LocaleResources;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaNodeStat;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class NumaController implements InformationServiceController<HostRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final NumaView view;

    private final NumaDAO numaDAO;
    private final int numberOfNumaNodes;

    private final HostRef hostRef;

    private long lastSeenTimestamp;
    private NumaStat lastSeenStat;

    public NumaController(ApplicationService appSvc, NumaDAO numaDAO, HostRef hostRef, NumaViewProvider provider) {
        this.hostRef = hostRef;
        this.numaDAO = numaDAO;

        view = provider.createView();

        numberOfNumaNodes = numaDAO.getNumberOfNumaNodes(hostRef);


        if (numberOfNumaNodes > 0) {
            setupTimer(appSvc.getTimerFactory().createTimer());
        }
    }

    private void setupTimer(final Timer timer) {
        for (int i = 0; i < numberOfNumaNodes; i++) {
            view.addChart(translator.localize(LocaleResources.NUMA_NODE, String.valueOf(i)).getContents());
        }

        Duration userDuration = view.getUserDesiredDuration(); //Has default of 10 minutes
        lastSeenTimestamp = System.currentTimeMillis() - userDuration.asMilliseconds();

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                update();
            }
        });

        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setInitialDelay(0);
        timer.setDelay(5);

        view.addActionListener(new com.redhat.thermostat.common.ActionListener<NumaView.Action>() {
            @Override
            public void actionPerformed(com.redhat.thermostat.common.ActionEvent<BasicView.Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        timer.stop();
                        break;
                    case VISIBLE:
                        timer.start();
                        break;
                    default:
                        assert false; // Cannot happen: null is caught in ActionEvent constructor, everything else by javac.
                }
            }
        });

        view.addUserActionListener(new ActionListener<NumaView.UserAction>() {

            @Override
            public void actionPerformed(ActionEvent<NumaView.UserAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case USER_CHANGED_TIME_RANGE:
                        Duration duration = view.getUserDesiredDuration();
                        lastSeenTimestamp = System.currentTimeMillis() - duration.asMilliseconds();
                        view.setVisibleDataRange(duration.getValue(), duration.getUnit());
                        break;
                    default:
                        throw new AssertionError("Unhandled action type");
                }
            }
        });
    }

    private void update() {
        List<NumaStat> numaStats = numaDAO.getLatestNumaStats(hostRef, lastSeenTimestamp); //Sorted by timestamp already
        Map<String, List<DiscreteTimeData<Double>[]>> viewData = getData(numaStats);

        for (Map.Entry<String, List<DiscreteTimeData<Double>[]>> entry : viewData.entrySet()) {
            view.addData(entry.getKey(), entry.getValue());
        }

        lastSeenTimestamp = System.currentTimeMillis();
    }

    private Map<String, List<DiscreteTimeData<Double>[]>> getData(List<NumaStat> numaStats) {
        Map<String, List<DiscreteTimeData<Double>[]>> map = new HashMap<>();
        for (int i = 0; i < numberOfNumaNodes; i++) {
            map.put(translator.localize(LocaleResources.NUMA_NODE, String.valueOf(i)).getContents(), new ArrayList<DiscreteTimeData<Double>[]>());
        }

        if (lastSeenStat != null && lastSeenStat.getTimeStamp() < numaStats.get(0).getTimeStamp()) {
            buildData(map, lastSeenStat, numaStats.get(0));
        }

        for (int i = 1; i < numaStats.size(); i++) {
            NumaStat first = numaStats.get(i - 1);
            NumaStat second = numaStats.get(i);

            buildData(map, first, second);
        }

        lastSeenStat = numaStats.get(numaStats.size() - 1);

        return map;
    }

    private void buildData(Map<String, List<DiscreteTimeData<Double>[]>> map, NumaStat first, NumaStat second) {
        for (int j = 0; j < numberOfNumaNodes; j++) {
            DiscreteTimeData<Double>[] data = buildStat(j, first, second);
            map.get(translator.localize(LocaleResources.NUMA_NODE, String.valueOf(j)).getContents()).add(data);
        }
    }

    private DiscreteTimeData<Double>[] buildStat(int node, NumaStat first, NumaStat second) {
        NumaNodeStat[] firstStat = first.getNodeStats();
        NumaNodeStat[] secondStat = second.getNodeStats();

        DiscreteTimeData<Double>[] data = new DiscreteTimeData[3];

        //Hits
        data[0] = new DiscreteTimeData<>(second.getTimeStamp(),
                (double)(secondStat[node].getNumaHit() - firstStat[node].getNumaHit()) /
                        (second.getTimeStamp() - first.getTimeStamp()));
        //Misses
        data[1] = new DiscreteTimeData<>(second.getTimeStamp(),
                (double)(secondStat[node].getNumaMiss() - firstStat[node].getNumaMiss()) /
                        (second.getTimeStamp() - first.getTimeStamp()));
        //Foreign hits
        data[2] = new DiscreteTimeData<>(second.getTimeStamp(),
                (double)(secondStat[node].getNumaForeign() - firstStat[node].getNumaForeign()) /
                        (second.getTimeStamp() - first.getTimeStamp()));

        return data;
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.NUMA_TAB);
    }
}
