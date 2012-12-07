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

package com.redhat.thermostat.client.heap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.VmInformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.heap.HeapView.HeapDumperAction;
import com.redhat.thermostat.client.heap.chart.OverviewChart;
import com.redhat.thermostat.client.heap.cli.HeapDumperCommand;
import com.redhat.thermostat.client.heap.internal.HeapDumpDetailsController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.DisplayableValues.Scale;
import com.redhat.thermostat.storage.model.HeapInfo;
import com.redhat.thermostat.storage.model.VmMemoryStat;
import com.redhat.thermostat.storage.model.VmMemoryStat.Generation;
import com.redhat.thermostat.storage.model.VmMemoryStat.Space;

public class HeapDumpController implements VmInformationServiceController {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final VmMemoryStatDAO vmDao;
    private final VmRef ref;
    
    private final HeapDAO heapDAO;
        
    private HeapView view;
    private final Timer timer;
    
    private OverviewChart model;
    private ApplicationService appService;
    private HeapDumpDetailsViewProvider detailsViewProvider;
    private HeapHistogramViewProvider histogramViewProvider;
    private ObjectDetailsViewProvider objectDetailsViewProvider;
    private ObjectRootsViewProvider objectRootsViewProvider;

    public HeapDumpController(final AgentInfoDAO agentInfoDao, final VmMemoryStatDAO vmMemoryStatDao, final HeapDAO heapDao, final VmRef ref, final ApplicationService appService, HeapViewProvider viewProvider, HeapDumpDetailsViewProvider detailsViewProvider, HeapHistogramViewProvider histogramProvider, ObjectDetailsViewProvider objectDetailsProvider, ObjectRootsViewProvider objectRootsProvider) {
        this(agentInfoDao, vmMemoryStatDao, heapDao, ref, appService, new HeapDumperCommand(), viewProvider, detailsViewProvider, histogramProvider, objectDetailsProvider, objectRootsProvider);
    }

