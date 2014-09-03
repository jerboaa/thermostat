/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JPanelFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

@RunWith(CacioFESTRunner.class)
public class HeapDumpListViewTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private SwingHeapDumpListView view;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new SwingHeapDumpListView();
                frame = new JFrame();
                frame.add(view.getUiComponent());

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
    public void testDumpDetailsFired() {
        final boolean [] result = new boolean[1];
        final HeapDump [] selectedHeap = new HeapDump[1];

        view.addListListener(new ActionListener<HeapDumpListView.ListAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumpListView.ListAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case OPEN_DUMP_DETAILS:
                        result[0] = true;
                        selectedHeap[0] = (HeapDump) actionEvent.getPayload();
                        break;
                }
            }
        });

        HeapDump dump1 = mock(HeapDump.class);
        when(dump1.toString()).thenReturn("dump1");

        List<HeapDump> dumps = new ArrayList<>();
        dumps.add(dump1);

        view.setDumps(dumps);

        JLabelFixture label = frameFixture.label("dump1_label");
        label.doubleClick();

        assertTrue(result[0]);
        assertEquals(dump1, selectedHeap[0]);
    }

    @GUITest
    @Test
    public void testDu1mpSelectFired() {
        final boolean [] result = new boolean[1];
        final JPanel[] selectedHeap = new JPanel[1];


        HeapDump dump1 = mock(HeapDump.class);
        when(dump1.toString()).thenReturn("dump1");

        List<HeapDump> dumps = new ArrayList<>();
        dumps.add(dump1);

        view.setDumps(dumps);

        JPanelFixture item = frameFixture.panel("dump1_panel");

        JLabelFixture label = frameFixture.label("dump1_label");
        label.click();

        JButtonFixture button = frameFixture.button("dump1_button");

        assertTrue(button.target.isVisible());
    }

    @GUITest
    @Test
    public void testExportFired() {
        final boolean [] result = new boolean[1];
        final HeapDump [] selectedHeap = new HeapDump[1];

        view.addListListener(new ActionListener<HeapDumpListView.ListAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeapDumpListView.ListAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case EXPORT_DUMP:
                        result[0] = true;
                        selectedHeap[0] = (HeapDump) actionEvent.getPayload();
                        break;
                }
            }
        });

        HeapDump dump1 = mock(HeapDump.class);
        when(dump1.toString()).thenReturn("dump1");

        List<HeapDump> dumps = new ArrayList<>();
        dumps.add(dump1);

        view.setDumps(dumps);

        JLabelFixture label = frameFixture.label("dump1_label");
        label.click();

        JButtonFixture button = frameFixture.button("dump1_button");
        button.click();

        assertTrue(result[0]);
        assertEquals(dump1, selectedHeap[0]);
    }
}

