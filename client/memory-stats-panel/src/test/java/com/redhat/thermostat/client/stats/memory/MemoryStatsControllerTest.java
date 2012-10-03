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

package com.redhat.thermostat.client.stats.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;

public class MemoryStatsControllerTest {

    private List<Generation> generations = new ArrayList<>();
    
    private MemoryStatsView view;
    private Timer timer;
    
    private ActionListener<MemoryStatsView.Action> viewListener;
    
    private MemoryStatsController controller;
    
    private Space canary;
    
    @Before
    public void setUp() {
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());
        
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
        
        List<VmMemoryStat> vmInfo = new ArrayList<>();
        
        for (int i = 0; i < 2; i++) {
            Generation generation = new Generation();
            generation.name = "fluff" + i;
            generation.spaces = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                Space space = new Space();
                space.name = "fluffer" + i + j;
                space.used = 100;
                space.capacity = 1000;
                space.maxCapacity = 10000;
                
                generation.spaces.add(space);
            }
            generations.add(generation);
        }
        
        // special payload because the others have all the same values
        canary = new Space();
        canary.name = "canary";
        canary.used = 1;
        canary.capacity = 2;
        canary.maxCapacity = 3;
        
        long timestamp = 1;
        int vmID = 1;
        for (int i = 0; i < 5; i++) {
            VmMemoryStat vmMemory = new VmMemoryStat(timestamp++, vmID, generations);
            vmInfo.add(vmMemory);
        }
        
        generations.get(0).spaces.add(canary);
        
        VmMemoryStatDAO memoryStatDao = mock(VmMemoryStatDAO.class);
        when(memoryStatDao.getLatestVmMemoryStats(any(VmRef.class), anyLong())).thenReturn(vmInfo);
        
        view = mock(MemoryStatsView.class);
        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(MemoryStatsView.class))).thenReturn(view);
        ApplicationContext.getInstance().setViewFactory(viewFactory);
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor =
                ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        VmRef ref = mock(VmRef.class);
        
        controller = new MemoryStatsController(memoryStatDao, ref);
        
        viewListener = viewArgumentCaptor.getValue();
    }
    
    @Test
    public void testStartStopTimer() {
        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.VISIBLE));

        verify(timer).start();
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);

        viewListener.actionPerformed(new ActionEvent<>(view, MemoryStatsView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void testPayloadContainSpaces() {
        MemoryStatsController.VMCollector collettor = controller.getCollector();
        collettor.run();
        
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
        MemoryStatsController.VMCollector collettor = controller.getCollector();
        collettor.run();
        
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
        String tooltip = payload.getName() + ": used: " + payload.getUsed() + " " +
                         payload.getUsedUnit() + ", capacity: " +
                         payload.getCapacity() + " " + payload.getUsedUnit() +
                         ", max capacity: " + payload.getMaxCapacity() + " " +
                         payload.getUsedUnit();
        
        assertEquals(tooltip, payload.getTooltip());
    }
    
    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }
}
