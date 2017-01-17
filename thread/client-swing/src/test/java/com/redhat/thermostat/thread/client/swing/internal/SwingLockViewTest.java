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

package com.redhat.thermostat.thread.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.thread.model.LockInfo;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingLockViewTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private SwingLockView view;

    @BeforeClass
    public static void setupOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setup() {
        GuiActionRunner.execute(new GuiTask() {

            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                view = new SwingLockView();
                frame.add(view.getUiComponent());
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @GUITest
    @Category(GUITest.class)
    @Test
    public void verifyInitialValues() {
        frameFixture.show();

        String[][] contents = frameFixture.table(SwingLockView.TABLE_NAME).contents();
        for (int i = 0; i < 18; i++) {
            String label = contents[i][0];
            String value = contents[i][1];
            assertFalse(label.contains("LocalizedString"));
            assertEquals("0", value);
        }
    }

    @GUITest
    @Category(GUITest.class)
    @Test
    public void verifyTableIsUpdated() {
        frameFixture.show();

        LockInfo lockData = new LockInfo(System.currentTimeMillis(), "writer", "vm",
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        // incidentally, this also checks for correct handling of calls from non EDT
        view.setLatestLockData(lockData);

        String[][] contents = frameFixture.table(SwingLockView.TABLE_NAME).contents();
        for (int i = 0; i < 18; i++) {
            String value = contents[i][1];
            assertEquals(String.valueOf(i + 1), value);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame mainWindow = new JFrame("mainWindow");

                SwingLockView view = new SwingLockView();
                mainWindow.add(view.getUiComponent());

                LockInfo lockData = new LockInfo(System.currentTimeMillis(), "writer", "vm",
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

                view.setLatestLockData(lockData);
                mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainWindow.pack();
                mainWindow.setVisible(true);
            }
        });
    }
}
