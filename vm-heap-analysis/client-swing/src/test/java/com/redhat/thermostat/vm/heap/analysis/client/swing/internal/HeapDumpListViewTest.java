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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

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
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

@Category(CacioTest.class)
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
    public void testDumpSelectFired() {
        HeapDump dump1 = mock(HeapDump.class);
        when(dump1.toString()).thenReturn("dump1");

        List<HeapDump> dumps = new ArrayList<>();
        dumps.add(dump1);

        view.setDumps(dumps);

        JLabelFixture label = frameFixture.label("dump1_label");
        label.click();

        JButtonFixture button = frameFixture.button("dump1_button");

        assertTrue(button.target.isVisible());
    }
    
    @GUITest
    @Test
    public void testDumpDetailsSorted() {
        HeapDump dump1 = mock(HeapDump.class);
        when(dump1.toString()).thenReturn("dump1");
        when(dump1.getTimestamp()).thenReturn(100L);
        HeapDump dump2 = mock(HeapDump.class);
        when(dump2.toString()).thenReturn("dump2");
        when(dump2.getTimestamp()).thenReturn(1000L);

        // Add dumps in ascending order so that it will get reversed when
        // sorting.
        List<HeapDump> dumps = new ArrayList<>();
        dumps.add(dump1);
        dumps.add(dump2);

        final CountDownLatch latch = new CountDownLatch(1);
        Runnable callBack = new Runnable() {
            
            @Override
            public void run() {
                latch.countDown();
            }
        };
        // Call with callback so as to avoid races
        view.setDumps(dumps, callBack);
        boolean latchExpired = false;
        try {
            latchExpired = !(latch.await(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            // ignored
        }
        assertFalse("Timeout waiting for invokelater task to execute", latchExpired);

        JPanelFixture table = frameFixture.panel("_heapdump_table_list");
        
        ByteArrayOutputStream baOut = new ByteArrayOutputStream();
        // List the component so as to be able to make assertions about its
        // content.
        table.target.list(new PrintStream(baOut));
        
        ByteArrayInputStream baIn = new ByteArrayInputStream(baOut.toByteArray());
        Scanner scanner = new Scanner(baIn);
        int lineIdx = 0;
        int dump1Order = -1;
        int dump2Order = -1;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("dump1_label")) {
                if (dump1Order == -1) {
                    dump1Order = lineIdx;
                } else {
                    fail("Did not expect dump1_label to be found twice");
                }
            }
            if (line.contains("dump2_label")) {
                if (dump2Order == -1) {
                    dump2Order = lineIdx;
                } else {
                    fail("Did not expect dump2_label to be found twice");
                }
            }
            lineIdx++;
        }
        scanner.close();
        assertTrue("dump1_label not found", dump1Order != -1);
        assertTrue("dump2_label not found", dump2Order != -1);
        String failMsg = "Expected dump2 to be first in list. got (dump1 = " + 
                        dump1Order + ") <= (dump2 = " + dump2Order + ")";
        assertTrue(failMsg, dump1Order > dump2Order);
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

