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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.swing.components.SearchField;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView.ObjectAction;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.ObjectDetailsPanel;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

@RunWith(CacioFESTRunner.class)
public class ObjectDetailsPanelTest {

    private JFrame frame;
    private FrameFixture frameFixture;
    private ObjectDetailsPanel view;
    private ActionListener<ObjectAction> listener;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new ObjectDetailsPanel();
                frame = new JFrame();
                frame.add(view.getUiComponent());

            }
        });
        frameFixture = new FrameFixture(frame);

        listener = mock(ActionListener.class);
        view.addObjectActionListener(listener);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @GUITest
    @Test
    public void verifySearchWorks() {
        final String SEARCH_TEXT = "test";

        frameFixture.show();

        JTextComponentFixture searchBox = frameFixture.textBox(SearchField.VIEW_NAME);
        searchBox.enterText(SEARCH_TEXT);

        verify(listener, times(SEARCH_TEXT.length())).actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.SEARCH));

        assertEquals(SEARCH_TEXT, view.getSearchText());
    }

    @GUITest
    @Test
    public void verifyMatchingObjectsWork() {
        frameFixture.show();

        HeapObjectUI obj1 = new HeapObjectUI("obj1", "obj1");
        HeapObjectUI obj2 = new HeapObjectUI("obj2", "obj2");

        view.setMatchingObjects(Arrays.asList(obj1, obj2));

        JTreeFixture tree = frameFixture.tree(ObjectDetailsPanel.TREE_NAME);

        TreePath[] paths = getRows(tree.component());

        assertEquals(obj1, getUserObject(paths[0]));
        assertEquals(obj2, getUserObject(paths[1]));

        tree.selectPath("obj1");

        assertEquals(obj1, view.getSelectedMatchingObject());
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

    @GUITest
    @Test
    public void verifySettingObjectDetails() {
        final String OBJECT_ID = "id";
        final boolean HEAP_ALLOCATED = true;
        final int OBJECT_SIZE = 10;
        final String OBJECT_CLASS = "foo.bar.Baz";

        frameFixture.show();

        JavaClass klass = mock(JavaClass.class);
        when(klass.getName()).thenReturn(OBJECT_CLASS);

        JavaHeapObject heapObj = mock(JavaHeapObject.class);
        when(heapObj.getIdString()).thenReturn(OBJECT_ID);
        when(heapObj.isHeapAllocated()).thenReturn(HEAP_ALLOCATED);
        when(heapObj.getSize()).thenReturn(OBJECT_SIZE);
        when(heapObj.getClazz()).thenReturn(klass);

        view.setObjectDetails(heapObj);

        JTextComponentFixture textBox = frameFixture.textBox(ObjectDetailsPanel.DETAILS_NAME);
        String expected = "Object ID:" + OBJECT_ID  + "\nType:" + OBJECT_CLASS + "\nSize:" + OBJECT_SIZE + " bytes\nHeap allocated:" + HEAP_ALLOCATED + "\n";
        assertEquals(expected, textBox.text());
    }

}

