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

import java.util.Arrays;
import java.util.List;
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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.RecentTimeSeriesChartComposite;

@RunWith(SWTBotJunit4ClassRunner.class)
public class RecentTimeSeriesChartCompositeTest {
    private SWTWorkbenchBot bot;
    private Shell shell;
    private JFreeChart chart;
    private RecentTimeSeriesChartComposite composite;

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

                chart = createChart();
                composite = new RecentTimeSeriesChartComposite(parent,
                        SWT.NONE, chart);
                shell.open();
            }
        });
    }

    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null,
                new TimeSeriesCollection(), false, false, false);
        return chart;
    }

    @After
    public void afterTest() throws Exception {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                if (shell != null) {
                    shell.close();
                }
            }
        });
    }

    @Test
    public void testTimeUnitMinutes() throws Exception {
        SWTBotCombo timeUnitCombo = bot.comboBoxWithId(
                ThermostatConstants.TEST_TAG,
                RecentTimeSeriesChartComposite.TEST_ID_UNIT_COMBO);
        
        List<TimeUnit> units = Arrays.asList(composite.getController().getTimeUnits());
        timeUnitCombo.setSelection(units.indexOf(TimeUnit.MINUTES));
        
        checkTimeUnit(TimeUnit.MINUTES);
    }
    
    @Test
    public void testTimeUnitHours() throws Exception {
        SWTBotCombo timeUnitCombo = bot.comboBoxWithId(
                ThermostatConstants.TEST_TAG,
                RecentTimeSeriesChartComposite.TEST_ID_UNIT_COMBO);
        
        List<TimeUnit> units = Arrays.asList(composite.getController().getTimeUnits());
        timeUnitCombo.setSelection(units.indexOf(TimeUnit.HOURS));
        
        checkTimeUnit(TimeUnit.HOURS);
    }
    
    @Test
    public void testTimeUnitDays() throws Exception {
        SWTBotCombo timeUnitCombo = bot.comboBoxWithId(
                ThermostatConstants.TEST_TAG,
                RecentTimeSeriesChartComposite.TEST_ID_UNIT_COMBO);
        
        List<TimeUnit> units = Arrays.asList(composite.getController().getTimeUnits());
        timeUnitCombo.setSelection(units.indexOf(TimeUnit.DAYS));
        
        checkTimeUnit(TimeUnit.DAYS);
    }
    
    @Test
    public void testTimeDuration() throws Exception {
        final int duration = 200;
        SWTBotText durationText = bot.textWithId(ThermostatConstants.TEST_TAG,
                RecentTimeSeriesChartComposite.TEST_ID_DURATION_TEXT);
        
        durationText.setText(String.valueOf(duration));
        
        checkTimeDuration(duration);
    }
    
    @Test(expected = TimeoutException.class)
    public void testTimeDurationBad() throws Exception {
        final int duration = 200;
        
        // Set a proper duration value, then try a bad one
        SWTBotText durationText = bot.textWithId(ThermostatConstants.TEST_TAG,
                RecentTimeSeriesChartComposite.TEST_ID_DURATION_TEXT);
        
        durationText.setText(String.valueOf(duration));
        
        checkTimeDuration(duration);
        
        durationText.setText("Not an int");
        
        // Ensure duration unchanged
        bot.waitWhile(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                return composite.getController().getTimeValue() == duration;
            }
            
            @Override
            public String getFailureMessage() {
                return "Success";
            }
        });
    }

    private void checkTimeDuration(final int duration) {
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                return composite.getController().getTimeValue() == duration;
            }
            
            @Override
            public String getFailureMessage() {
                return "Duration not set";
            }
        });
    }

    private void checkTimeUnit(final TimeUnit unit) {
        bot.waitUntil(new DefaultCondition() {
            
            @Override
            public boolean test() throws Exception {
                return unit.equals(composite.getController().getTimeUnit());
            }
            
            @Override
            public String getFailureMessage() {
                return "Time unit not set to " + unit.toString();
            }
        });
    }

}

