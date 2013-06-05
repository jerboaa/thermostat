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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Container;
import java.util.concurrent.CountDownLatch;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JPanelFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView.HeapDumperAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.chart.OverviewChart;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapChartPanel;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.OverlayComponent;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

@RunWith(CacioFESTRunner.class)
public class HeapSwingViewTest {

    private HeapSwingView view;

    private FrameFixture frame;

    private OverviewChart model;
    
    private long now;
    
    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() throws Exception {
        
        now = 1000;
        
        GuiActionRunner.execute(new GuiTask() {
            
            @Override
            protected void executeInEDT() throws Throwable {
                view = new HeapSwingView();
                
                model = new OverviewChart("fluff", "fluff", "fluff", "fluff", "fluff");                
                model.addData(now, 1, 1000);
                model.addData(now + 10000, 1, 1000);
                view.setModel(model);
            }
        });
        frame = Containers.showInFrame((Container) view.getUiComponent());
    }

    @After
    public void tearDown() {
        frame.cleanUp();
        frame = null;
        view = null;
    }

    @GUITest
    @Test
    public void testAddHeapDump() {
        final boolean [] result = new boolean[1];
        final int [] resultTimes = new int[1];
        view.addDumperListener(new ActionListener<HeapDumperAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumperAction> actionEvent) {
                if (actionEvent.getActionId() == HeapDumperAction.DUMP_REQUESTED) {
                    result[0] = true;
                    resultTimes[0]++;
                }
            }
        });
        
        frame.show();
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                model.addData(now + 20000, 1, 1000);
            }
        });
        
        assertFalse(result[0]);
        
        JPanelFixture panel = frame.panel(HeapChartPanel.class.getName());
        JPopupMenuFixture popup = panel.showPopupMenu();
        popup.click();
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                model.addData(now + 30000, 1, 1000);
            }
        });
        
        assertTrue(result[0]);
        assertEquals(1, resultTimes[0]);
    }
    
    @GUITest
    @Test
    public void testActivateHeapDump() throws InterruptedException {
        final boolean [] result = new boolean[1];
        final int [] resultTimes = new int[1];
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        HeapInfo info = mock(HeapInfo.class);
        when(info.getTimeStamp()).thenReturn(now + 10000);
        final HeapDump dump = new HeapDump(info, null);
        
        view.addDumperListener(new ActionListener<HeapDumperAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumperAction> actionEvent) {
                if (actionEvent.getActionId() == HeapDumperAction.ANALYSE) {
                    result[0] = true;
                    resultTimes[0]++;
                } else {
                    view.addHeapDump(dump);
                    latch.countDown();
                }
            }
        });
        
        frame.show();

        // really same as previous test, this time we get the overlay component
        // though
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                model.addData(now + 20000, 1, 1000);
            }
        });
        
        assertFalse(result[0]); 
       
        final JPanelFixture panel = frame.panel(HeapChartPanel.class.getName());
        JPopupMenuFixture popup = panel.showPopupMenu();
        popup.click();
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                // needs this because OverlayComponent and HeapChartPanel are
                // bound by a special layout manager
                panel.component().doLayout();
            }
        });
        
        latch.await();
        
        JLabelFixture overlay =  panel.label(OverlayComponent.class.getName());
        overlay.doubleClick();
        
        OverlayComponent overlayComponent = (OverlayComponent) overlay.component();
        assertEquals(dump, overlayComponent.getHeapDump());
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                panel.component().doLayout();
            }
        });
        
        assertTrue(result[0]);
        assertEquals(1, resultTimes[0]);
    }
    
    @GUITest
    @Test
    public void testHeapDumperActionFired() {
        
        final boolean [] result = new boolean[1];
        view.addDumperListener(new ActionListener<HeapView.HeapDumperAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumperAction> actionEvent) {
                result[0] = true;
            }
        });
        
        frame.show();
        
        JToggleButtonFixture listDupms = frame.toggleButton("LIST_DUMPS_ACTION");
        listDupms.click();

        assertTrue(result[0]);
    }
}

