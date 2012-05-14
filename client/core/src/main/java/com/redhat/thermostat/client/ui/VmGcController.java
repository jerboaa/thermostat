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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.VmGcView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmGcStatDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;

class VmGcController {

    private final VmRef ref;
    private final VmGcView view;

    private final VmGcStatDAO gcDao;
    private final VmMemoryStatDAO memDao;

    private final Set<String> addedCollectors = new TreeSet<String>();

    private final Timer timer;

    public VmGcController(VmRef ref) {
        this.ref = ref;
        this.view = ApplicationContext.getInstance().getViewFactory().getView(VmGcView.class);
        this.timer = ApplicationContext.getInstance().getTimerFactory().createTimer();

        DAOFactory df = ApplicationContext.getInstance().getDAOFactory();
        gcDao = df.getVmGcStatDAO();
        memDao = df.getVmMemoryStatDAO();

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
                doUpdateCollectorData();
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
        return localize(LocaleResources.VM_GC_COLLECTOR_OVER_GENERATION,
                collectorName, generationName);
    }

    private void doUpdateCollectorData() {
        Map<String, List<DiscreteTimeData<? extends Number>>> dataToAdd = new HashMap<>();
        for (VmGcStat stat : gcDao.getLatestVmGcStats(ref)) {
            double walltime = 1.0E-6 * stat.getWallTime();
            String collector = stat.getCollectorName();
            List<DiscreteTimeData<? extends Number>> data = dataToAdd.get(collector);
            if (data == null) {
                data = new ArrayList<DiscreteTimeData<? extends Number>>();
                dataToAdd.put(collector, data);
            }
            data.add(new DiscreteTimeData<Double>(stat.getTimeStamp(), walltime));
        }
        for (Map.Entry<String, List<DiscreteTimeData<? extends Number>>> entry : dataToAdd.entrySet()) {
            String name = entry.getKey();
            if (!addedCollectors.contains(name)) {
                view.addChart(name, chartName(name, getCollectorGeneration(name)));
                addedCollectors.add(name);
            }
            view.addData(entry.getKey(), entry.getValue());
        }
    }

    public String getCollectorGeneration(String collectorName) {
        VmMemoryStat info = memDao.getLatestMemoryStat(ref);

        for (Generation g: info.getGenerations()) {
            if (g.collector.equals(collectorName)) {
                return g.name;
            }
        }
        return localize(LocaleResources.UNKNOWN_GEN);
    }

    /**
     * @return
     */
    public Component getComponent() {
        return view.getUiComponent();
    }

}
