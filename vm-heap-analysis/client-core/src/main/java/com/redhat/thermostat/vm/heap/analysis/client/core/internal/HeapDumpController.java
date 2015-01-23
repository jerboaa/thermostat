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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.DumpDisabledReason;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.HeapDumperAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.chart.OverviewChart;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.DumpFile;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class HeapDumpController implements InformationServiceController<VmRef> {

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
    private HeapDumpListViewProvider heapDumpListViewProvider;

    private ProgressNotifier notifier;
    
    public HeapDumpController(final VmMemoryStatDAO vmMemoryStatDao,
                              final VmInfoDAO vmInfoDao,
                              final HeapDAO heapDao, final VmRef ref,
                              final ApplicationService appService, HeapViewProvider viewProvider,
                              HeapDumpDetailsViewProvider detailsViewProvider,
                              HeapHistogramViewProvider histogramProvider,
                              ObjectDetailsViewProvider objectDetailsProvider,
                              ObjectRootsViewProvider objectRootsProvider,
                              HeapDumpListViewProvider heapDumpListViewProvider,
                              ProgressNotifier notifier)
    {
        this(vmMemoryStatDao, vmInfoDao, heapDao, ref, appService, viewProvider,
             detailsViewProvider, histogramProvider, objectDetailsProvider,
             objectRootsProvider, heapDumpListViewProvider, new HeapDumper(ref),
             notifier);
    }

    HeapDumpController(final VmMemoryStatDAO vmMemoryStatDao,
                       final VmInfoDAO vmInfoDao,
                       final HeapDAO heapDao, final VmRef ref,
                       final ApplicationService appService,
                       HeapViewProvider viewProvider,
                       HeapDumpDetailsViewProvider detailsViewProvider,
                       HeapHistogramViewProvider histogramProvider,
                       ObjectDetailsViewProvider objectDetailsProvider,
                       ObjectRootsViewProvider objectRootsProvider,
                       HeapDumpListViewProvider heapDumpListViewProvider,
                       final HeapDumper heapDumper,
                       ProgressNotifier notifier)
    {
        this.notifier = notifier;
        this.objectDetailsViewProvider = objectDetailsProvider;
        this.objectRootsViewProvider = objectRootsProvider;
        this.histogramViewProvider = histogramProvider;
        this.detailsViewProvider = detailsViewProvider;
        this.appService = appService;
        this.ref = ref;
        this.vmDao = vmMemoryStatDao;
        this.heapDAO = heapDao;
        this.heapDumpListViewProvider = heapDumpListViewProvider;
        
        model = new OverviewChart(
                    null,
                    translator.localize(LocaleResources.HEAP_CHART_TIME_AXIS).getContents(),
                    translator.localize(LocaleResources.HEAP_CHART_HEAP_AXIS).getContents(),
                    translator.localize(LocaleResources.HEAP_CHART_CAPACITY).getContents(),
                    translator.localize(LocaleResources.HEAP_CHART_USED).getContents());
        
        timer = appService.getTimerFactory().createTimer();
        timer.setAction(new HeapOverviewDataCollector());
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        model.setRange(1800);
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
                    view.disableHeapDumping(DumpDisabledReason.DUMP_IN_PROGRESS);
                    requestDump(heapDumper);
                    break;
                    
                case REQUEST_DISPLAY_DUMP_LIST:
                    openDumpList();
                    break;
                    
                case ANALYSE:
                    dump = (HeapDump) actionEvent.getPayload();
                    analyseDump(dump);
                    break;
                    
                case REQUEST_EXPORT: {
                    dump = (HeapDump) actionEvent.getPayload();
                    exportDump(dump);
                } break;
                
                case SAVE_HEAP_DUMP: {                    
                    DumpFile localHeapDump = (DumpFile) actionEvent.getPayload();
                    saveHeapDump(localHeapDump);
                } break;
                
                default:
                    break;
                }
            }
        });

        boolean vmIsAlive = vmInfoDao.getVmInfo(ref).isAlive();
        if (!vmIsAlive) {
            view.disableHeapDumping(DumpDisabledReason.PROCESS_DEAD);
        }
    }

    private void saveHeapDump(final DumpFile localHeapDump) {
        appService.getApplicationExecutor().execute(new Runnable() {
            @Override
            public void run() {

                LocalizedString taskName = translator.localize(LocaleResources.HEAP_DUMP_IN_PROGRESS);
                
                final ProgressHandle handle = new ProgressHandle(taskName);
                handle.setTask(taskName);
                handle.setIndeterminate(true);
                notifier.register(handle);

                HeapDump dump = localHeapDump.getDump();
                File file = localHeapDump.getFile();
                if (dump == null || file == null) {
                    // this is here mainly for the tests, since we don't
                    // expect files or dumps to be null
                    return;
                }
                
                handle.start();
                try (InputStream in = heapDAO.getHeapDumpData(dump.getInfo())) {
                    Files.copy(in, file.toPath());
                    
                } catch (IOException e) {
                    LocalizedString message = translator.localize(LocaleResources.ERROR_EXPORTING_FILE);
                    view.displayWarning(message);
                    Logger.getLogger(HeapDumpController.class.getSimpleName()).
                        log(Level.WARNING, message.getContents(), e);
                } finally {
                    handle.stop();
                }
            }
        });
    }
    
    private void openDumpList() {
        
        appService.getApplicationExecutor().execute(new Runnable() {
            @Override
            public void run() {
                List<HeapDump> dumps = getHeapDumps();
                HeapDumpListController controller =
                        new HeapDumpListController(heapDumpListViewProvider,
                                                   HeapDumpController.this);
                controller.setDumps(dumps);
                view.openDumpListView(controller.getView());                
            }
        });
    }
    
    private void requestDump(final HeapDumper heapDumper) {
        appService.getApplicationExecutor().execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    heapDumper.dump();
                    view.enableHeapDumping();
                    view.notifyHeapDumpComplete();
                } catch (CommandException e) {
                    view.displayWarning(e.getTranslatedMessage());
                }
            }
        });
    }

    void analyseDump(final HeapDump dump) {
        appService.getApplicationExecutor().execute(new Runnable() {
            @Override
            public void run() {
                showHeapDumpDetails(dump);
                appService.getApplicationCache().addAttribute(ref, dump);
            }
        });
    }

    void exportDump(final HeapDump dump) {
        DumpFile localHeapDump = new DumpFile();
        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
        Date date = new Date(dump.getTimestamp());
        String timeStamp = format.format(date);
        String id = "heapdump-" + ref.getName() + "-" + timeStamp + "." + dump.getType();
        localHeapDump.setFile(new File(id));
        localHeapDump.setDump(dump);
        view.openExportDialog(localHeapDump);
    }

    private void showHeapDumpDetails(HeapDump dump) {
        HeapDumpDetailsController controller =
                new HeapDumpDetailsController(appService, detailsViewProvider,
                                              histogramViewProvider,
                                              objectDetailsViewProvider,
                                              objectRootsViewProvider);
        controller.setDump(dump);
        view.setActiveDump(dump);
        view.setChildView(controller.getView());
        view.openDumpView();
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.HEAP_SECTION_TITLE);
    }

    private List<HeapDump> getHeapDumps() {
        Collection<HeapInfo> heapInfos = heapDAO.getAllHeapInfo(ref);
        List<HeapDump> heapDumps = new ArrayList<HeapDump>(heapInfos.size());
        for (HeapInfo heapInfo : heapInfos) {
            heapDumps.add(new HeapDump(heapInfo, heapDAO));
        }
        return heapDumps;
    }
    
    class HeapOverviewDataCollector implements Runnable {

        private long desiredUpdateTimeStamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

        @Override
        public void run() {
            checkForHeapDumps();
            updateMemoryChartAndDisplay();
        }

        private void checkForHeapDumps() {
            view.updateHeapDumpList(getHeapDumps());
        }

        private void updateMemoryChartAndDisplay() {
            List<VmMemoryStat> vmInfo = vmDao.getLatestVmMemoryStats(ref, desiredUpdateTimeStamp);

            for (VmMemoryStat memoryStats : vmInfo) {
                long used = 0l;
                long capacity = 0l;
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
                    }
                }
                model.addData(memoryStats.getTimeStamp(), used, capacity);

                String _used = Size.bytes(used).toString();
                String _capacity= Size.bytes(capacity).toString();
                
                view.updateUsedAndCapacity(_used, _capacity);
                desiredUpdateTimeStamp = Math.max(desiredUpdateTimeStamp, memoryStats.getTimeStamp());
            }

            model.notifyListenersOfModelChange();
        }
    }
}

