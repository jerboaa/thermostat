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

package com.redhat.thermostat.eclipse.test.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.IntervalXYDataset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.eclipse.chart.common.SWTVmGcView;
import com.redhat.thermostat.storage.model.IntervalTimeData;

public class SWTVmGcViewTest {
    private SWTWorkbenchBot bot;
    private SWTVmGcView view;
    private Shell shell;

    @Before
    public void beforeTest() throws Exception {
        bot = new SWTWorkbenchBot();
        
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                shell = new Shell(Display.getCurrent());
                Composite parent = new Composite(shell, SWT.NONE);
                parent.setLayout(new GridLayout());
                parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                        true));
                view = new SWTVmGcView();
                view.createControl(parent);
                shell.open();
            }
        });
    }

    @After
    public void afterTest() throws Exception {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                if (shell != null) {
                    shell.close();
                    view = null;
                }
            }
        });
    }

    @Test
    public void testAddChart() {
        addChart("TESTGC", "Test GC");
    }

    private void addChart(final String tag, String name) {
        view.addChart(tag, name, "ms");
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                return view.getChart(tag) != null;
            }
            
            @Override
            public String getFailureMessage() {
                return "Chart not added";
            }
        });
    }
    
    @Test
    public void testAddChartMultiple() {
        String tag = "TESTGC1";
        addChart(tag, "Test GC 1");
        addChart("TESTGC2", "Test GC 2");
        
        // Ensure first is still there
        assertNotNull(view.getChart(tag));
    }

    @Test
    public void testRemoveChart() {
        final String tag1 = "TESTGC1";
        addChart(tag1, "Test GC 1");
        String tag2 = "TESTGC2";
        addChart(tag2, "Test GC 2");
        
        view.removeChart(tag1);
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                return view.getChart(tag1) == null;
            }
            
            @Override
            public String getFailureMessage() {
                return "Chart not removed";
            }
        });
        
        // Ensure other chart still there
        assertNotNull(view.getChart(tag2));
    }

    @Test
    public void testAddData() {
        final String tag = "TESTGC";
        addChart(tag, "Test GC");
        
        List<IntervalTimeData<Double>> data = new ArrayList<IntervalTimeData<Double>>();
        data.add(new IntervalTimeData<Double>(1000L, 1500L, 100D));
        data.add(new IntervalTimeData<Double>(1500L, 2000L, 120D));
        data.add(new IntervalTimeData<Double>(2000L, 2500L, 170D));
        
        addData(tag, data);
        
        JFreeChart chart = view.getChart(tag);
        IntervalXYDataset dataset = (IntervalXYDataset) chart.getXYPlot().getDataset();
        
        assertEquals(1000L, dataset.getStartX(0, 0));
        assertEquals(1500L, dataset.getEndX(0, 0));
        assertEquals(100D, dataset.getY(0, 0));
        
        assertEquals(1500L, dataset.getStartX(0, 1));
        assertEquals(2000L, dataset.getEndX(0, 1));
        assertEquals(120D, dataset.getY(0, 1));
        
        assertEquals(2000L, dataset.getStartX(0, 2));
        assertEquals(2500L, dataset.getEndX(0, 2));
        assertEquals(170D, dataset.getY(0, 2));
    }
    
    @Test
    public void testAddDataMultiple() {
        final String tag1 = "TESTGC1";
        addChart(tag1, "Test GC1");
        final String tag2 = "TESTGC2";
        addChart(tag2, "Test GC2");
        
        List<IntervalTimeData<Double>> data1 = new ArrayList<IntervalTimeData<Double>>();
        data1.add(new IntervalTimeData<Double>(1000L, 1500L, 100D));
        data1.add(new IntervalTimeData<Double>(1500L, 2000L, 120D));
        data1.add(new IntervalTimeData<Double>(2000L, 2500L, 170D));
        
        addData(tag1, data1);
        
        List<IntervalTimeData<Double>> data2 = new ArrayList<IntervalTimeData<Double>>();
        data2.add(new IntervalTimeData<Double>(1200L, 1700L, 140D));
        data2.add(new IntervalTimeData<Double>(1700L, 2200L, 130D));
        data2.add(new IntervalTimeData<Double>(2200L, 2700L, 190D));
        
        addData(tag2, data2);
        
        JFreeChart chart1 = view.getChart(tag1);
        IntervalXYDataset dataset1 = (IntervalXYDataset) chart1.getXYPlot().getDataset();
        
        assertEquals(1000L, dataset1.getStartX(0, 0));
        assertEquals(1500L, dataset1.getEndX(0, 0));
        assertEquals(100D, dataset1.getY(0, 0));
        
        assertEquals(1500L, dataset1.getStartX(0, 1));
        assertEquals(2000L, dataset1.getEndX(0, 1));
        assertEquals(120D, dataset1.getY(0, 1));
        
        assertEquals(2000L, dataset1.getStartX(0, 2));
        assertEquals(2500L, dataset1.getEndX(0, 2));
        assertEquals(170D, dataset1.getY(0, 2));
        
        JFreeChart chart2 = view.getChart(tag2);
        IntervalXYDataset dataset2 = (IntervalXYDataset) chart2.getXYPlot().getDataset();
        
        assertEquals(1200L, dataset2.getStartX(0, 0));
        assertEquals(1700L, dataset2.getEndX(0, 0));
        assertEquals(140D, dataset2.getY(0, 0));
        
        assertEquals(1700L, dataset2.getStartX(0, 1));
        assertEquals(2200L, dataset2.getEndX(0, 1));
        assertEquals(130D, dataset2.getY(0, 1));
        
        assertEquals(2200L, dataset2.getStartX(0, 2));
        assertEquals(2700L, dataset2.getEndX(0, 2));
        assertEquals(190D, dataset2.getY(0, 2));
    }

    private void addData(final String tag,
            List<IntervalTimeData<Double>> data) {
        view.addData(tag, data);
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart(tag);
                return chart.getXYPlot().getDataset().getItemCount(0) == 3;
            }
            
            @Override
            public String getFailureMessage() {
                return "Data not added";
            }
        });
    }

    @Test
    public void testClearData() {
        final String tag1 = "TESTGC1";
        addChart(tag1, "Test GC1");
        final String tag2 = "TESTGC2";
        addChart(tag2, "Test GC2");
        
        List<IntervalTimeData<Double>> data1 = new ArrayList<IntervalTimeData<Double>>();
        data1.add(new IntervalTimeData<Double>(1000L, 1500L, 100D));
        data1.add(new IntervalTimeData<Double>(1500L, 2000L, 120D));
        data1.add(new IntervalTimeData<Double>(2000L, 2500L, 170D));
        
        addData(tag1, data1);
        
        List<IntervalTimeData<Double>> data2 = new ArrayList<IntervalTimeData<Double>>();
        data2.add(new IntervalTimeData<Double>(1200L, 1700L, 140D));
        data2.add(new IntervalTimeData<Double>(1700L, 2200L, 130D));
        data2.add(new IntervalTimeData<Double>(2200L, 2700L, 190D));
        
        addData(tag2, data2);
        
        // Remove data from the first chart
        view.clearData(tag1);
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart(tag1);
                return chart.getXYPlot().getDataset().getItemCount(0) == 0;
            }
            
            @Override
            public String getFailureMessage() {
                return "Data not removed";
            }
        });
        
        // Ensure other chart unchanged
        assertEquals(3, view.getChart(tag2).getXYPlot().getDataset().getItemCount(0));
    }

}
