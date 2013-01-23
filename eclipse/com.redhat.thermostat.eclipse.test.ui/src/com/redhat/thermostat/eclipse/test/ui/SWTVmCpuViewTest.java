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
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.eclipse.chart.common.SWTVmCpuView;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class SWTVmCpuViewTest {
    private static final long TIMEOUT = 5000L;
    private SWTWorkbenchBot bot;
    private SWTVmCpuView view;
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
                view = new SWTVmCpuView(parent);
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
    public void testAddData() {
        List<DiscreteTimeData<? extends Number>> data = new ArrayList<DiscreteTimeData<? extends Number>>();
        
        data.add(new DiscreteTimeData<Number>(1000L, 20));
        data.add(new DiscreteTimeData<Number>(2000L, 80));
        data.add(new DiscreteTimeData<Number>(3000L, 50));
        
        addSeries(data);
        
        JFreeChart chart = view.getChart();
        XYDataset dataset = chart.getXYPlot().getDataset();
        assertEquals(1000L, dataset.getX(0, 0));
        assertEquals(2000L, dataset.getX(0, 1));
        assertEquals(3000L, dataset.getX(0, 2));
        
        assertEquals(20, dataset.getY(0, 0));
        assertEquals(80, dataset.getY(0, 1));
        assertEquals(50, dataset.getY(0, 2));
    }

    private void addSeries(List<DiscreteTimeData<? extends Number>> data) {
        view.addData(data);
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                return chart.getXYPlot().getDataset().getItemCount(0) == 3;
            }
            
            @Override
            public String getFailureMessage() {
                return "Data never added";
            }
        });
    }

    @Test
    public void testClearData() {
        List<DiscreteTimeData<? extends Number>> data = new ArrayList<DiscreteTimeData<? extends Number>>();
        
        data.add(new DiscreteTimeData<Number>(1000L, 20));
        data.add(new DiscreteTimeData<Number>(2000L, 80));
        data.add(new DiscreteTimeData<Number>(3000L, 50));
        
        addSeries(data);
        
        view.clearData();
        
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                JFreeChart chart = view.getChart();
                return chart.getXYPlot().getDataset().getItemCount(0) == 0;
            }
            
            @Override
            public String getFailureMessage() {
                return "Data never cleared";
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

