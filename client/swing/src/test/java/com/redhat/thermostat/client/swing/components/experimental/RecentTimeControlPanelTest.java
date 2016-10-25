/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JComboBox;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.common.Duration;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class RecentTimeControlPanelTest {

    private RecentTimeControlPanel recentTimeControlPanel;
    private Duration duration;
    private JFrame frame;
    private FrameFixture frameFixture;
    private final int ESCAPE = 27;

    @BeforeClass
    public static void setupOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        duration = new Duration(10, TimeUnit.MINUTES);

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                recentTimeControlPanel = new RecentTimeControlPanel(duration);
                frame = new JFrame();
                frame.add(recentTimeControlPanel);
            }
        });
        frameFixture = new FrameFixture(frame);
        frameFixture.show();
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @GUITest
    @Test
    public void testComboBoxHasAppropriateValues() {
        assertEquals(RecentTimeControlPanel.DEFAULT_TIMEUNITS.length, frameFixture.comboBox("unitSelector").contents().length);
        int length = RecentTimeControlPanel.DEFAULT_TIMEUNITS.length;
        for (int i = 0; i < length; i++) {
            assertEquals(RecentTimeControlPanel.DEFAULT_TIMEUNITS[i].toString(), frameFixture.comboBox("unitSelector").valueAt(i));
        }
    }

    @GUITest
    @Test
    public void testComboBoxSelecting() {
        int length = RecentTimeControlPanel.DEFAULT_TIMEUNITS.length;
        for (int i = 0; i < length; i++) {
            frameFixture.comboBox("unitSelector").selectItem(i);
            assertEquals(RecentTimeControlPanel.DEFAULT_TIMEUNITS[i].toString(), selectedItemOf(frameFixture.comboBox("unitSelector").component()));
        }
    }

    @GUITest
    @Test
    public void testTextFieldUpdates() {
        frameFixture.textBox("durationSelector").selectAll();
        frameFixture.textBox("durationSelector").enterText("5");
        frameFixture.textBox("durationSelector").pressKey(ESCAPE);
        assertEquals("5", frameFixture.textBox("durationSelector").text());
    }

    @GUITest
    @Test
    public void testTextFieldUpdatingFiresTimeChangeListener() {
        final boolean[] b = new boolean[] {false};

        recentTimeControlPanel.addPropertyChangeListener(ThermostatChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
              Duration d = (Duration) evt.getNewValue();
              Duration expected = new Duration(5, TimeUnit.MINUTES);
              if (expected.equals(d)) {
                  b[0] = true;
              }
            }
        });

        frameFixture.textBox("durationSelector").selectAll();
        frameFixture.textBox("durationSelector").enterText("5");
        frameFixture.textBox("durationSelector").pressKey(27);

        assertEquals("5", frameFixture.textBox("durationSelector").text());
        assertTrue(b[0]);
    }

    @GUITest
    @Test
    public void testComboBoxUpdatingFiresTimeChangeListener() {
        final boolean[] b = new boolean[] {false};

        recentTimeControlPanel.addPropertyChangeListener(ThermostatChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
              Duration d = (Duration) evt.getNewValue();
              Duration expected = new Duration(10, TimeUnit.HOURS);
              if (d.equals(expected)) {
                  b[0] = true;
              }
            }
        });

        frameFixture.comboBox("unitSelector").selectItem(1);

        assertTrue(b[0]);
    }
    public static String selectedItemOf(final JComboBox comboBox) {
        return GuiActionRunner.execute(new GuiQuery<String>() {
            public String executeInEDT() {
                return comboBox.getSelectedItem().toString();
            }
        });
    }
}
