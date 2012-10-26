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
