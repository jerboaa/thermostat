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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.internal.views.SWTHostOverviewView;

public class SWTHostOverviewViewTest {
    private static final long TIMEOUT = 5000L;
    private SWTWorkbenchBot bot;
    private SWTHostOverviewView view;
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
                view = new SWTHostOverviewView(parent);
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
    public void testSetHostName() {
        final String hostname = "Test Host";

        view.setHostName(hostname);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_HOSTNAME);
                return label.getText().equals(hostname);
            }

            @Override
            public String getFailureMessage() {
                return "Hostname label not set";
            }

        });
    }

    @Test
    public void testSetCpuModel() {
        final String cpuModel = "Test CPU";

        view.setCpuModel(cpuModel);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_PROC_MODEL);
                return label.getText().equals(cpuModel);
            }

            @Override
            public String getFailureMessage() {
                return "CPU Model label not set";
            }

        });
    }

    @Test
    public void testSetCpuCount() {
        final String cpuCount = "8";

        view.setCpuCount(cpuCount);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_PROC_CORE_COUNT);
                return label.getText().equals(cpuCount);
            }

            @Override
            public String getFailureMessage() {
                return "CPU Count label not set";
            }

        });
    }

    @Test
    public void testSetTotalMemory() {
        final String totalMem = "100 TB";

        view.setTotalMemory(totalMem);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_TOTAL_MEMORY);
                return label.getText().equals(totalMem);
            }

            @Override
            public String getFailureMessage() {
                return "Total Memory label not set";
            }

        });
    }

    @Test
    public void testSetOsName() {
        final String osName = "Test OS";

        view.setOsName(osName);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_OS_NAME);
                return label.getText().equals(osName);
            }

            @Override
            public String getFailureMessage() {
                return "OS Name label not set";
            }

        });
    }

    @Test
    public void testSetOsKernel() {
        final String osKernel = "Test Kernel";

        view.setOsKernel(osKernel);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotLabel label = bot.labelWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_OS_KERNEL);
                return label.getText().equals(osKernel);
            }

            @Override
            public String getFailureMessage() {
                return "OS Kernel label not set";
            }

        });
    }

    @Test
    public void testSetNetworkTableColumns() {
        final Object[] columns = { "Col 1", "Col 2", "Col 3" };
        addColumns(columns);
        
        SWTBotTable table = bot.tableWithId(
                ThermostatConstants.TEST_TAG,
                SWTHostOverviewView.TEST_ID_NETWORK_INTERFACES);

        List<String> tableColumns = table.columns();
        assertEquals(columns[0], tableColumns.get(0));
        assertEquals(columns[1], tableColumns.get(1));
        assertEquals(columns[2], tableColumns.get(2));
    }

    protected void addColumns(final Object[] columns) {
        view.setNetworkTableColumns(columns);

        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotTable table = bot.tableWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_NETWORK_INTERFACES);

                return table.columnCount() == 3;
            }

            @Override
            public String getFailureMessage() {
                return "Network Interfaces columns not set";
            }

        });
    }

    @Test
    public void testSetInitialNetworkTableData() {
        final Object[] columns = { "Col 1", "Col 2", "Col 3" };
        final Object[][] data = { { "Iface 1", "IPv4 Addr 1", "IPv6 Addr 1" },
                { "Iface 2", "IPv4 Addr 2", "IPv6 Addr 2" },
                { "Iface 3", "IPv4 Addr 3", "IPv6 Addr 3" } };
        
        addColumns(columns);
        addInitialData(data);
        
        SWTBotTable table = bot.tableWithId(
                ThermostatConstants.TEST_TAG,
                SWTHostOverviewView.TEST_ID_NETWORK_INTERFACES);
        
        assertEquals(data[0][0], table.cell(0, 0));
        assertEquals(data[0][1], table.cell(0, 1));
        assertEquals(data[0][2], table.cell(0, 2));
        
        assertEquals(data[1][0], table.cell(1, 0));
        assertEquals(data[1][1], table.cell(1, 1));
        assertEquals(data[1][2], table.cell(1, 2));
        
        assertEquals(data[2][0], table.cell(2, 0));
        assertEquals(data[2][1], table.cell(2, 1));
        assertEquals(data[2][2], table.cell(2, 2));
    }

    protected void addInitialData(final Object[][] data) {
        view.setInitialNetworkTableData(data);
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotTable table = bot.tableWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_NETWORK_INTERFACES);

                return table.rowCount() == 3;
            }

            @Override
            public String getFailureMessage() {
                return "Network Interfaces inital data not set";
            }

        });
    }

    @Test
    public void testUpdateNetworkTableData() {
        final Object[] columns = { "Col 1", "Col 2", "Col 3" };
        final Object[][] data = { { "Iface 1", "IPv4 Addr 1", "IPv6 Addr 1" },
                { "Iface 2", "IPv4 Addr 2", "IPv6 Addr 2" },
                { "Iface 3", "IPv4 Addr 3", "IPv6 Addr 3" } };
        
        addColumns(columns);
        addInitialData(data);
        
        changeItem(0, 0, "Change 1");
        changeItem(0, 1, "Change 2");
        changeItem(2, 1, "Change 3");
        changeItem(1, 2, "Change 4");
        changeItem(2, 2, "Change 5");
    }

    protected void changeItem(final int row, final int col, final String item) {
        view.updateNetworkTableData(row, col, item);
        bot.waitUntil(new DefaultCondition() {

            @Override
            public boolean test() throws Exception {
                SWTBotTable table = bot.tableWithId(
                        ThermostatConstants.TEST_TAG,
                        SWTHostOverviewView.TEST_ID_NETWORK_INTERFACES);

                return table.cell(row, col).equals(item);
            }

            @Override
            public String getFailureMessage() {
                return "Network Interfaces inital data not set";
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

