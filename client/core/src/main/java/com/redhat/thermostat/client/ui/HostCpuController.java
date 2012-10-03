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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.locale.Translate;
import com.redhat.thermostat.client.osgi.service.BasicView;
import com.redhat.thermostat.client.osgi.service.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.HostInfo;

public class HostCpuController {

    private final HostCpuView view;
    private final Timer backgroundUpdateTimer;

    private final HostInfoDAO hostInfoDAO;
    private final CpuStatDAO cpuStatDAO;
    private final HostRef ref;

    private int chartsAdded = 0;
    private long lastSeenTimeStamp = Long.MIN_VALUE;

    public HostCpuController(HostInfoDAO hostInfoDao, HostRef ref) {
        this.ref = ref;
        view = ApplicationContext.getInstance().getViewFactory().getView(HostCpuView.class);
        view.clearCpuUsageData();
        DAOFactory daos = ApplicationContext.getInstance().getDAOFactory();
        this.hostInfoDAO = hostInfoDao;
        cpuStatDAO = daos.getCpuStatDAO();

        backgroundUpdateTimer = ApplicationContext.getInstance().getTimerFactory().createTimer();
        backgroundUpdateTimer.setAction(new Runnable() {

            @Override
            public void run() {
                updateView();
            }

        });
        backgroundUpdateTimer.setInitialDelay(0);
        backgroundUpdateTimer.setDelay(5);
        backgroundUpdateTimer.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdateTimer.setSchedulingType(SchedulingType.FIXED_RATE);

        view.addActionListener(new ActionListener<HostCpuView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case VISIBLE:
                        start();
                        break;
                    case HIDDEN:
                        stop();
                        break;
                    default:
                        throw new NotImplementedException("unhandled action: " + actionEvent.getActionId());
                }
            }
        });

    }

    // TODO: Consider doing this in a background thread (move to view and use SwingWorker or such).
    private void updateView() {
        HostInfo hostInfo = hostInfoDAO.getHostInfo(ref);

        view.setCpuCount(String.valueOf(hostInfo.getCpuCount()));
        view.setCpuModel(hostInfo.getCpuModel());

        doCpuChartUpdate();
    }

    private void start() {
        backgroundUpdateTimer.start();
    }

    private void stop() {
        backgroundUpdateTimer.stop();
    }

    private void doCpuChartUpdate() {
        List<CpuStat> cpuStats = cpuStatDAO.getLatestCpuStats(ref, lastSeenTimeStamp);
        List<List<DiscreteTimeData<Double>>> results = new ArrayList<>();
        for (CpuStat stat : cpuStats) {
            List<Double> data = stat.getPerProcessorUsage();
            for (int i = 0 ; i < data.size(); i++) {
                if (results.size() == i) {
                    results.add(new ArrayList<DiscreteTimeData<Double>>());
                }
                results.get(i).add(new DiscreteTimeData<Double>(stat.getTimeStamp(), data.get(i)));
                lastSeenTimeStamp = Math.max(lastSeenTimeStamp, stat.getTimeStamp());
            }
        }

        for (int i = 0; i < results.size(); i++) {
            if (i == chartsAdded) {
                view.addCpuUsageChart(i, Translate.localize(LocaleResources.HOST_CPU_ID, String.valueOf(i)));
                chartsAdded++;
            }
            view.addCpuUsageData(i, results.get(i));
        }
    }

    public BasicView getView() {
        return view;
    }

}
