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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.lowagie.text.pdf.codec.Base64.InputStream;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationCache;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.DumpDisabledReason;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.HeapDumperAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.DumpFile;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class HeapDumpControllerTest {
    private static int TIMEOUT_MS = 1000;
    
    private ActionListener<HeapView.Action> actionListener;
    private ActionListener<HeapView.HeapDumperAction> heapDumperListener;

    private Timer timer;

    private HeapDAO heapDao;
    private VmInfoDAO vmInfoDao;
    private VmMemoryStatDAO vmDao;
    private HeapView view;
    private HeapDumpDetailsView detailsView;

    @SuppressWarnings("unused")
    private HeapDumpController controller;
    private HeapDumper heapDumper;
    private ApplicationService appService;
    private ArgumentCaptor<Runnable> timerActionCaptor;

    private HeapViewProvider viewProvider;
    private HeapDumpDetailsViewProvider detailsViewProvider;
    private HeapHistogramViewProvider histogramProvider;
    private HeapTreeMapViewProvider treeMapProvider;
    private ObjectDetailsViewProvider objectDetailsProvider;
    private ObjectRootsViewProvider objectRootsProvider;

    private HeapDumpListViewProvider heapDumpListViewProvider;
    
    private ProgressNotifier notifier;
    private HeapDumpListController heapDumpListController;
    @Before
    public void setUp() {
        heapDao = mock(HeapDAO.class);
        vmDao = mock(VmMemoryStatDAO.class);
        vmInfoDao = mock(VmInfoDAO.class);
        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive()).thenReturn(true);
        when(vmInfoDao.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);
        appService = mock(ApplicationService.class);
        heapDumper = mock(HeapDumper.class);

        notifier = mock(ProgressNotifier.class);
        
        heapDumpListViewProvider = mock(HeapDumpListViewProvider.class);
        
        setUpView();
    }

    private void setUpTimers() {
        timer = mock(Timer.class);
        timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appService.getTimerFactory()).thenReturn(timerFactory);
    }

    private void setUpView() {
        view = mock(HeapView.class);
        viewProvider = mock(HeapViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        detailsViewProvider = mock(HeapDumpDetailsViewProvider.class);
        detailsView = mock(HeapDumpDetailsView.class);
        when(detailsViewProvider.createView()).thenReturn(detailsView);

        HeapHistogramView histogramView = mock(HeapHistogramView.class);
        histogramProvider = mock(HeapHistogramViewProvider.class);
        when(histogramProvider.createView()).thenReturn(histogramView);

        HeapTreeMapView treeMapView = mock(HeapTreeMapView.class);
        treeMapProvider = mock(HeapTreeMapViewProvider.class);
        when(treeMapProvider.createView()).thenReturn(treeMapView);

        ObjectDetailsView objectView = mock(ObjectDetailsView.class);
        objectDetailsProvider = mock(ObjectDetailsViewProvider.class);
        when(objectDetailsProvider.createView()).thenReturn(objectView);

        ObjectRootsView objectRootsView = mock(ObjectRootsView.class);
        objectRootsProvider = mock(ObjectRootsViewProvider.class);
        when(objectRootsProvider.createView()).thenReturn(objectRootsView);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setUpListeners() {        
        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor1.capture());
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor2 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addDumperListener(viewArgumentCaptor2.capture());
        
        createController();
        
        actionListener = viewArgumentCaptor1.getValue();
        heapDumperListener = viewArgumentCaptor2.getValue();
    }
    
    private void createController() {
        ApplicationCache cache = mock(ApplicationCache.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        setUpTimers();

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("agent-id");

        VmRef ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn("vm-id");
        when(ref.getHostRef()).thenReturn(hostRef);

        heapDumpListController = mock(HeapDumpListController.class);
        
        controller = new HeapDumpController(vmDao, vmInfoDao, heapDao, ref, appService,
                viewProvider, detailsViewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider, heapDumpListViewProvider,
                heapDumper, notifier) {
            HeapDumpListController createHeapDumpListController() {
                return heapDumpListController;
            }
        };
    }
    
    @After
    public void tearDown() {
        controller = null;
        vmDao = null;
        heapDao = null;
        viewProvider = null;
        detailsViewProvider = null;
        histogramProvider = null;
        treeMapProvider = null;
        objectDetailsProvider = null;
        objectRootsProvider = null;
        appService = null;
    }

    @Test
    public void testTimerStartOnViewVisible() {

        setUpListeners();

        actionListener.actionPerformed(new ActionEvent<>(view,
                HeapView.Action.VISIBLE));
        verify(timer).start();
    }

    @Test
    public void testTimerStopsOnViewHidden() {

        setUpListeners();

        actionListener.actionPerformed(new ActionEvent<>(view,
                HeapView.Action.HIDDEN));
        verify(timer).stop();
    }

    @Test
    public void testNotAddHeapDumpsAtStartupWhenNoDumps() {

        when(heapDao.getAllHeapInfo(any(VmRef.class))).thenReturn(
                new ArrayList<HeapInfo>());

        createController();

        verify(view, times(0)).addHeapDump(any(HeapDump.class));
    }

    @Test
    public void testAddHeapDumpsAtStartupWhenDumpsAreThere() {
        HeapInfo info1 = mock(HeapInfo.class);
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDao.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);

        createController();
        
        verify(view, times(2)).addHeapDump(any(HeapDump.class));
    }
    
    @Test
    public void testOpenDumpCalledWhenPreviousDump() throws IOException {

        setUpTimers();
        HeapDump dump = mock(HeapDump.class);

        ObjectHistogram histogram = mock(ObjectHistogram.class);
        when(dump.getHistogram()).thenReturn(histogram);
        
        HeapInfo info1 = mock(HeapInfo.class);
        when(dump.getInfo()).thenReturn(info1);
        
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDao.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);
        
        ApplicationCache cache = mock(ApplicationCache.class);
        when(cache.getAttribute(any(VmRef.class))).thenReturn(dump);
        when(appService.getApplicationCache()).thenReturn(cache);
        VmRef ref = mock(VmRef.class);
        controller = new HeapDumpController(vmDao, vmInfoDao, heapDao, ref, appService,
                viewProvider, detailsViewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider, heapDumpListViewProvider,
                heapDumper, notifier);
        
        verify(view, times(1)).setChildView(any(HeapView.class));
        verify(view, times(1)).openDumpView();
    }
    
    @Test
    public void testNotOpenDumpCalledWhenNoPreviousDump() {

        setUpTimers();
        HeapInfo info1 = mock(HeapInfo.class);        
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDao.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);
        
        ApplicationCache cache = mock(ApplicationCache.class);
        when(cache.getAttribute(any(VmRef.class))).thenReturn(null);

        when(appService.getApplicationCache()).thenReturn(cache);
        VmRef ref = mock(VmRef.class);
        controller = new HeapDumpController(vmDao, vmInfoDao, heapDao, ref, appService,
                viewProvider, detailsViewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider, heapDumpListViewProvider,
                heapDumper, notifier);
        
        verify(view, times(0)).openDumpView();
    }

    @Test
    public void testHeapDumpingDisabledForDeadVms() {
        ApplicationCache cache = mock(ApplicationCache.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        setUpTimers();

        VmRef ref = mock(VmRef.class);

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive()).thenReturn(false);

        when(vmInfoDao.getVmInfo(ref)).thenReturn(vmInfo);

        controller = new HeapDumpController(vmDao, vmInfoDao, heapDao, ref, appService,
                viewProvider, detailsViewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider, heapDumpListViewProvider,
                heapDumper, notifier);

        verify(view).disableHeapDumping(DumpDisabledReason.PROCESS_DEAD);
    }

    @Test
    public void testRequestHeapDump() throws CommandException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mockExecutorService(latch);
        setUpListeners();

        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));
        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        verify(heapDumper).dump();
        verify(view).notifyHeapDumpComplete();
    }
    
    @Test
    public void testRequestExport() throws CommandException, InterruptedException {
        setUpListeners();

        HeapDump dump = mock(HeapDump.class);
        when(dump.getTimestamp()).thenReturn(1l);
        when(dump.getType()).thenReturn("TEST");

        ArgumentCaptor<DumpFile> localDumpCaptor = ArgumentCaptor.forClass(DumpFile.class);
        
        ActionEvent<HeapDumperAction> event = new ActionEvent<>(view, HeapDumperAction.REQUEST_EXPORT);
        event.setPayload(dump);
        
        heapDumperListener.actionPerformed(event);
        
        verify(view).openExportDialog(localDumpCaptor.capture());
        DumpFile localDump = localDumpCaptor.getValue();
        
        assertTrue(localDump.getFile().getName().endsWith(".TEST"));
        assertEquals(dump, localDump.getDump());
    }
    
    @Test
    public void testExportLocation() throws CommandException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mockExecutorService(latch);
        
        setUpListeners();

        HeapInfo info = mock(HeapInfo.class);
        HeapDump dump = mock(HeapDump.class);
        when(dump.getInfo()).thenReturn(info);
        DumpFile localDump = mock(DumpFile.class);
        when(localDump.getDump()).thenReturn(dump);

        InputStream stream = mock(InputStream.class);
        when(heapDao.getHeapDumpData(any(HeapInfo.class))).thenReturn(stream);
        
        ActionEvent<HeapDumperAction> event = new ActionEvent<>(view, HeapDumperAction.SAVE_HEAP_DUMP);
        event.setPayload(localDump);
        
        heapDumperListener.actionPerformed(event);
        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        verify(localDump).getFile();
        verify(localDump).getDump();
    }
        
    @Test
    public void testRequestHeapDumpFails() throws CommandException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final LocalizedString errMessage = new LocalizedString("Error dumping heap (agent: agent-id, vm: vm-id)");
        mockExecutorService(latch);
        setUpListeners();
        
        doThrow(new CommandException(errMessage)).when(heapDumper).dump();
        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));
        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        
        verify(heapDumper).dump();
        verify(view).displayWarning(errMessage);
    }

    private void mockExecutorService(final CountDownLatch latch) {
        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(new Answer<Object>() {
    
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                latch.countDown();
                return null;
            }
        }).when(executor).execute(any(Runnable.class));
        when(appService.getApplicationExecutor()).thenReturn(executor);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimerChecksForNewHeapDumps() {

        HeapInfo info1 = mock(HeapInfo.class);
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);

        when(heapDao.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);

        createController();

        timerActionCaptor.getValue().run();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> heapDumps = ArgumentCaptor.forClass(List.class);
        verify(view).updateHeapDumpList(heapDumps.capture());
        assertTrue(heapDumps.getValue().contains(new HeapDump(info1, heapDao)));
        assertTrue(heapDumps.getValue().contains(new HeapDump(info2, heapDao)));
    }

    @Test
    public void testTimerFetchesMemoryDataAndUpdatesView() {
        createController();
        Runnable timerAction = timerActionCaptor.getValue();

        final long CAPACITY = 10;
        final long USED = 5;
        Space space = new Space();
        space.setCapacity(CAPACITY);
        space.setMaxCapacity(20);
        space.setUsed(USED);
        Generation gen = new Generation();
        gen.setName("foobar");
        gen.setSpaces(new Space[] { space });
        VmMemoryStat stat = new VmMemoryStat();
        stat.setGenerations(new Generation[] { gen });

        when(vmDao.getLatestVmMemoryStats(isA(VmRef.class), anyLong()))
                .thenReturn(Arrays.asList(stat));

        timerAction.run();

        ArgumentCaptor<Long> timeStampCaptor = ArgumentCaptor
                .forClass(Long.class);
        verify(vmDao).getLatestVmMemoryStats(isA(VmRef.class),
                timeStampCaptor.capture());
        assertTimeStampIsAround(
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                timeStampCaptor.getValue());
        verify(view).updateUsedAndCapacity(USED + " B", CAPACITY + " B");
    }

    @Test
    public void testTimerFetchesMemoryDataDeltaOnly() {
        ArgumentCaptor<Long> timeStampCaptor = ArgumentCaptor
                .forClass(Long.class);

        final long DATA_TIMESTAMP = System.currentTimeMillis() + 1000000000;
        Space space = new Space();
        space.setCapacity(10);
        space.setMaxCapacity(20);
        space.setUsed(5);
        Generation gen = new Generation();
        gen.setName("foobar");
        gen.setSpaces(new Space[] { space });
        VmMemoryStat stat = new VmMemoryStat();
        stat.setTimeStamp(DATA_TIMESTAMP);
        stat.setGenerations(new Generation[] { gen });

        when(vmDao.getLatestVmMemoryStats(isA(VmRef.class), anyLong()))
                .thenReturn(Arrays.asList(stat));

        createController();
        Runnable timerAction = timerActionCaptor.getValue();

        timerAction.run();
        timerAction.run();

        verify(vmDao, times(2)).getLatestVmMemoryStats(isA(VmRef.class),
                timeStampCaptor.capture());

        long timeStamp1 = timeStampCaptor.getAllValues().get(0);
        assertTimeStampIsAround(
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                timeStamp1);

        long timeStamp2 = timeStampCaptor.getAllValues().get(1);
        assertTimeStampIsAround(DATA_TIMESTAMP, timeStamp2);
    }

    @Test
    public void testTimerFetchesMemoryDataDeltaOnlyEvenWithNoData() {
        ArgumentCaptor<Long> timeStampCaptor = ArgumentCaptor
                .forClass(Long.class);

        createController();
        Runnable timerAction = timerActionCaptor.getValue();

        timerAction.run();
        timerAction.run();

        verify(vmDao, times(2)).getLatestVmMemoryStats(isA(VmRef.class),
                timeStampCaptor.capture());

        long timeStamp1 = timeStampCaptor.getAllValues().get(0);
        assertTimeStampIsAround(
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                timeStamp1);

        long timeStamp2 = timeStampCaptor.getAllValues().get(1);
        assertTimeStampIsAround(
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                timeStamp2);
    }

    private void assertTimeStampIsAround(long expected, long actual) {
        assertTrue(actual <= expected + 1000);
        assertTrue(actual >= expected - 1000);
    }
    
    @Test
    public void testRequestHeapDumpUpdatesDumpList() throws CommandException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mockExecutorService(latch);
        setUpListeners();

        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));
        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        verify(heapDumper).dump();
        verify(view).notifyHeapDumpComplete();
        
        // the list has not been opened yet, so no interaction should happen
        // here
        verify(heapDumpListController, times(0)).setDumps(any(List.class));
        
        // now the list should be intialiased
        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.REQUEST_DISPLAY_DUMP_LIST));
        verify(heapDumpListController, times(1)).setDumps(any(List.class));
        
        // since it was initialised before, we should see another interaction
        // here
        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));
        verify(heapDumpListController, times(2)).setDumps(any(List.class));
    }

}
