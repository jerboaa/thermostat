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

package com.redhat.thermostat.client.swing.components.experimental;

import org.jfree.chart.renderer.category.GradientBarPainter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TreeMapComponentTest {

    private TreeMapComponent treeMap;
    private static TreeMapNode tree;
    private static TreeMapNode node1;
    private static TreeMapNode node2;

    @BeforeClass
    public static void setUpOnce() {
        tree = new TreeMapNode(1);
        node1 = new TreeMapNode(1);
        node2 = new TreeMapNode(1);
        tree.addChild(node1);
        node1.addChild(node2);
    }

    @Before
    public void setup() {
        treeMap = new TreeMapComponent();
    }

    @Test
    public final void testTreeMapComponent() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {

                try {
                    treeMap = new TreeMapComponent();
                    // pass
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }

                try {
                    treeMap = new TreeMapComponent(tree);
                    // pass
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }

                try {
                    treeMap = new TreeMapComponent(null);
                    // pass
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }
            }
        });
    }

    @Test
    public final void testGetRoot() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testSetModel() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.setModel(tree);
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }

                boolean caught = false;
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.setModel(null);
                } catch (NullPointerException e) {
                    caught = true;
                }
                assertTrue(caught);
            }
        });
    }

    @Test
    public final void testProcessAndDrawTreeMap() throws InvocationTargetException,
            InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.processAndDrawTreeMap(node1);
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }

                boolean caught = false;
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.processAndDrawTreeMap(null);
                } catch (NullPointerException e) {
                    caught = true;
                }
                assertTrue(caught);
            }
        });
    }

    @Test
    public final void testIsZoomInEnabled() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.setModel(tree);
                    treeMap.isZoomInEnabled(node1);
                } catch (NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                }

                boolean caught = false;
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.isZoomInEnabled(node1);
                } catch (NullPointerException e) {
                    caught = true;
                }
                assertTrue(caught);

                TreeMapComponent treeMap = new TreeMapComponent();
                treeMap.setModel(tree);

                assertFalse("Should not be able to zoom in on null", treeMap.isZoomInEnabled(null));
                assertFalse("Should not be able to zoom in on root", treeMap.isZoomInEnabled(tree));
                assertTrue("Should be able to zoom in on node 1", treeMap.isZoomInEnabled(node1));
                assertFalse("Should not be able to zoom in on node 2", treeMap.isZoomInEnabled(node2));
            }
        });
    }

    @Test
    public final void testZoomIn() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);

                treeMap.zoomIn(node1);
                assertEquals(node1, treeMap.getTreeMapRoot());

                treeMap.zoomIn(node2);
                assertEquals(node1, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testZoomOut() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);

                treeMap.zoomOut();
                assertEquals(tree, treeMap.getTreeMapRoot());

                treeMap.zoomIn(node1); //if zoom out root is tree
                treeMap.zoomIn(node2); //no-op, cannot zoom on leaf

                assertEquals(node1, treeMap.getTreeMapRoot());

                treeMap.zoomOut();
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testZoomFull() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);

                treeMap.zoomIn(node2);
                treeMap.zoomFull();
                assertEquals(tree, treeMap.getTreeMapRoot());

            }
        });
    }

    @Test
    public final void testGetZoomCallsStack() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);

                // the root is always in the stack
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(tree);
                // zooming on the same element nothing happen
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(node1);
                treeMap.zoomIn(node2);
                treeMap.zoomFull();
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testClearZoomCallsStack() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap.setModel(tree);

                treeMap.clearZoomCallsStack();
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(node1);
                treeMap.zoomIn(node2);
                treeMap.clearZoomCallsStack();
                assertEquals(1, treeMap.getZoomCallsStack().size());
            }
        });
    }

    @Test
    public final void testObserver() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            boolean zoomedIn = false;
            boolean zoomedOut = false;
            boolean zoomedFull = false;

            TreeMapObserver observer = new TreeMapObserver() {
                @Override
                public void notifyZoomOut() {
                    zoomedOut = true;
                }

                @Override
                public void notifyZoomIn(TreeMapNode node) {
                    zoomedIn = true;
                }

                @Override
                public void notifyZoomFull() {
                    zoomedFull = true;
                }

                @Override
                public void notifySelection(TreeMapNode node) {
                }
            };

            @Override
            public void run() {
                TreeMapNode child = new TreeMapNode(1);
                tree.addChild(child);
                TreeMapNode grandchild = new TreeMapNode(1);
                child.addChild(grandchild);

                treeMap.setModel(tree);
                treeMap.register(observer);

                treeMap.zoomIn(child);
                assertTrue("Should have zoomed in on child", zoomedIn);
                zoomedIn = false;

                treeMap.zoomIn(grandchild);
                assertFalse("Should not have zoomed in on grandchild", zoomedIn);

                treeMap.zoomOut();
                assertTrue("Should have zoomed out", zoomedOut);

                treeMap.zoomIn(child);
                treeMap.zoomFull();
                assertTrue("Should have zoomed full", zoomedFull);
            }
        });
    }

    @Test
    public final void testSetNode() {
        try {
            TreeMapComponent.Comp comp = treeMap.new Comp();
            comp.setNode(node1);
        } catch (NullPointerException e) {
            Assert.fail("Didn't expect exception.");
        }
    }


    public void performKeyboardShortcutTest(final KeyStroke keyStroke, final Runnable pre, final KeyShortcutTestResultHandler handler)
            throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            boolean zoomedIn = false;
            boolean zoomedOut = false;
            boolean zoomedFull = false;

            TreeMapObserver observer = new TreeMapObserver() {
                @Override
                public void notifyZoomOut() {
                    zoomedOut = true;
                }

                @Override
                public void notifyZoomIn(TreeMapNode node) {
                    zoomedIn = true;
                }

                @Override
                public void notifyZoomFull() {
                    zoomedFull = true;
                }

                @Override
                public void notifySelection(TreeMapNode node) {
                }
            };

            @Override
            public void run() {
                TreeMapNode child = new TreeMapNode(1);
                tree.addChild(child);
                TreeMapNode grandchild = new TreeMapNode(1);
                child.addChild(grandchild);
                treeMap.setModel(tree);
                treeMap.zoomIn(child);
                treeMap.zoomIn(grandchild);
                pre.run();
                treeMap.register(observer);
                ActionListener action = treeMap.getActionForKeyStroke(keyStroke);
                assertThat(action, is(not(equalTo(null))));
                action.actionPerformed(null);
                handler.handle(new KeyShortcutTestResults(zoomedIn, zoomedOut, zoomedFull));
            }
        });
    }

    public void performKeyboardShortcutTest(final KeyStroke keyStroke, final KeyShortcutTestResultHandler handler) throws InvocationTargetException, InterruptedException {
        Runnable emptyPre = new Runnable() {
            @Override
            public void run() {}
        };
        performKeyboardShortcutTest(keyStroke, emptyPre, handler);
    }

    @Test
    public void testKeyShortcutBackspace() throws InvocationTargetException, InterruptedException {
        final int NO_MODIFIERS = 0;
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, NO_MODIFIERS);
        performKeyboardShortcutTest(ks, new KeyShortcutTestResultHandler() {
            @Override
            public void handle(KeyShortcutTestResults results) {
                assertThat(results.zoomedIn, is(false));
                assertThat(results.zoomedOut, is(true));
                assertThat(results.zoomedFull, is(false));
                assertThat(treeMap.getClickedComponent(), is(equalTo(null)));
            }
        });
    }

    @Test
    public void testKeyShortcutEscape() throws InvocationTargetException, InterruptedException {
        final int NO_MODIFIERS = 0;
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, NO_MODIFIERS);
        performKeyboardShortcutTest(ks, new KeyShortcutTestResultHandler() {
            @Override
            public void handle(KeyShortcutTestResults results) {
                assertThat(results.zoomedIn, is(false));
                assertThat(results.zoomedOut, is(true));
                assertThat(results.zoomedFull, is(false));
                assertThat(treeMap.getClickedComponent(), is(equalTo(null)));
            }
        });
    }

    @Test
    public void testKeyShortcutHome() throws InvocationTargetException, InterruptedException {
        final int NO_MODIFIERS = 0;
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, NO_MODIFIERS);
        performKeyboardShortcutTest(ks, new KeyShortcutTestResultHandler() {
            @Override
            public void handle(KeyShortcutTestResults results) {
                assertThat(results.zoomedIn, is(false));
                assertThat(results.zoomedOut, is(false));
                assertThat(results.zoomedFull, is(true));
                assertThat(treeMap.getClickedComponent(), is(equalTo(null)));
            }
        });
    }

    //@Test
    // FIXME: this test depends on lastClicked being set at some point prior to the test running, but this requires
    // clicking on the painted TreeMap
    public void testKeyShortcutEnter() throws InvocationTargetException, InterruptedException {
        final int NO_MODIFIERS = 0;
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, NO_MODIFIERS);
        performKeyboardShortcutTest(ks,
            new Runnable() {
                @Override
                public void run() {
                    treeMap.zoomFull();
                }
            },
            new KeyShortcutTestResultHandler() {
                @Override
                public void handle(KeyShortcutTestResults results) {
                    assertThat(results.zoomedIn, is(false));
                    assertThat(results.zoomedOut, is(false));
                    assertThat(results.zoomedFull, is(false));
                    assertThat(treeMap.getClickedComponent(), is(equalTo(null)));
                }
            }
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame mainWindow = new JFrame();
                mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                final TreeMapNode modelA = new TreeMapNode("A", 1.0);
                modelA.addChild(new TreeMapNode("AA", 2.0));
                modelA.addChild(new TreeMapNode("AB", 3.0));

                final TreeMapNode modelB = new TreeMapNode("B", 5.0);
                modelB.addChild(new TreeMapNode("BA", 10.0));
                modelB.addChild(new TreeMapNode("BB", 10.0));

                // FIXME this hack should not be needed
                UIManager.put("thermostat-default-font", Font.decode(Font.MONOSPACED));

                final TreeMapComponent treeMap = new TreeMapComponent();
                treeMap.setModel(modelA);

                JPanel container = new JPanel(new BorderLayout());

                JPanel buttonPanel = new JPanel();
                JButton changeModelButton = new JButton("Change model");
                changeModelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        TreeMapNode newModel = treeMap.getTreeMapRoot() == modelA ? modelB : modelA;
                        treeMap.setModel(newModel);
                    }
                });
                buttonPanel.add(changeModelButton);

                JButton addNewNodeButton = new JButton("Add new node");
                addNewNodeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        TreeMapNode currentModel = treeMap.getTreeMapRoot();
                        currentModel.addChild(new TreeMapNode("new", 10.0));

                        treeMap.setModel(currentModel);
                    }
                });
                buttonPanel.add(addNewNodeButton);

                container.add(buttonPanel, BorderLayout.PAGE_START);
                container.add(treeMap, BorderLayout.CENTER);

                mainWindow.add(container, BorderLayout.CENTER);

                mainWindow.setSize(400, 200);
                mainWindow.setVisible(true);
            }
        });
    }

    interface KeyShortcutTestResultHandler {
        void handle(KeyShortcutTestResults results);
    }

    class KeyShortcutTestResults {
        boolean zoomedIn = false;
        boolean zoomedOut = false;
        boolean zoomedFull = false;

        public KeyShortcutTestResults(boolean zoomedIn, boolean zoomedOut, boolean zoomedFull) {
            this.zoomedIn = zoomedIn;
            this.zoomedOut = zoomedOut;
            this.zoomedFull = zoomedFull;
        }
    }

}