    HeapDumpController(final AgentInfoDAO agentInfoDao, final VmMemoryStatDAO vmMemoryStatDao, final HeapDAO heapDao, final VmRef ref, final ApplicationService appService, final HeapDumperCommand command, HeapViewProvider viewProvider, HeapDumpDetailsViewProvider detailsViewProvider, HeapHistogramViewProvider histogramProvider, ObjectDetailsViewProvider objectDetailsProvider, ObjectRootsViewProvider objectRootsProvider) {
        
        this.objectDetailsViewProvider = objectDetailsProvider;
        this.objectRootsViewProvider = objectRootsProvider;
        this.histogramViewProvider = histogramProvider;
        this.detailsViewProvider = detailsViewProvider;
        this.appService = appService;
        this.ref = ref;
        this.vmDao = vmMemoryStatDao;
        this.heapDAO = heapDao;
        
        model = new OverviewChart(
                    translator.localize(LocaleResources.HEAP_CHART_TITLE),
                    translator.localize(LocaleResources.HEAP_CHART_TIME_AXIS),
                    translator.localize(LocaleResources.HEAP_CHART_HEAP_AXIS),
                    translator.localize(LocaleResources.HEAP_CHART_CAPACITY),
                    translator.localize(LocaleResources.HEAP_CHART_USED));
        
        timer = appService.getTimerFactory().createTimer();
        timer.setAction(new HeapOverviewDataCollector());
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        model.setRange(3600);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        
        view = viewProvider.createView();
        view.setModel(model);
        
        HeapDump dump = null;
        view.clearHeapDumpList();
        Collection<HeapInfo> infos = heapDAO.getAllHeapInfo(ref);
        for (HeapInfo info : infos) {
            dump = new HeapDump(info, heapDAO);
            view.addHeapDump(dump);
        }
        
        // check if we were reading some of the dumps
        dump = (HeapDump) appService.getApplicationCache().getAttribute(ref);
        if (dump != null && infos.contains(dump.getInfo())) {
            showHeapDumpDetails(dump);
        }
        
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

                default:
                    throw new NotImplementedException("unknown event: " + actionEvent.getActionId());
                }
            }
        });

        view.addDumperListener(new ActionListener<HeapView.HeapDumperAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumperAction> actionEvent) {
                HeapDump dump = null;
                switch (actionEvent.getActionId()) {
                case DUMP_REQUESTED:
                    command.execute(agentInfoDao, ref, new Runnable() {
                        public void run() {
                            view.notifyHeapDumpComplete();
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            view.displayWarning(translator.localize(
                                    LocaleResources.HEAP_DUMP_ERROR,
                                    ref.getAgent().getAgentId(),
                                    ref.getIdString()));
                        }
                    });
                    
                    break;
                
                case ANALYSE:
                    dump = (HeapDump) actionEvent.getPayload();
                    analyseDump(dump);
                    break;
                }
            }
        });
    }

    
    private void analyseDump(final HeapDump dump) {
        appService.getApplicationExecutor().execute(new Runnable() {
            
            @Override
            public void run() {
                showHeapDumpDetails(dump);
                appService.getApplicationCache().addAttribute(ref, dump);
            }
        });
    }

    private void showHeapDumpDetails(HeapDump dump) {
        HeapDumpDetailsController controller = new HeapDumpDetailsController(appService, detailsViewProvider, histogramViewProvider, objectDetailsViewProvider, objectRootsViewProvider);
        controller.setDump(dump);
        view.setChildView(controller.getView());
        view.openDumpView();
    }

    @Override
    public UIComponent getView() {
        return (UIComponent) view;
    }

    @Override
    public String getLocalizedName() {
        return translator.localize(LocaleResources.HEAP_SECTION_TITLE);
    }

    class HeapOverviewDataCollector implements Runnable {

        private long desiredUpdateTimeStamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

        @Override
        public void run() {
            checkForHeapDumps();
            updateMemoryChartAndDisplay();
        }

        private void checkForHeapDumps() {
            Collection<HeapInfo> heapInfos = heapDAO.getAllHeapInfo(ref);
            List<HeapDump> heapDumps = new ArrayList<HeapDump>(heapInfos.size());
            for (HeapInfo heapInfo : heapInfos) {
                heapDumps.add(new HeapDump(heapInfo, heapDAO));
            }
            view.updateHeapDumpList(heapDumps);
        }

        private void updateMemoryChartAndDisplay() {
            List<VmMemoryStat> vmInfo = null;
            vmInfo = vmDao.getLatestVmMemoryStats(ref, desiredUpdateTimeStamp);

            for (VmMemoryStat memoryStats: vmInfo) {
                long used = 0l;
                long capacity = 0l;
                long max = 0l;
                Generation[] generations = memoryStats.getGenerations();
                for (Generation generation : generations) {
                    
                    // non heap
                    if (generation.getName().equals("perm")) {
                        continue;
                    }
                    
                    Space[] spaces = generation.getSpaces();
                    for (Space space: spaces) {
                        used += space.getUsed();
                        capacity += space.getCapacity();
                        
                        // TODO
                        max =+ space.getMaxCapacity();
                    }
                }
                model.addData(memoryStats.getTimeStamp(), used, capacity);
                
                NumberFormat formatter = DecimalFormat.getInstance();

                double res = Scale.convertTo(Scale.B, used);
                String _used = formatter.format(res) + " " + Scale.B;
                
                res = Scale.convertTo(Scale.B, capacity);
                String _capacity= formatter.format(capacity) + " " + Scale.B;
                
                view.updateUsedAndCapacity(_used, _capacity);
                desiredUpdateTimeStamp = Math.max(desiredUpdateTimeStamp, memoryStats.getTimeStamp());
            }

            model.notifyListenersOfModelChange();
        }

    }
}
