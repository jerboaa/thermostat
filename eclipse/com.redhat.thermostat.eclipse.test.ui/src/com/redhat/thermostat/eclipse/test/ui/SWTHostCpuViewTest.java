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
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.LocalizedString;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.SWTHostCpuView;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SWTHostCpuViewTest {
    private static final long TIMEOUT = 5000L;
    private SWTWorkbenchBot bot;
    private SWTHostCpuView view;
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
                view = new SWTHostCpuView(parent);
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
    public void testSetCpuModel() throws Exception {
        String model = "Test CPU";

        view.setCpuModel(model);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostCpuView.TEST_ID_CPU_MODEL);
                return !label.getText().equals("Unknown"); // TODO Externalize
            }

            @Override
            public String getFailureMessage() {
                return "CPU Model label unchanged after set";
            }

        });
    }

    @Test
    public void testSetCpuCount() throws Exception {
        String count = "8";

        view.setCpuCount(count);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostCpuView.TEST_ID_CPU_COUNT);
                return !label.getText().equals("Unknown"); // TODO Externalize
            }

            @Override
            public String getFailureMessage() {
                return "CPU Model label unchanged after set";
            }

        });
    }

    @Test
    public void testAddCpuUsageChart() throws Exception {
        String humanReadableName = "Test";

        // Test series added
        addSeries(0, humanReadableName, 1);

        // Verify legend added
        SWTBotLabel label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostCpuView.TEST_ID_LEGEND_ITEM);
        assertEquals(humanReadableName, label.getText());
    }

    private void addSeries(int seriesIndex, String humanReadableName,
            final int numSeries) {
        view.addCpuUsageChart(seriesIndex, new LocalizedString(humanReadableName));

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                XYPlot plot = (XYPlot) chart.getPlot();
                int count = plot.getSeriesCount();
                return count == numSeries;
            }

            @Override
            public String getFailureMessage() {
                return "Data series never added";
            }
        });

        // Wait until legend added
        bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostCpuView.TEST_ID_LEGEND_ITEM);
    }

    @Test
    public void testAddCpuUsageChartMultiple() throws Exception {
        String humanReadableName1 = "Test 1";
        String humanReadableName2 = "Test 2";

        addSeries(0, humanReadableName1, 1);
        addSeries(1, humanReadableName2, 2);

        // Verify legend added
        SWTBotLabel label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostCpuView.TEST_ID_LEGEND_ITEM, 0);
        assertEquals(humanReadableName1, label.getText());
        label = bot.labelWithId(ThermostatConstants.TEST_TAG,
                SWTHostCpuView.TEST_ID_LEGEND_ITEM, 1);
        assertEquals(humanReadableName2, label.getText());
    }

    @Test
    public void testAddCpuUsageData() throws Exception {
        List<DiscreteTimeData<Double>> data = new ArrayList<DiscreteTimeData<Double>>();

        data.add(new DiscreteTimeData<Double>(1000L, 50D));
        data.add(new DiscreteTimeData<Double>(2000L, 75D));
        data.add(new DiscreteTimeData<Double>(3000L, 25D));

        addSeries(0, "Test", 1);

        addData(0, data);

        JFreeChart chart = view.getChart();
        TimeSeriesCollection dataset = (TimeSeriesCollection) chart.getXYPlot()
                .getDataset();

        assertEquals(1000L, dataset.getX(0, 0));
        assertEquals(2000L, dataset.getX(0, 1));
        assertEquals(3000L, dataset.getX(0, 2));

        assertEquals(50D, dataset.getY(0, 0));
        assertEquals(75D, dataset.getY(0, 1));
        assertEquals(25D, dataset.getY(0, 2));
    }

    private void addData(final int seriesIndex,
            final List<DiscreteTimeData<Double>> data) {
        view.addCpuUsageData(seriesIndex, data);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                TimeSeriesCollection dataset = (TimeSeriesCollection) chart
                        .getXYPlot().getDataset();
                return dataset.getItemCount(seriesIndex) == data.size();
            }

            @Override
            public String getFailureMessage() {
                return "Data not added";
            }
        });
    }

    @Test
    public void testAddCpuUsageDataMultiple() throws Exception {
        List<DiscreteTimeData<Double>> data1 = new ArrayList<DiscreteTimeData<Double>>();

        data1.add(new DiscreteTimeData<Double>(1000L, 50D));
        data1.add(new DiscreteTimeData<Double>(2000L, 75D));
        data1.add(new DiscreteTimeData<Double>(3000L, 25D));

        List<DiscreteTimeData<Double>> data2 = new ArrayList<DiscreteTimeData<Double>>();

        data2.add(new DiscreteTimeData<Double>(1500L, 30D));
        data2.add(new DiscreteTimeData<Double>(2500L, 60D));
        data2.add(new DiscreteTimeData<Double>(3500L, 90D));

        addSeries(0, "Test 1", 1);
        addSeries(1, "Test 2", 2);

        addData(0, data1);
        addData(1, data2);

        JFreeChart chart = view.getChart();
        TimeSeriesCollection dataset = (TimeSeriesCollection) chart.getXYPlot()
                .getDataset();

        assertEquals(1000L, dataset.getX(0, 0));
        assertEquals(2000L, dataset.getX(0, 1));
        assertEquals(3000L, dataset.getX(0, 2));

        assertEquals(50D, dataset.getY(0, 0));
        assertEquals(75D, dataset.getY(0, 1));
        assertEquals(25D, dataset.getY(0, 2));

        assertEquals(1500L, dataset.getX(1, 0));
        assertEquals(2500L, dataset.getX(1, 1));
        assertEquals(3500L, dataset.getX(1, 2));

        assertEquals(30D, dataset.getY(1, 0));
        assertEquals(60D, dataset.getY(1, 1));
        assertEquals(90D, dataset.getY(1, 2));
    }

    @Test
    public void testClearCpuUsageData() {
        List<DiscreteTimeData<Double>> data1 = new ArrayList<DiscreteTimeData<Double>>();

        data1.add(new DiscreteTimeData<Double>(1000L, 50D));
        data1.add(new DiscreteTimeData<Double>(2000L, 75D));
        data1.add(new DiscreteTimeData<Double>(3000L, 25D));

        List<DiscreteTimeData<Double>> data2 = new ArrayList<DiscreteTimeData<Double>>();

        data2.add(new DiscreteTimeData<Double>(1500L, 30D));
        data2.add(new DiscreteTimeData<Double>(2500L, 60D));
        data2.add(new DiscreteTimeData<Double>(3500L, 90D));

        addSeries(0, "Test 1", 1);
        addSeries(1, "Test 2", 2);

        addData(0, data1);
        addData(1, data2);

        view.clearCpuUsageData();

        // Wait until data cleared
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                int count = chart.getXYPlot().getSeriesCount();
                return count == 0;
            }

            @Override
            public String getFailureMessage() {
                return "Data not cleared";
            }
        });
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

