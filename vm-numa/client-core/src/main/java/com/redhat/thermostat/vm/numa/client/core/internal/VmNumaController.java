/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.numa.client.core.internal;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.numa.client.core.VmNumaView;
import com.redhat.thermostat.vm.numa.client.core.VmNumaViewProvider;
import com.redhat.thermostat.vm.numa.client.core.locale.LocaleResources;
import com.redhat.thermostat.vm.numa.common.NumaMemoryLocations;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private VmNumaView view;

    private VmId vmId;
    private AgentId agentId;
    private VmNumaDAO vmNumaDAO;

    private long lastSeenTimestamp = Long.MIN_VALUE;

    public VmNumaController(ApplicationService appSvc, NumaDAO numaDAO, VmNumaDAO vmNumaDAO, VmId vmId, AgentId agentId, VmNumaViewProvider vmNumaViewProvider) {
        this.vmId = vmId;
        this.agentId = agentId;

        this.vmNumaDAO = vmNumaDAO;

        int numNumaNodes = numaDAO.getNumberOfNumaNodes(new HostRef(agentId.get(), ""));

        this.view = vmNumaViewProvider.createView();

        if (numNumaNodes > 1) {
            for (NumaMemoryLocations location : NumaMemoryLocations.values()) {
                view.addChart(numNumaNodes, location.getName());
            }

            setupTimer(appSvc.getTimerFactory().createTimer());

            view.addUserActionListener(new ActionListener<VmNumaView.UserAction>() {
                @Override
                public void actionPerformed(ActionEvent<VmNumaView.UserAction> actionEvent) {
                    switch (actionEvent.getActionId()) {
                        case USER_CHANGED_TIME_RANGE:
                            Duration duration = view.getUserDesiredDuration();
                            lastSeenTimestamp = System.currentTimeMillis() - duration.asMilliseconds();
                            view.setVisibleDataRange(duration);
                            break;
                        default:
                            throw new AssertionError("Unhandled action type: " + actionEvent.getActionId());
                    }
                }
            });
        } else {
            view.showNumaUnavailable();
        }
    }

    private void setupTimer(final Timer timer) {
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setInitialDelay(0);
        timer.setDelay(5);

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                updateData();
            }
        });

        view.addActionListener(new com.redhat.thermostat.common.ActionListener<VmNumaView.Action>() {
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
                        throw new AssertionError("Unhandled action type");
                }
            }
        });
    }

    private void updateData() {
        List<VmNumaStat> stats = vmNumaDAO.getNumaStats(agentId, vmId, lastSeenTimestamp, System.currentTimeMillis());

        if (stats.size() > 0) {
            lastSeenTimestamp = stats.get(stats.size() - 1).getTimeStamp();
        }

        for(VmNumaStat stat : stats) {
            VmNumaNodeStat[] nodeStats = stat.getVmNodeStats();
            for (VmNumaNodeStat nodeStat : nodeStats) {
                int node = nodeStat.getNode();
                long timestamp = stat.getTimeStamp();
                view.addData(NumaMemoryLocations.HEAP.getName(), node,
                        new DiscreteTimeData<>(timestamp, nodeStat.getHeapMemory()));
                view.addData(NumaMemoryLocations.HUGE.getName(), node,
                        new DiscreteTimeData<>(timestamp, nodeStat.getHugeMemory()));
                view.addData(NumaMemoryLocations.PRIVATE.getName(), node,
                        new DiscreteTimeData<>(timestamp, nodeStat.getPrivateMemory()));
                view.addData(NumaMemoryLocations.STACK.getName(), node,
                        new DiscreteTimeData<>(timestamp, nodeStat.getStackMemory()));
            }
        }
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VM_NUMA_TITLE);
    }


}
