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

package com.redhat.thermostat.eclipse.test.ui;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.WithId;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.SWTHostMemoryView;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView.GraphVisibilityChangeListener;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SWTHostMemoryViewTest implements GraphVisibilityChangeListener {
    private static final long TIMEOUT = 5000L;
    private SWTWorkbenchBot bot;
    private SWTHostMemoryView view;
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
                view = new SWTHostMemoryView(parent);
                view.addGraphVisibilityListener(SWTHostMemoryViewTest.this);
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
    public void testSetTotalMemory() throws Exception {
        String totalMem = "8 GB";

        view.setTotalMemory(totalMem);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                        SWTHostMemoryView.TEST_ID_TOTAL_MEM);
                return !label.getText().equals("Unknown"); // TODO Externalize
            }

            @Override
            public String getFailureMessage() {
                return "Total memory label unchanged after set";
            }

        });
    }

    @Test
    public void testAddMemoryChart() throws Exception {
        final String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);

        // Verify legend added
        SWTBotLabel label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL);
        assertEquals(humanReadableName, label.getText());
    }

    @Test
    public void testAddMemoryChartMultiple() throws Exception {
        final String tag1 = "TEST1";
        final String tag2 = "TEST2";
        String humanReadableName1 = "Test 1";
        String humanReadableName2 = "Test 2";

        addSeries(tag1, humanReadableName1);
        addSeries(tag2, humanReadableName2);

        // Verify legend added
        SWTBotLabel label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL, 0);
        assertEquals(humanReadableName1, label.getText());
        label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL, 1);
        assertEquals(humanReadableName2, label.getText());
    }

    @Test
    public void testRemoveMemoryChart() throws Exception {
        final String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);
        
        view.showMemoryChart(tag);
        waitUntilSeriesShown(1);

        view.removeMemoryChart(tag);

        checkSeriesRemoved(tag, 0);

        // Verify legend removed
        checkLegendRemoved();
    }

    @Test
    public void testRemoveMemoryChartMultiple() throws Exception {
        final String tag1 = "TEST1";
        final String tag2 = "TEST2";
        String humanReadableName1 = "Test 1";
        String humanReadableName2 = "Test 2";

        addSeries(tag1, humanReadableName1);
        addSeries(tag2, humanReadableName2);
        
        // Show both series
        view.showMemoryChart(tag1);
        waitUntilSeriesShown(1);

        view.showMemoryChart(tag2);
        waitUntilSeriesShown(2);

        view.removeMemoryChart(tag2);

        checkSeriesRemoved(tag2, 1);

        // Verify legend removed
        checkLegendItemRemoved(1);
    }

    private void checkSeriesRemoved(final String tag, final int numSeries) {
        // Wait until series removed from chart and dataset
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                XYPlot plot = chart.getXYPlot();
                int count = plot.getSeriesCount();
                return view.getSeries(tag) == null && count == numSeries;
            }

            @Override
            public String getFailureMessage() {
                return "Data series never removed";
            }
        });
    }

    private void checkLegendRemoved() {
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                boolean result = false;

                // Don't make this wait
                long saveTimeout = SWTBotPreferences.TIMEOUT;
                SWTBotPreferences.TIMEOUT = 0;
                try {
                    bot.widget(WithId.withId(ThermostatConstants.TEST_TAG,
                            SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL));
                } catch (WidgetNotFoundException e) {
                    result = true;
                }
                SWTBotPreferences.TIMEOUT = saveTimeout;

                return result;
            }

            @Override
            public String getFailureMessage() {
                return "Legend not removed";
            }
        });
    }
    
    private void checkLegendItemRemoved(final int size) {
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                List<? extends Widget> widgets = bot.widgets(WithId.withId(ThermostatConstants.TEST_TAG,
                        SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL));
                return widgets.size() == size;
            }

            @Override
            public String getFailureMessage() {
                return "Legend item not removed";
            }
        });
    }

    @Test
    public void testHideMemoryChart() throws Exception {
        String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);
        view.showMemoryChart(tag);

        waitUntilSeriesShown(1);

        // Click checkbox to trigger hideMemoryChart
        SWTBotCheckBox checkbox = bot.checkBoxWithId(
                ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_CHECKBOX);
        checkbox.click();

        // Wait until series hidden
        waitUntilSeriesShown(0);
    }

    @Test
    public void testShowMemoryChart() throws Exception {
        String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);
        view.showMemoryChart(tag);

        waitUntilSeriesShown(1);
    }

    @Test
    public void testShowHiddenMemoryChart() throws Exception {
        String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);
        view.showMemoryChart(tag);

        waitUntilSeriesShown(1);

        // Click checkbox to trigger hideMemoryChart
        SWTBotCheckBox checkbox = bot.checkBoxWithId(
                ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_CHECKBOX);
        checkbox.click();

        // Wait until series hidden
        waitUntilSeriesShown(0);

        // Click checkbox to trigger showMemoryChart
        checkbox.click();

        waitUntilSeriesShown(1);
    }
    
    @Test
    public void testAddMemoryData() throws Exception {
        String tag = "TEST";
        String humanReadableName = "Test";

        addSeries(tag, humanReadableName);
        view.showMemoryChart(tag);

        waitUntilSeriesShown(1);
        
        // Add some test data
        List<DiscreteTimeData<? extends Number>> data = new ArrayList<DiscreteTimeData<? extends Number>>();
        data.add(new DiscreteTimeData<Long>(1000L, 134217728L)); // 128MiB
        data.add(new DiscreteTimeData<Long>(2000L, 268435456L)); // 256MiB
        data.add(new DiscreteTimeData<Long>(3000L, 536870912L)); // 512MiB
        
        final JFreeChart chart = view.getChart();
        final TimeSeries series = view.getSeries(tag);
        
        addData(tag, data, series);
        
        TimeSeriesCollection dataset = (TimeSeriesCollection) chart.getXYPlot().getDataset();
        int seriesIndex = chart.getXYPlot().getDataset().indexOf(series.getKey());
        
        assertEquals(1000L, dataset.getX(seriesIndex, 0));
        assertEquals(2000L, dataset.getX(seriesIndex, 1));
        assertEquals(3000L, dataset.getX(seriesIndex, 2));
        
        assertEquals(128D, dataset.getY(seriesIndex, 0));
        assertEquals(256D, dataset.getY(seriesIndex, 1));
        assertEquals(512D, dataset.getY(seriesIndex, 2));
    }

    private void addData(String tag,
            final List<DiscreteTimeData<? extends Number>> data,
            final TimeSeries series) {
        view.addMemoryData(tag, data);

        // Wait until data added to chart
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                return series.getItemCount() == data.size();
            }

            @Override
            public String getFailureMessage() {
                return "Data never added";
            }
        });
    }
    
    @Test
    public void testAddMemoryDataMultiple() throws Exception {
        String tag1 = "TEST1";
        String tag2 = "TEST2";
        String humanReadableName1 = "Test 1";
        String humanReadableName2 = "Test 2";

        addSeries(tag1, humanReadableName1);
        view.showMemoryChart(tag1);
        
        addSeries(tag2, humanReadableName2);
        view.showMemoryChart(tag2);

        waitUntilSeriesShown(2);
        
        // Add some test data
        List<DiscreteTimeData<? extends Number>> data1 = new ArrayList<DiscreteTimeData<? extends Number>>();
        data1.add(new DiscreteTimeData<Long>(1000L, 134217728L)); // 128MiB
        data1.add(new DiscreteTimeData<Long>(2000L, 268435456L)); // 256MiB
        data1.add(new DiscreteTimeData<Long>(3000L, 536870912L)); // 512MiB
        
        List<DiscreteTimeData<? extends Number>> data2 = new ArrayList<DiscreteTimeData<? extends Number>>();
        data2.add(new DiscreteTimeData<Long>(1500L, 536870912L)); // 512MiB
        data2.add(new DiscreteTimeData<Long>(2500L, 134217728L)); // 128MiB
        data2.add(new DiscreteTimeData<Long>(3500L, 268435456L)); // 256MiB
        
        final JFreeChart chart = view.getChart();
        final TimeSeries series1 = view.getSeries(tag1);
        final TimeSeries series2 = view.getSeries(tag2);
        
        addData(tag1, data1, series1);
        addData(tag2, data2, series2);
        
        TimeSeriesCollection dataset = (TimeSeriesCollection) chart.getXYPlot().getDataset();
        int series1Index = chart.getXYPlot().getDataset().indexOf(series1.getKey());
        int series2Index = chart.getXYPlot().getDataset().indexOf(series2.getKey());
        
        assertEquals(1000L, dataset.getX(series1Index, 0));
        assertEquals(2000L, dataset.getX(series1Index, 1));
        assertEquals(3000L, dataset.getX(series1Index, 2));
        
        assertEquals(128D, dataset.getY(series1Index, 0));
        assertEquals(256D, dataset.getY(series1Index, 1));
        assertEquals(512D, dataset.getY(series1Index, 2));
        
        assertEquals(1500L, dataset.getX(series2Index, 0));
        assertEquals(2500L, dataset.getX(series2Index, 1));
        assertEquals(3500L, dataset.getX(series2Index, 2));
        
        assertEquals(512D, dataset.getY(series2Index, 0));
        assertEquals(128D, dataset.getY(series2Index, 1));
        assertEquals(256D, dataset.getY(series2Index, 2));
    }
    
    @Test
    public void testClearMemoryData() throws Exception {
        String tag1 = "TEST1";
        String tag2 = "TEST2";
        String humanReadableName1 = "Test 1";
        String humanReadableName2 = "Test 2";

        addSeries(tag1, humanReadableName1);
        view.showMemoryChart(tag1);
        
        addSeries(tag2, humanReadableName2);
        view.showMemoryChart(tag2);

        waitUntilSeriesShown(2);
        
        // Add some test data
        List<DiscreteTimeData<? extends Number>> data1 = new ArrayList<DiscreteTimeData<? extends Number>>();
        data1.add(new DiscreteTimeData<Long>(1000L, 134217728L)); // 128MiB
        data1.add(new DiscreteTimeData<Long>(2000L, 268435456L)); // 256MiB
        data1.add(new DiscreteTimeData<Long>(3000L, 536870912L)); // 512MiB
        
        List<DiscreteTimeData<? extends Number>> data2 = new ArrayList<DiscreteTimeData<? extends Number>>();
        data2.add(new DiscreteTimeData<Long>(1500L, 536870912L)); // 512MiB
        data2.add(new DiscreteTimeData<Long>(2500L, 134217728L)); // 128MiB
        data2.add(new DiscreteTimeData<Long>(3500L, 268435456L)); // 256MiB
        
        final TimeSeries series1 = view.getSeries(tag1);
        final TimeSeries series2 = view.getSeries(tag2);
        
        addData(tag1, data1, series1);
        addData(tag2, data2, series2);
        
        // Remove data from series
        view.clearMemoryData(tag1);
        
        // Wait until data removed to chart
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                return series1.getItemCount() == 0;
            }

            @Override
            public String getFailureMessage() {
                return "Data never added";
            }
        });
        
        // Check other series' size
        assertEquals(data2.size(), series2.getItemCount());
    }

    private void waitUntilSeriesShown(final int numSeries) {
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                XYPlot plot = chart.getXYPlot();
                int count = plot.getSeriesCount();
                return count == numSeries;
            }

            @Override
            public String getFailureMessage() {
                return "Data series never shown/hidden";
            }
        });
    }

    private void addSeries(final String tag, String humanReadableName) {
        view.addMemoryChart(tag, humanReadableName);

        // Wait until series added
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                return view.getSeries(tag) != null;
            }

            @Override
            public String getFailureMessage() {
                return "Data series never added";
            }
        });

        // Wait for legend
        bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostMemoryView.TEST_ID_LEGEND_ITEM_LABEL);
    }

    @Override
    public void show(String tag) {
        view.showMemoryChart(tag);
    }

    @Override
    public void hide(String tag) {
        view.hideMemoryChart(tag);
    }
    
    @Test
    public void testShowView() throws Exception {
        final Action[] action = new Action[1];
        final CountDownLatch latch = new CountDownLatch(1);
        view.addActionListener(new ActionListener<BasicView.Action>() {
            
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                action[0] = actionEvent.getActionId();
                latch.countDown();
            }
        });
        
        view.show();
        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(Action.VISIBLE, action[0]);
    }
    
    @Test
    public void testHideView() throws Exception {
        final Action[] action = new Action[1];
        final CountDownLatch latch = new CountDownLatch(1);
        view.addActionListener(new ActionListener<BasicView.Action>() {
            
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                action[0] = actionEvent.getActionId();
                latch.countDown();
            }
        });
        
        view.hide();
        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(Action.HIDDEN, action[0]);
    }
}

