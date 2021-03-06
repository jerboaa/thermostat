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

package com.redhat.thermostat.vm.memory.client.core.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.experimental.TimeRangeController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView.Type;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsViewProvider;
import com.redhat.thermostat.vm.memory.client.core.Payload;
import com.redhat.thermostat.vm.memory.client.core.StatsModel;
import com.redhat.thermostat.vm.memory.client.locale.LocaleResources;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.VmTlabStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.vm.memory.common.model.VmTlabStat;

public class MemoryStatsController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();
    private static final Duration defaultDuration = new Duration(10, TimeUnit.MINUTES);

    private final MemoryStatsView view;
    private final VmMemoryStatDAO vmDao;
    private final VmTlabStatDAO tlabStatDao;

    private final VmRef ref;
    private final Timer memoryStatsTimer;
    private final Timer tlabStatsTimer;

    private final Map<String, Payload> regions;
    
    private VmMemoryStatCollector statCollector;
    private VmTlabStatCollector tlabCollector;

    private Duration userDesiredDuration = defaultDuration;

    private TimeRangeController<VmMemoryStat, VmRef> memoryTimeRangeController;
    private TimeRangeController<VmTlabStat, VmRef> tlabTimeRangeController;

    class VmMemoryStatCollector implements Runnable {

        private long desiredUpdateTimeStamp = System.currentTimeMillis() - defaultDuration.asMilliseconds();

        @Override
        public void run() {
            VmMemoryStat oldest = vmDao.getOldestMemoryStat(ref);
            VmMemoryStat newest = vmDao.getNewestMemoryStat(ref);
            // Do nothing if there is no data
            if (oldest == null || newest == null) {
                return;
            }

            Range<Long> newAvailableRange = new Range<>(oldest.getTimeStamp(), newest.getTimeStamp());

            TimeRangeController.StatsSupplier<VmMemoryStat, VmRef> statsSupplier = new TimeRangeController.StatsSupplier<VmMemoryStat, VmRef>() {
                @Override
                public List<VmMemoryStat> getStats(VmRef ref, long since, long to) {
                    return vmDao.getVmMemoryStats(ref, since, to);
                }
            };

            TimeRangeController.SingleArgRunnable<VmMemoryStat> runnable = new TimeRangeController.SingleArgRunnable<VmMemoryStat>() {
                @Override
                public void run(VmMemoryStat arg) {
                    update(arg);
                }
            };

            memoryTimeRangeController.update(userDesiredDuration, newAvailableRange, statsSupplier, ref, runnable);
        }

        private void update(VmMemoryStat memoryStats) {
            Generation[] generations = memoryStats.getGenerations();
            for (Generation generation : generations) {
                updateGeneration(generation, memoryStats.getTimeStamp());
            }
            if (memoryStats.isMetaspacePresent()) {
                updateMetaspace(memoryStats.getTimeStamp(),
                        memoryStats.getMetaspaceMaxCapacity(),
                        memoryStats.getMetaspaceMinCapacity(),
                        memoryStats.getMetaspaceCapacity(),
                        memoryStats.getMetaspaceUsed());
            }
        }

        private void updateGeneration(Generation generation, long timeStamp) {
            Space[] spaces = generation.getSpaces();
            for (Space space: spaces) {
                updateSpace(space, timeStamp);
            }
        }

        private void updateSpace(Space space, long timeStamp) {
            Payload payload = getPayload(space.getName());

            updatePayloadModel(payload, timeStamp, space.getUsed());

            setPayloadFields(payload, space.getUsed(), space.getCapacity(), space.getMaxCapacity());

            updateViewAndRegion(payload);

            desiredUpdateTimeStamp = Math.max(desiredUpdateTimeStamp, timeStamp);
        }

        private void updateMetaspace(long timeStamp, long maxCapacity, long minCapacity, long capacity, long used) {
            Payload payload = getPayload(VmMemoryStat.METASPACE_NAME);
            updatePayloadModel(payload, timeStamp, used);
            setPayloadFields(payload, used, capacity, maxCapacity);
            updateViewAndRegion(payload);

            desiredUpdateTimeStamp = Math.max(desiredUpdateTimeStamp, timeStamp);
        }

        private void updateViewAndRegion(Payload payload) {
            if (regions.containsKey(payload.getName())) {
                view.updateRegion(payload.clone());
            } else {
                view.addRegion(payload.clone());
                regions.put(payload.getName(), payload);
            }

            view.requestRepaint();
        }

        private void updatePayloadModel(Payload payload, long timeStamp, long used) {
            StatsModel model = payload.getModel();
            if (model == null) {
                model = new StatsModel(defaultDuration.asMilliseconds());
                model.setName(payload.getName());
            }
            // normalize this always in the same unit
            model.addData(timeStamp, Size.bytes(used).convertTo(Size.Unit.MiB).getValue());

            payload.setModel(model);
        }

        private Payload getPayload(String payloadName) {
            Payload payload = regions.get(payloadName);
            if (payload == null) {
                payload = new Payload();
                payload.setName(payloadName);
            }
            return payload;
        }


        public void setPayloadFields(Payload payload, long used, long capacity, long maxCapacity) {
            Size.Unit usedScale = bestUnitForRange(used, capacity);
            double usedSpace = Size.bytes(used).convertTo(usedScale).getValue();
            double maxUsed = Size.bytes(capacity).convertTo(usedScale).getValue();

            payload.setUsed(usedSpace);
            payload.setMaxUsed(maxUsed);
            payload.setUsedUnit(usedScale);

            Size.Unit maxScale = bestUnitForRange(capacity, maxCapacity);
            double capacitySpace = Size.bytes(capacity).convertTo(maxScale).getValue();
            double maxCapacitySpace = Size.bytes(maxCapacity).convertTo(maxScale).getValue();

            payload.setCapacity(capacitySpace);
            payload.setMaxCapacity(maxCapacitySpace);
            payload.setCapacityUnit(maxScale);

            String tooltip = payload.getName() + ": used: " + String.format("%.2f", usedSpace) + " " + usedScale +
                    ", capacity: " + String.format("%.2f", capacitySpace) + " " + maxScale +
                    ", max capacity: " + String.format("%.2f", maxCapacitySpace) + " " + maxScale;

            payload.setTooltip(tooltip);
        }
    }

    class VmTlabStatCollector implements Runnable {

        @Override
        public void run() {
            VmTlabStat oldest = tlabStatDao.getOldestStat(ref);
            VmTlabStat newest = tlabStatDao.getNewestStat(ref);
            // Do nothing if there is no data
            if (oldest == null || newest == null) {
                return;
            }

            final List<DiscreteTimeData<? extends Number>> allocatingThreads = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalAllocations = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalRefills = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> maxRefills = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalSlowAllocations = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> maxSlowAllocations = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalGcWaste = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> maxGcWaste = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalSlowWaste = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> maxSlowWaste = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> totalFastWaste = new LinkedList<>();
            final List<DiscreteTimeData<? extends Number>> maxFastWaste= new LinkedList<>();

            Range<Long> newAvailableRange = new Range<>(oldest.getTimeStamp(), newest.getTimeStamp());

            TimeRangeController.StatsSupplier<VmTlabStat, VmRef> statsSupplier = new TimeRangeController.StatsSupplier<VmTlabStat, VmRef>() {
                @Override
                public List<VmTlabStat> getStats(VmRef ref, long since, long to) {
                    return tlabStatDao.getStats(ref, since, to);
                }
            };

            TimeRangeController.SingleArgRunnable<VmTlabStat> runnable = new TimeRangeController.SingleArgRunnable<VmTlabStat>() {
                @Override
                public void run(VmTlabStat arg) {
                    long timeStamp = arg.getTimeStamp();
                    allocatingThreads.add(new DiscreteTimeData<>(timeStamp, arg.getTotalAllocatingThreads()));
                    totalAllocations.add(new DiscreteTimeData<>(timeStamp, arg.getTotalAllocations()));
                    totalRefills.add(new DiscreteTimeData<>(timeStamp, arg.getTotalRefills()));
                    maxRefills.add(new DiscreteTimeData<>(timeStamp, arg.getMaxRefills()));
                    totalSlowAllocations.add(new DiscreteTimeData<>(timeStamp, arg.getTotalSlowAllocations()));
                    maxSlowAllocations.add(new DiscreteTimeData<>(timeStamp, arg.getMaxSlowAllocations()));
                    totalGcWaste.add(new DiscreteTimeData<>(timeStamp, arg.getTotalGcWaste()));
                    maxGcWaste.add(new DiscreteTimeData<>(timeStamp, arg.getMaxGcWaste()));
                    totalSlowWaste.add(new DiscreteTimeData<>(timeStamp, arg.getTotalSlowWaste()));
                    maxSlowWaste.add(new DiscreteTimeData<>(timeStamp, arg.getMaxSlowWaste()));
                    totalFastWaste.add(new DiscreteTimeData<>(timeStamp, arg.getTotalFastWaste()));
                    maxFastWaste.add(new DiscreteTimeData<>(timeStamp, arg.getMaxFastWaste()));
                }
            };

            tlabTimeRangeController.update(userDesiredDuration, newAvailableRange, statsSupplier, ref, runnable);

            view.addTlabData(Type.TOTAL_ALLOCATING_THREADS, allocatingThreads);
            view.addTlabData(Type.TOTAL_ALLOCATIONS, totalAllocations);
            view.addTlabData(Type.TOTAL_REFILLS, totalRefills);
            view.addTlabData(Type.MAX_REFILLS, maxRefills);
            view.addTlabData(Type.TOTAL_SLOW_ALLOCATIONS, totalSlowAllocations);
            view.addTlabData(Type.MAX_SLOW_ALLOCATIONS, maxSlowAllocations);
            view.addTlabData(Type.TOTAL_GC_WASTE, totalGcWaste);
            view.addTlabData(Type.MAX_GC_WASTE, maxGcWaste);
            view.addTlabData(Type.TOTAL_SLOW_WASTE, totalSlowWaste);
            view.addTlabData(Type.MAX_SLOW_WASTE, maxSlowWaste);
            view.addTlabData(Type.TOTAL_FAST_WASTE, totalFastWaste);
            view.addTlabData(Type.MAX_FAST_WASTE, maxFastWaste);
        }
    }

    public MemoryStatsController(ApplicationService appSvc, final VmInfoDAO vmInfoDao,
                                 final VmMemoryStatDAO vmMemoryStatDao, final VmTlabStatDAO vmTlabStatDao,
                                 final VmRef ref, MemoryStatsViewProvider viewProvider,
                                 final AgentInfoDAO agentDAO, final GCRequest gcRequest) {
        
        regions = new HashMap<>();
        this.ref = ref;
        vmDao = vmMemoryStatDao;
        tlabStatDao = vmTlabStatDao;
        view = viewProvider.createView();

        statCollector = new VmMemoryStatCollector();
        tlabCollector = new VmTlabStatCollector();

        memoryStatsTimer = createTimer(appSvc.getTimerFactory(), statCollector);
        tlabStatsTimer = createTimer(appSvc.getTimerFactory(), tlabCollector);

        view.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch(actionEvent.getActionId()) {
                    case HIDDEN:
                        stop();
                        break;
                        
                    case VISIBLE:
                        view.setEnableGCAction(vmInfoDao.getVmInfo(ref).isAlive());
                        start();
                        break;
                        
                    default:
                        throw new NotImplementedException("unknown event: " + actionEvent.getActionId());
                }
            }
        });

        view.addUserActionListener(new ActionListener<MemoryStatsView.UserAction>() {
            @Override
            public void actionPerformed(ActionEvent<MemoryStatsView.UserAction> e) {
                switch (e.getActionId()) {
                    case USER_CHANGED_TIME_RANGE:
                        Duration duration = (Duration) e.getPayload();
                        for (Map.Entry<String, Payload> entry : regions.entrySet()) {
                            Payload p = entry.getValue();
                            StatsModel model = p.getModel();
                            if (model != null) {
                                model.setTimeRangeToShow(duration);
                            }
                        }
                        break;
                    default:
                        throw new AssertionError("Unhandled action type: " + e.getActionId());
                }
            }
        });
        
        view.addGCActionListener(new ActionListener<GCAction>() {
            @Override
            public void actionPerformed(ActionEvent<GCAction> actionEvent) {
                RequestResponseListener listener = new RequestResponseListener() {
                    @Override
                    public void fireComplete(Request request, Response response) {
                        view.setEnableGCAction(true);
                        if (response.getType() == ResponseType.ERROR) {
                            view.displayWarning(translate.localize(
                                    LocaleResources.ERROR_PERFORMING_GC,
                                    ref.getHostRef().getAgentId(),
                                    ref.getVmId()));
                        }
                    }
                };
                view.setEnableGCAction(false);
                gcRequest.sendGCRequestToAgent(ref, agentDAO, listener);
            }
        });

        view.setEnableGCAction(vmInfoDao.getVmInfo(ref).isAlive());

        memoryTimeRangeController = new TimeRangeController<>();
        tlabTimeRangeController = new TimeRangeController<>();
    }

    private Timer createTimer(TimerFactory timerFactory, Runnable collector) {
        final Timer timer = timerFactory.createTimer();

        timer.setAction(collector);

        timer.setInitialDelay(0);
        timer.setDelay(1000);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);

        return timer;
    }

    // for testing
    VmMemoryStatCollector getMemoryStatCollector() {
        return statCollector;
    }

    VmTlabStatCollector getMemoryTlabCollector() {
        return tlabCollector;
    }

    Map<String, Payload> getRegions() {
        return regions;
    }
    
    private Size.Unit bestUnitForRange(long min, long max) {
        // FIXME: this is very dumb and very inefficient
        // needs cleanup
        Size.Unit minScale = Size.Unit.getBestUnit(min);
        Size.Unit maxScale = Size.Unit.getBestUnit(max);
        
        Size.Unit[] scales = Size.Unit.values();
        int maxID = 0;
        int minID = 0;
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] == minScale) {
                minID = i;
            }
            if (scales[i] == maxScale) {
                maxID = i;
            }
        }
        while (maxID - minID >= 2) {
            minID++;
        }
        return scales[minID];
    }
    
    private void start() {
        memoryStatsTimer.start();
        tlabStatsTimer.start();
    }

    private void stop() {
        memoryStatsTimer.stop();
        tlabStatsTimer.stop();
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translate.localize(LocaleResources.VM_INFO_TAB_MEMORY);
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) view;
    }
}

