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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView.Action;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class ObjectRootsFrameTest {

    private ObjectRootsFrame frame;
    private FrameFixture frameFixture;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        frame = GuiActionRunner.execute(new GuiQuery<ObjectRootsFrame>() {
            @Override
            protected ObjectRootsFrame executeInEDT() throws Throwable {
                return new ObjectRootsFrame();
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
        frame.hideView();
    }

    @GUITest
    @Test
    public void verifyShowHide() {
        frame.showView();

        frameFixture.requireVisible();

        frame.hideView();

        frameFixture.requireNotVisible();
    }

    @GUITest
    @Test
    public void verifySetPath() {
        List<HeapObjectUI> path = new ArrayList<>();
        path.add(new HeapObjectUI("test", "test"));
        path.add(new HeapObjectUI("test2", "test2"));
        frame.setPathToRoot(path);

        frame.showView();

        frameFixture.requireVisible();

        JTreeFixture tree = frameFixture.tree(ObjectRootsFrame.TREE_NAME);

        TreePath[] paths = getRows(tree.component());

        assertEquals(path.get(0), getUserObject(paths[0]));
        assertEquals(path.get(1), getUserObject(paths[1]));
    }

    private static Object getUserObject(TreePath path) {
        return ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
    }

    private static TreePath[] getRows(JTree tree) {
        List<TreePath> paths = new ArrayList<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            paths.add(tree.getPathForRow(i));
        }
        return paths.toArray(new TreePath[0]);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @GUITest
    @Test
    public void verifyTreeInteraction() {
        frame.showView();

        frameFixture.requireVisible();

        ActionListener<ObjectRootsView.Action> listener = mock(ActionListener.class);
        frame.addActionListener(listener);

        List<HeapObjectUI> path = new ArrayList<>();
        path.add(new HeapObjectUI("test", "test"));
        frame.setPathToRoot(path);

        JTreeFixture tree = frameFixture.tree(ObjectRootsFrame.TREE_NAME);
        tree.selectRow(0);

        ArgumentCaptor<ActionEvent> argCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        // We really want to verify that there's exactly one actionPerformend() call.
        // If there are more events (such as VISIBLE events) fired, this may be a recipe
        // for desaster if visble events trigger other actions, such as spawning a thread or
        // starting/stopping a timer.
        verify(listener).actionPerformed(argCaptor.capture());

        assertEquals(frame, argCaptor.getValue().getSource());
        
        assertEquals(Action.OBJECT_SELECTED, argCaptor.getValue().getActionId());
        assertEquals(path.get(0), argCaptor.getValue().getPayload());
    }

    @GUITest
    @Test
    public void verifySetDetails() {
        String detailsText = "foo-bar";
        frame.setObjectDetails(detailsText);

        frame.showView();

        frameFixture.requireVisible();

        JTextComponentFixture textBox = frameFixture.textBox(ObjectRootsFrame.DETAILS_NAME);
        textBox.requireText(detailsText);
    }

}

