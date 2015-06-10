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

package com.redhat.thermostat.client.swing.components;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class RecentTimeSeriesChartPanelTest {

    private RecentTimeSeriesChartPanel recentTimeSeriesChartPanel;
    private RecentTimeSeriesChartController controller;
    private JFreeChart chart;
    private final TimeSeriesCollection dataset = new TimeSeriesCollection();
    private final int ESCAPE = 27;

    private JFrame frame;
    private FrameFixture frameFixture;

    @BeforeClass
    public static void setupOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        chart = ChartFactory.createTimeSeriesChart(
                null,
                "Time Label",
                "Value Label",
                dataset,
                false, false, false);
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                controller = new RecentTimeSeriesChartController(chart);
                recentTimeSeriesChartPanel = new RecentTimeSeriesChartPanel(controller);
                frame = new JFrame();
                frame.add(recentTimeSeriesChartPanel);
            }
        });
        frameFixture = new FrameFixture(frame);
        frameFixture.show();
    }

    @After
    public void tearDown() {
        recentTimeSeriesChartPanel = null;
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @GUITest
    @Test
    public void testDurationChangeFiresPropertyChange() {

        frameFixture.textBox("durationSelector").selectAll();
        frameFixture.textBox("durationSelector").enterText("5");
        frameFixture.textBox("durationSelector").pressKey(ESCAPE);
        assertEquals(5, controller.getTimeValue());
        assertEquals(TimeUnit.MINUTES, controller.getTimeUnit());
    }

    @GUITest
    @Test
    public void testTimeUnitChangeFiresPropertyChange() {

        frameFixture.comboBox("unitSelector").selectItem(1);
        assertEquals(10, controller.getTimeValue());
        assertEquals(TimeUnit.HOURS, controller.getTimeUnit());
    }

    @GUITest
    @Test
    public void testSetDataInformationLabel() {
        recentTimeSeriesChartPanel.setDataInformationLabel("15");
        assertEquals("15", frameFixture.textBox("crossHair").text());
    }
}
