/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.host.memory.client.core.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView;
import com.redhat.thermostat.host.memory.client.core.HostMemoryViewProvider;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView.GraphVisibilityChangeListener;
import com.redhat.thermostat.host.memory.client.locale.LocaleResources;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.storage.model.MemoryStat;
import com.redhat.thermostat.storage.model.MemoryType;

public class HostMemoryController implements InformationServiceController<HostRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final HostMemoryView view;

    private final HostInfoDAO hostInfoDAO;
    private final MemoryStatDAO memoryStatDAO;
    private final HostRef ref;

    private final Timer backgroundUpdateTimer;
    private final GraphVisibilityChangeListener listener = new ShowHideGraph();

    private long lastSeenTimeStamp = Long.MIN_VALUE;

    public HostMemoryController(ApplicationService appSvc, HostInfoDAO hostInfoDAO, MemoryStatDAO memoryStatDAO, final HostRef ref, HostMemoryViewProvider provider) {
        this.ref = ref;
        this.hostInfoDAO = hostInfoDAO;
        this.memoryStatDAO = memoryStatDAO;

        view = provider.createView();

        view.addMemoryChart(MemoryType.MEMORY_TOTAL.name(), translator.localize(LocaleResources.HOST_MEMORY_TOTAL));
        view.addMemoryChart(MemoryType.MEMORY_FREE.name(), translator.localize(LocaleResources.HOST_MEMORY_FREE));
        view.addMemoryChart(MemoryType.MEMORY_USED.name(), translator.localize(LocaleResources.HOST_MEMORY_USED));
        view.addMemoryChart(MemoryType.SWAP_TOTAL.name(), translator.localize(LocaleResources.HOST_SWAP_TOTAL));
        view.addMemoryChart(MemoryType.SWAP_FREE.name(), translator.localize(LocaleResources.HOST_SWAP_FREE));
        view.addMemoryChart(MemoryType.BUFFERS.name(), translator.localize(LocaleResources.HOST_BUFFERS));

        view.addGraphVisibilityListener(listener);
        view.addActionListener(new ActionListener<HostMemoryView.Action>() {
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
                        throw new NotImplementedException("action event not handled: " + actionEvent.getActionId());
                }
            }
        });

        backgroundUpdateTimer = appSvc.getTimerFactory().createTimer();
        backgroundUpdateTimer.setAction(new Runnable() {
            @Override
            public void run() {
                long memorySize = HostMemoryController.this.hostInfoDAO.getHostInfo(ref).getTotalMemory();
                view.setTotalMemory(Size.bytes(memorySize).toString());
                doMemoryChartUpdate();
            }
        });
        backgroundUpdateTimer.setSchedulingType(SchedulingType.FIXED_RATE);
        backgroundUpdateTimer.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdateTimer.setInitialDelay(0);
        backgroundUpdateTimer.setDelay(5);
    }

    private void startBackgroundUpdates() {
        for (MemoryType type : MemoryType.values()) {
            view.showMemoryChart(type.name());
        }

        backgroundUpdateTimer.start();
    }

    private void stopBackgroundUpdates() {
        backgroundUpdateTimer.stop();
        for (MemoryType type : MemoryType.values()) {
            view.hideMemoryChart(type.name());
        }
    }

    public UIComponent getView() {
        return view;
    }

    private void doMemoryChartUpdate() {
        List<DiscreteTimeData<? extends Number>> memFree = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> memTotal = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> memUsed = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> buf = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> swapTotal = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> swapFree = new LinkedList<>();

        for (MemoryStat stat : memoryStatDAO.getLatestMemoryStats(ref, lastSeenTimeStamp)) {
            long timeStamp = stat.getTimeStamp();
            memFree.add(new DiscreteTimeData<Long>(timeStamp, stat.getFree()));
            memTotal.add(new DiscreteTimeData<Long>(timeStamp, stat.getTotal()));
            memUsed.add(new DiscreteTimeData<Long>(timeStamp, stat.getTotal() - stat.getFree()));
            buf.add(new DiscreteTimeData<Long>(timeStamp, stat.getBuffers()));
            swapTotal.add(new DiscreteTimeData<Long>(timeStamp, stat.getSwapTotal()));
            swapFree.add(new DiscreteTimeData<Long>(timeStamp, stat.getSwapFree()));
            lastSeenTimeStamp = Math.max(lastSeenTimeStamp, stat.getTimeStamp());
        }

        view.addMemoryData(MemoryType.MEMORY_FREE.name(), memFree);
        view.addMemoryData(MemoryType.MEMORY_TOTAL.name(), memTotal);
        view.addMemoryData(MemoryType.MEMORY_USED.name(), memUsed);
        view.addMemoryData(MemoryType.BUFFERS.name(), buf);
        view.addMemoryData(MemoryType.SWAP_FREE.name(), swapFree);
        view.addMemoryData(MemoryType.SWAP_TOTAL.name(), swapTotal);
    }

    private class ShowHideGraph implements GraphVisibilityChangeListener {
        @Override
        public void show(String tag) {
            view.showMemoryChart(tag);
        }
        @Override
        public void hide(String tag) {
            view.hideMemoryChart(tag);
        }
    }

    @Override
    public String getLocalizedName() {
        return translator.localize(LocaleResources.HOST_INFO_TAB_MEMORY);
    }
}

