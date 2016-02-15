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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.components.Icon;

@Category(CacioTest.class)
public class TreeMapBreadcrumbTest {

    private String SOME_ROOT = "root";
    private double SOME_WEIGHT = 0.0;
    private int SOME_WIDTH = 5;
    private int SCALING_INCREMENT = 10;
    private String LABEL = "label";

    private TreeMapNode root;
    private TreeMapBreadcrumb breadcrumb;

    @Before
    public void setup() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                root = new TreeMapNode(SOME_ROOT, SOME_WEIGHT);

                TreeMapComponent treeMapComponent = mock(TreeMapComponent.class);
                when(treeMapComponent.getTreeMapRoot()).thenReturn(root);

                breadcrumb = new TreeMapBreadcrumb(treeMapComponent, root);
            }
        });
    }

    @Test
    public void testNotifyZoom() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TreeMapNode grandchild = createTreeWithDepthTree(root);
                breadcrumb.notifyZoomIn(grandchild);
                assertEquals(3, breadcrumb.getItemCount());

                breadcrumb.notifyZoomOut();
                assertEquals(2, breadcrumb.getItemCount());

                breadcrumb.notifyZoomFull();
                assertEquals(1, breadcrumb.getItemCount());
            }
        });
    }

    private TreeMapNode createTreeWithDepthTree(TreeMapNode root) {
        TreeMapNode child = new TreeMapNode("childA", SOME_WEIGHT);
        root.addChild(child);

        TreeMapNode grandchild = new TreeMapNode("childAA", SOME_WEIGHT);
        child.addChild(grandchild);

        grandchild = new TreeMapNode("childAB", SOME_WEIGHT);
        child.addChild(grandchild);

        return grandchild;
    }

    @Test
    public void testAdaptIconWithValidLabel() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JLabel label = new JLabel();
                label.setHorizontalTextPosition(JLabel.CENTER);
                label.setText(LABEL);
                ImageIcon icon = new Icon(BreadcrumbIconResources.getIcon(BreadcrumbIconResources
                        .BREADCRUMB_BODY));

                breadcrumb.adaptIcon(label, icon);
                assertNotNull(label.getIcon());
                Rectangle fontArea = label.getFont().getStringBounds(label.getText(),
                        new FontRenderContext(label.getFont().getTransform(), false, false)).getBounds();
                assertEquals(fontArea.getBounds().width + 10, label.getIcon().getIconWidth());
            }
        });
    }

    @Test
    public void testAdaptIconWithFontlessLabel() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JLabel label = new JLabel();
                label.setFont(null);
                label.setBounds(0, 0, SOME_WIDTH, 0);
                ImageIcon icon = new Icon(BreadcrumbIconResources.getIcon(BreadcrumbIconResources
                        .BREADCRUMB_BODY));

                breadcrumb.adaptIcon(label, icon);
                assertNotNull(label.getIcon());
                assertEquals(SOME_WIDTH + SCALING_INCREMENT, label.getIcon().getIconWidth());
            }
        });
    }
}
