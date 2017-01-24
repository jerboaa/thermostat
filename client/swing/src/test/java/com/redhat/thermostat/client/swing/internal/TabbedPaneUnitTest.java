/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.shared.locale.LocalizedString;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

import static junit.framework.Assert.assertEquals;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class TabbedPaneUnitTest {

    private JFrame frame;
    private FrameFixture frameFixture;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    private TabbedPane pane;

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                frame.getContentPane().setLayout(new BorderLayout());
                pane = new TabbedPane();
                frame.add(pane);
                frame.setSize(500, 500);
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @Test
    @GUITest
    @RunsInEDT
    public void testAddTabs() {

        frameFixture.show();

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                pane.add(createContent("__test_1__"));
                pane.add(createContent("__test_2__"));
            }
        });

        assertEquals("__test_1__", pane.getTabs().get(0).getTabName().getContents());
        assertEquals("__test_2__", pane.getTabs().get(1).getTabName().getContents());
        assertEquals(2, pane.getTabs().size());

        JLabelFixture labelFixture = frameFixture.label("__test_1__");
        labelFixture.requireVisible();

        final int[] selected = new int[] { -1 };

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                selected[0] = pane.getSelectedIndex();
            }
        });

        assertEquals(0, selected[0]);

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                pane.setSelectedIndex(1);
            }
        });

        JLabelFixture labelFixture2 = frameFixture.label("__test_2__");
        labelFixture2.requireVisible();

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                selected[0] = pane.getSelectedIndex();
            }
        });

        // be sure order didn't change
        assertEquals(1, selected[0]);
        assertEquals("__test_1__", pane.getTabs().get(0).getTabName().getContents());
        assertEquals("__test_2__", pane.getTabs().get(1).getTabName().getContents());
        assertEquals(2, pane.getTabs().size());
    }

    private Tab createContent(final String name) {

        JPanel contentPane = new JPanel() {
            {
                setBackground(Color.WHITE);
                setLayout(new BorderLayout());

                JLabel label = new JLabel(name);
                label.setName(name);
                add(label);
            }
        };

        return new Tab(contentPane, new LocalizedString(name));
    }
}