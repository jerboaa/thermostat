/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.client.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.storage.model.TimeStampedPojoComparator;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.core.VmGcViewProvider;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;

public class VmGcController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final GcCommonNameMapper mapper = new GcCommonNameMapper();

    private final VmRef ref;
    private final VmGcView view;

    private final VmGcStatDAO gcDao;
    private final VmMemoryStatDAO memDao;
    private final VmInfoDAO infoDAO;
    private final AgentInfoDAO agentDAO;

    private final Set<String> addedCollectors = new TreeSet<>();
    // the last value seen for each collector
    private final Map<String, VmGcStat> lastValueSeen = new TreeMap<>();

    private final Timer timer;

    private long lastSeenTimeStamp;

    public VmGcController(ApplicationService appSvc, VmMemoryStatDAO vmMemoryStatDao, VmGcStatDAO vmGcStatDao, VmInfoDAO vmInfoDAO, AgentInfoDAO agentInfoDAO, final VmRef ref, VmGcViewProvider provider, final GCRequest gcRequest) {
        this.ref = ref;
        this.view = provider.createView();
        this.timer = appSvc.getTimerFactory().createTimer();

        gcDao = vmGcStatDao;
        memDao = vmMemoryStatDao;
        infoDAO = vmInfoDAO;
        agentDAO = agentInfoDAO;

        view.addActionListener(new ActionListener<VmGcView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        stop();
                        break;
                    case VISIBLE:
                        view.setEnableGCAction(infoDAO.getVmInfo(ref).isAlive());
                        start();
                        break;
                    default:
                        throw new NotImplementedException("unkonwn action: " + actionEvent.getActionId());
                }
            }
        });

        view.addUserActionListener(new ActionListener<VmGcView.UserAction>() {
            @Override
            public void actionPerformed(ActionEvent<VmGcView.UserAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case USER_CHANGED_TIME_RANGE:
                        Duration userDuration = view.getUserDesiredDuration();
                        lastSeenTimeStamp = System.currentTimeMillis() - userDuration.asMilliseconds();
                        doUpdateCollectorData();
                        break;
                    default:
                        throw new AssertionError("Unhandled action type");
                }
            }
        });

        view.addGCActionListener(new ActionListener<GCAction>() {
            @Override
            public void actionPerformed(ActionEvent<GCAction> actionEvent) {
                RequestResponseListener listener = new RequestResponseListener() {
                    @Override
                    public void fireComplete(Request request, Response response) {
                        if (response.getType() == Response.ResponseType.ERROR) {
                            view.displayWarning(translator.localize(
                                    LocaleResources.ERROR_PERFORMING_GC,
                                    VmGcController.this.ref.getHostRef().getAgentId(),
                                    VmGcController.this.ref.getVmId()));
                        }
                        view.setEnableGCAction(true);
                    }
                };
                view.setEnableGCAction(false);
                gcRequest.sendGCRequestToAgent(VmGcController.this.ref, agentDAO, listener);
            }
        });

        Duration userDuration = view.getUserDesiredDuration(); //Has default of 10 minutes
        lastSeenTimeStamp = System.currentTimeMillis() - userDuration.asMilliseconds();

        timer.setAction(new Runnable() {
            @Override
            public void run() {
                try {
                    doUpdateCollectorData();
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw t;
                }
            }
        });
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setInitialDelay(0);
        timer.setDelay(5);
        timer.setTimeUnit(TimeUnit.SECONDS);

        view.setEnableGCAction(infoDAO.getVmInfo(ref).isAlive());
    }

    private void start() {
        timer.start();
    }

    private void stop() {
         timer.stop();
    }

    private LocalizedString chartName(String collectorName, String generationName, boolean isGenerational) {
        if (isGenerational) {
            return translator.localize(LocaleResources.VM_GC_COLLECTOR_OVER_GENERATION,
                collectorName, generationName);
        } else {
            return translator.localize(LocaleResources.VM_GC_COLLECTOR_NON_GENERATIONAL, collectorName);
        }
    }

    private synchronized void doUpdateCollectorData() {
        CollectorCommonName commonName = getCommonName();
        String rawJavaVersion = infoDAO.getVmInfo(ref).getJavaVersion();
        view.setCollectorInfo(commonName, rawJavaVersion);

        Map<String, List<IntervalTimeData<Double>>> dataToAdd = new HashMap<>();
        List<VmGcStat> sortedList = gcDao.getLatestVmGcStats(ref, lastSeenTimeStamp);
        Collections.sort(sortedList, new TimeStampedPojoComparator<>());

        for (VmGcStat stat : sortedList) {
            String collector = stat.getCollectorName();
            List<IntervalTimeData<Double>> data = dataToAdd.get(collector);
            if (data == null) {
                data = new ArrayList<>();
                dataToAdd.put(collector, data);
            }
            if (lastValueSeen.containsKey(collector)) {
                if (stat.getTimeStamp() <= lastValueSeen.get(collector).getTimeStamp()) {
                    //FIXME: algorithm does not handle this condition at the moment
                    System.out.println("new gc collector value is older than previous value");
                }
                VmGcStat last = lastValueSeen.get(collector);
                lastSeenTimeStamp = Math.max(lastSeenTimeStamp, stat.getTimeStamp());
                long diffInMicro = (stat.getWallTime() - last.getWallTime());
                double diffInMillis = diffInMicro / 1000.0;
                // TODO there is not much point in adding data when diff is 0,
                // but we need to make the chart scroll automatically based on
                // the current time when we do that
                //  if (diff != 0) {
                data.add(new IntervalTimeData<>(last.getTimeStamp(), stat.getTimeStamp(), diffInMillis));
                // }
            }
            lastValueSeen.put(collector, stat);
        }
        final boolean isGenerational = isGenerationalCollector(commonName);
        for (Map.Entry<String, List<IntervalTimeData<Double>>> entry : dataToAdd.entrySet()) {
            String name = entry.getKey();
            if (!addedCollectors.contains(name)) {
                view.addChart(name, chartName(name, getCollectorGeneration(name), isGenerational), "ms");
                addedCollectors.add(name);
            }
            view.addData(entry.getKey(), entry.getValue());
        }
    }
    
    private boolean isGenerationalCollector(CollectorCommonName commonName) {
        switch (commonName) {
        case SHENANDOAH:
            return false;
        default:
            // Default to generational
            return true;
        }
    }

    private CollectorCommonName getCommonName() {
        Set<String> distinctCollectors = gcDao.getDistinctCollectorNames(ref);
        CollectorCommonName commonName = CollectorCommonName.UNKNOWN_COLLECTOR;
        if (distinctCollectors.size() > 0) {
            commonName = mapper.mapToCommonName(distinctCollectors);
        }
        return commonName;
    }

    public String getCollectorGeneration(String collectorName) {
        VmMemoryStat info = memDao.getNewestMemoryStat(ref);

        for (Generation g: info.getGenerations()) {
            if (g.getCollector().equals(collectorName)) {
                return g.getName();
            }
        }
        return translator.localize(LocaleResources.UNKNOWN_GEN).getContents();
    }

    public UIComponent getView() {
        return (UIComponent) view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VM_INFO_TAB_GC);
    }

}

