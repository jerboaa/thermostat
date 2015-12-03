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

package com.redhat.thermostat.vm.memory.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsViewProvider;
import com.redhat.thermostat.vm.memory.client.core.Payload;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class MemoryStatsControllerTest {

    private Generation[] generations = new Generation[2];

    private VmInfoDAO infoDao;
    private MemoryStatsView view;
    private Timer timer;

    private ActionListener<MemoryStatsView.Action> viewListener;
    private ActionListener<GCAction> gcActionListener;

    private MemoryStatsController controller;

    private Space canary;

    private AgentInfoDAO agentDAO;
    private GCRequest gcRequest;

    private VmRef ref;

    private Long[] timestamps = new Long[5];

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupVMAliveAndWithDAO(boolean vmIsAlive, VmMemoryStatDAO dao) {

        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        VmInfo vmOverallInformation = mock(VmInfo.class);
        when(vmOverallInformation.isAlive()).thenReturn(vmIsAlive);
        infoDao = mock(VmInfoDAO.class);
        when(infoDao.getVmInfo(any(VmRef.class))).thenReturn(vmOverallInformation);

        view = mock(MemoryStatsView.class);
        MemoryStatsViewProvider viewProvider = mock(MemoryStatsViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        ArgumentCaptor<ActionListener> viewArgumentCaptor =
                ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        ArgumentCaptor<ActionListener> gcArgumentCaptor =
                ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addGCActionListener(gcArgumentCaptor.capture());

        ref = mock(VmRef.class);

        agentDAO = mock(AgentInfoDAO.class);
        gcRequest = mock(GCRequest.class);

        controller = new MemoryStatsController(appSvc, infoDao, dao, ref, viewProvider, agentDAO, gcRequest);

        viewListener = viewArgumentCaptor.getValue();
        gcActionListener = gcArgumentCaptor.getValue();
    }

    private VmMemoryStatDAO setupVmMemoryStatDAOWithData() {
        List<VmMemoryStat> vmInfo = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Generation generation = new Generation();
            generation.setName("fluff" + i);
            VmMemoryStat.Space[] spaces = new VmMemoryStat.Space[2 + (1 - i)];
            for (int j = 0; j < 2; j++) {
                Space space = new Space();
                space.setName("fluffer" + i + j);
                space.setUsed(100);
                space.setCapacity(1000);
                space.setMaxCapacity(10000);
                spaces[j] = space;
            }
            if (i == 0) {
                // special payload because the others have all the same values
                canary = new Space();
                canary.setName("canary");
                canary.setUsed(1);
                canary.setCapacity(2);
                canary.setMaxCapacity(3);
                spaces[2] = canary;
            }
            generation.setSpaces(spaces);
            generations[i] = generation;
        }

        long timestamp = System.currentTimeMillis();
        String vmID = "vmId";
        for (int i = 0; i < 5; i++) {
            timestamps[i] = timestamp;
            VmMemoryStat vmMemory = new VmMemoryStat("foo-agent", timestamp, vmID, generations);
            vmInfo.add(vmMemory);
            timestamp++;
        }

        VmMemoryStatDAO memoryStatDao = mock(VmMemoryStatDAO.class);

        when(memoryStatDao.getVmMemoryStats(any(VmRef.class), anyLong(), anyLong())).thenReturn(vmInfo);

        when(memoryStatDao.getOldestMemoryStat(any(VmRef.class))).thenReturn(vmInfo.get(0));
        when(memoryStatDao.getNewestMemoryStat(any(VmRef.class))).thenReturn(vmInfo.get(4));
        return memoryStatDao;
    }

    @Test
    public void testStartStopTimer() {
        setupVMAliveAndWithDAO(true, mock(VmMemoryStatDAO.class));
        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.VISIBLE));

        verify(timer).start();
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);

        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void testGCIsDisabledForDeadVms() {
        setupVMAliveAndWithDAO(false, mock(VmMemoryStatDAO.class));

        verify(view).setEnableGCAction(false);
    }

    @Test
    public void testGCInvoked() {
        setupVMAliveAndWithDAO(true, mock(VmMemoryStatDAO.class));
        gcActionListener.actionPerformed(new ActionEvent<>(view, GCAction.REQUEST_GC));
        verify(gcRequest).sendGCRequestToAgent(eq(ref), eq(agentDAO), isA(RequestResponseListener.class));
    }

    @Test
    public void testPayloadContainSpaces() {
        setupVMAliveAndWithDAO(true, setupVmMemoryStatDAOWithData());
        Runnable collector = controller.getCollector();
        collector.run();

        Map<String, Payload> regions = controller.getRegions();
        assertEquals(5, regions.size());

        assertTrue(regions.containsKey("fluffer00"));
        assertTrue(regions.containsKey("fluffer01"));
        assertTrue(regions.containsKey("fluffer10"));
        assertTrue(regions.containsKey("fluffer11"));

        assertTrue(regions.containsKey("canary"));
    }

    @Test
    public void testValues() {
        setupVMAliveAndWithDAO(true, setupVmMemoryStatDAOWithData());
        Runnable collector = controller.getCollector();
        collector.run();

        Map<String, Payload> regions = controller.getRegions();

        Payload payload = regions.get("fluffer00");
        assertEquals("fluffer00", payload.getName());
        assertEquals(10000, payload.getMaxCapacity(), 0);
        assertEquals(1000, payload.getCapacity(), 0);
        assertEquals(100, payload.getUsed(), 0);

        payload = regions.get("canary");
        assertEquals("canary", payload.getName());
        assertEquals(3, payload.getMaxCapacity(), 0);
        assertEquals(2, payload.getCapacity(), 0);
        assertEquals(1, payload.getUsed(), 0);

        // the value above all ensure the same scale is used
        String tooltip = payload.getName() + ": used: " + String.format("%.2f", payload.getUsed()) + " " +
                         payload.getUsedUnit() + ", capacity: " +
                         String.format("%.2f", payload.getCapacity()) + " " + payload.getUsedUnit() +
                         ", max capacity: " + String.format("%.2f", payload.getMaxCapacity()) + " " +
                         payload.getUsedUnit();

        assertEquals(tooltip, payload.getTooltip());
    }


    @Test
    public void testTimerFetchesMemoryDataDeltaOnly() throws InterruptedException {
        VmMemoryStatDAO memoryStatDao = setupVmMemoryStatDAOWithData();
        setupVMAliveAndWithDAO(true, memoryStatDao);
        ArgumentCaptor<Long> timeStampCaptor = ArgumentCaptor.forClass(Long.class);

        Runnable timerAction = controller.getCollector();
        timerAction.run();

        Space space = new Space();
        space.setCapacity(10);
        space.setMaxCapacity(20);
        space.setUsed(5);
        Generation gen = new Generation();
        gen.setName("foobar");
        gen.setSpaces(new Space[]{space });
        VmMemoryStat stat = new VmMemoryStat();
        final long DATA_TIMESTAMP = System.currentTimeMillis() + 100;
        stat.setTimeStamp(DATA_TIMESTAMP);
        stat.setGenerations(new Generation[] { gen });

        when(memoryStatDao.getVmMemoryStats(any(VmRef.class), anyLong(), anyLong())).thenReturn(Arrays.asList(stat));
        when(memoryStatDao.getNewestMemoryStat(any(VmRef.class))).thenReturn(stat);

        timerAction.run();

        verify(memoryStatDao, times(2)).getVmMemoryStats(isA(VmRef.class), timeStampCaptor.capture(), timeStampCaptor.capture());

        List<Long> times = timeStampCaptor.getAllValues();

        long start1 = times.get(0);
        assertTimeStampIsAround(timestamps[0], start1);

        long end1 = times.get(1);
        long start2 = times.get(2);
        assertEquals(end1, start2);

        long end2 = times.get(3);
        assertTimeStampIsAround(DATA_TIMESTAMP, end2);
    }

    @Test
    public void verifyGcEnabled() {
        setupVMAliveAndWithDAO(true, mock(VmMemoryStatDAO.class));
        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.VISIBLE));
        verify(view, atLeastOnce()).setEnableGCAction(true);
    }

    @Test
    public void verifyGcDisabledWhenVmDead() {
        setupVMAliveAndWithDAO(false, mock(VmMemoryStatDAO.class));
        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.VISIBLE));
        verify(view, atLeastOnce()).setEnableGCAction(false);
    }
    
    @Test
    public void verifyNoMemoryDataDoesNotThrowNPE() {
        setupVMAliveAndWithDAO(true, mock(VmMemoryStatDAO.class));
        Runnable timerAction = controller.getCollector();
        timerAction.run();
    }

    private void assertTimeStampIsAround(long expected, long actual) {
        assertTrue(actual <= expected + 500);
        assertTrue(actual >= expected - 500);
    }
}
