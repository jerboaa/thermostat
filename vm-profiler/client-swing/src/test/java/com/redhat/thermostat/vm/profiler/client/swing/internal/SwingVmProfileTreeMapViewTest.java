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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.components.experimental.TreeMap;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapComponent;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapNode;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;
import com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileTreeMapView.TimeToolTipRenderer;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingVmProfileTreeMapViewTest {

    private static final String ROOT_LABEL = "root";
    private static final String CHILD_LABEL = "child";
    private static final double ROOT_WEIGHT = 1024.0;
    private static final double CHILD_WEIGHT = 3.14;

    private SwingVmProfileTreeMapView view;
    private TimeToolTipRenderer renderer;
    private TreeMapNode root;

    private FrameFixture frame;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new SwingVmProfileTreeMapView();
            }
        });
        frame = Containers.frameFixtureFor((Container) view.getUiComponent());

        root = new TreeMapNode(ROOT_LABEL, ROOT_WEIGHT);
        root.addChild(new TreeMapNode(CHILD_LABEL, CHILD_WEIGHT));
        renderer = new TimeToolTipRenderer();
    }

    @After
    public void tearDown() {
        frame.cleanUp();
        frame = null;
        view = null;
    }

    @GUITest
    @Test
    public void testDisplayWithNoResults() {
        frame.show();

        ProfilingResult result = new ProfilingResult(new ArrayList<MethodInfo>());
        view.display(result);

        JPanel panel = frame.panel(SwingVmProfileTreeMapView.PANEL_NAME).component();
        Component[] components = panel.getComponents();
        assertEquals(1, components.length);
        JLabel label = (JLabel) components[0];
        assertTrue(label.getText().contentEquals("No profiling data available"));
    }

    @GUITest
    @Test
    public void testDisplayWithResults() {
        frame.show();

        List<MethodInfo> methodInfos = new ArrayList<MethodInfo>();
        MethodDeclaration decl = new MethodDeclaration("foo.bar.Car", Arrays.asList("int"), "int");
        methodInfos.add(new MethodInfo(decl, 10, 100));
        ProfilingResult result = new ProfilingResult(methodInfos);
        view.display(result);

        JPanel panel = frame.panel(SwingVmProfileTreeMapView.PANEL_NAME).component();
        List<Component> components = new ArrayList<>(Arrays.asList(panel.getComponents()));
        assertEquals(2, components.size());

        for (Component component : new ArrayList<>(components)) {
            if (!SwingVmProfileTreeMapView.TREEMAPCOMP_NAME.equals(component.getName())) {
                components.remove(component);
            }
        }

        assertEquals(1, components.size());
        TreeMapComponent treeMapComponent = (TreeMapComponent) components.get(0);
        assertEquals(".foo.bar.Car", treeMapComponent.getTreeMapRoot().getLabel());
    }

    @Test
    public void testTimeToolTipRenderer() {
        String result = renderer.render(root);
        assertEquals("root - 1024ms", result);

        TreeMapNode child = TreeMap.searchNode(root, CHILD_LABEL);
        assertNotNull(child);
        result = renderer.render(child);
        assertEquals("child - 3ms", result);
    }
}
