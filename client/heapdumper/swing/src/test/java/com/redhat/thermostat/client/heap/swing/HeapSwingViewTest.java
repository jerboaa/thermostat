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

package com.redhat.thermostat.client.heap.swing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Container;
import java.util.Arrays;
import java.util.List;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JListFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.heap.HeapView.HeapDumperAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.heap.HeapDump;

@RunWith(CacioFESTRunner.class)
public class HeapSwingViewTest {

    private HeapSwingView view;

    private FrameFixture frame;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }


    @Before
    public void setUp() throws Exception {
        GuiActionRunner.execute(new GuiTask() {
            
            @Override
            protected void executeInEDT() throws Throwable {
                view = new HeapSwingView();
            }
        });
        frame = Containers.showInFrame((Container) view.getUiComponent());
    }

    @After
    public void tearDown() {
        frame.cleanUp();
        frame = null;
        view = null;
    }

    @Test
    @GUITest
    public void testActivateHeapDump() {
        @SuppressWarnings("unchecked")
        ActionListener<HeapDumperAction> l = mock(ActionListener.class);
        view.addDumperListener(l);
        JButtonFixture heapDumpButton = frame.button("heapDumpButton");
        heapDumpButton.click();
        verify(l).actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));
    }

    @Test
    @GUITest
    public void testNotifyHeapDumpComplete() {
        final JButtonFixture heapDumpButton = frame.button("heapDumpButton");
        GuiActionRunner.execute(new GuiTask() {
            
            @Override
            protected void executeInEDT() throws Throwable {
                heapDumpButton.component().setEnabled(false);
            }
        });

        view.notifyHeapDumpComplete();
        frame.robot.waitForIdle();

        heapDumpButton.requireEnabled();
    }

    @Test
    @GUITest
    public void testUpdateHeapDumpList() {
        JListFixture heapDumpList = frame.list("heapDumpList");
        heapDumpList.requireItemCount(0);

        HeapDump heapDump = mock(HeapDump.class);
        List<HeapDump> heapDumps = Arrays.asList(heapDump);

        view.updateHeapDumpList(heapDumps);
        frame.robot.waitForIdle();
        heapDumpList.requireItemCount(1);

        view.updateHeapDumpList(heapDumps);
        frame.robot.waitForIdle();
        heapDumpList.requireItemCount(1);

    }
}
