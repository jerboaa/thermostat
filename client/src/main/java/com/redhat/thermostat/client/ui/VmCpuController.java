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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.VmCpuStat;

public class VmCpuController implements AsyncUiFacade {

    private final VmRef ref;
    private final VmCpuStatDAO dao;
    private final VmCpuView view;

    private final Timer timer = new Timer();

    public VmCpuController(VmRef ref) {
        this.ref = ref;
        dao = ApplicationContext.getInstance().getDAOFactory().getVmCpuStatDAO();
        view = createView();
    }

    protected VmCpuView createView() {
        return new VmCpuPanel();
    }

    @Override
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doUpdateVmCpuCharts();
            }

        }, 0, TimeUnit.SECONDS.toMillis(5));

    }

    private void doUpdateVmCpuCharts() {
        List<VmCpuStat> stats = dao.getLatestVmCpuStats(ref);
        List<DiscreteTimeData<? extends Number>> toDisplay = new ArrayList<>(stats.size());
        for (VmCpuStat stat: stats) {
            DiscreteTimeData<? extends Number> data =
                    new DiscreteTimeData<Number>(stat.getTimeStamp(), stat.getCpuLoad());
            toDisplay.add(data);
        }

        view.addData(toDisplay);
    }

    @Override
    public void stop() {
        timer.cancel();
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

}
