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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TreeMapComponentTest {

    private static final double BASE = 2.0;
    private static final double DELTA = 0.01;
    private static final double SOME_WEIGHT = 1.0;
    private static final int DEFAULT_DIMENSION = 1024;
    private static final int SMALLER_DIMENSION = 512;

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
    public final void testGenerateTreeMapFailsWithNullRoot()
            throws InvocationTargetException, InterruptedException {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                boolean caught = false;
                try {
                    treeMap = new TreeMapComponent();
                    treeMap.generateTreeMap(null);
                } catch (NullPointerException e) {
                    caught = true;
                }
                assertTrue(caught);
            }
        });
    }

    @Test
    public final void testGenerateTreeMapRepresentsTreeModelAccurately()
            throws InvocationTargetException, InterruptedException{

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                treeMap = new TreeMapComponent();
                TreeMapNode root = generateSimpleTree();
                treeMap.setSize(DEFAULT_DIMENSION, DEFAULT_DIMENSION);

                // generateTreeMap is called when the model is set
                treeMap.setModel(root);

                TreeMapComponent.Tile rootTile = Objects.requireNonNull(treeMap.getRootTile());

                // verify that the TreeMap represents the tree model accurately
                TreeMapNode tileRoot = Objects.requireNonNull(rootTile.getNode());
                assertEquals(root, tileRoot);
                verifyTileHierarchy(root, rootTile);

                // check that the generated tiles are reasonably sized
                checkTileAreas(rootTile);
            }
        });
    }

    private void verifyTileHierarchy(TreeMapNode subRoot, TreeMapComponent.Tile tile) {
        int childCount = 0;
        for (Component component : Arrays.asList(tile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                childCount++;
            }
        }
        assertEquals(subRoot.getChildren().size(), childCount);

        for (TreeMapNode childNode : subRoot.getChildren()) {
            boolean found = false;
            for (Component component : Arrays.asList(tile.getComponents())) {
                if (component instanceof TreeMapComponent.Tile) {
                    TreeMapNode compNode = ((TreeMapComponent.Tile) component).getNode();
                    if (compNode == childNode) {
                        found = true;
                        verifyTileHierarchy(childNode, (TreeMapComponent.Tile) component);
                    }
                }
            }
            assertTrue(found);
        }
    }

    private void checkTileAreas(TreeMapComponent.Tile tile) {
        double areaSum = 0;
        double weightSum = 0;
        boolean foundTile = false;
        for (Component component : Arrays.asList(tile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                foundTile = true;
                TreeMapComponent.Tile childTile = ((TreeMapComponent.Tile) component);
                areaSum += childTile.getWidth() * childTile.getHeight();
                TreeMapNode childNode = childTile.getNode();
                weightSum += childNode.getRealWeight();
            }
        }

        if (!foundTile) {
            return;
        }

        for (Component component : Arrays.asList(tile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                TreeMapComponent.Tile childTile = ((TreeMapComponent.Tile) component);
                double childArea = childTile.getWidth() * childTile.getHeight();
                TreeMapNode childNode = childTile.getNode();
                double childWeight = childNode.getRealWeight();

                assertEquals(childWeight / weightSum, childArea / areaSum, DELTA);

                checkTileAreas(childTile);
            }
        }


        Rectangle2D.Double parentRegion = new Rectangle2D.Double(0, 0, tile.getWidth(), tile.getHeight());
        Rectangle2D.Double availableRegion = TreeMapComponent.getAvailableRegion(parentRegion);
        double availableArea = availableRegion.getWidth() * availableRegion.getHeight();

        // this fraction must be close to 1 but may not be exactly
        assertEquals(1, availableArea / areaSum, DELTA);
    }

    @Test
    public final void testTreeMapTileColoring()
            throws InvocationTargetException, InterruptedException {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                treeMap = new TreeMapComponent();
                List<TreeMapNode> path = generateDeepTree(TreeMapComponent.colors.length + 1);
                TreeMapNode root = path.get(0);
                treeMap.setSize(DEFAULT_DIMENSION, DEFAULT_DIMENSION);

                // generateTreeMap is called when the model is set
                treeMap.setModel(root);

                TreeMapComponent.Tile rootTile = Objects.requireNonNull(treeMap.getRootTile());
                Color rootColor = Objects.requireNonNull(rootTile.getColor());
                assertEquals(TreeMapComponent.START_COLOR, rootColor);

                checkTileColorSetup(rootTile, rootColor, TreeMapComponent.colors);
                checkTileColorConsistency(path, rootTile);
            }
        });
    }

    private void checkTileColorSetup(TreeMapComponent.Tile tile, Color parentColor, Color[] colors) {
        Color expectedColor = TreeMapComponent.getNextColor(parentColor, colors);
        for (Component component : Arrays.asList(tile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                TreeMapComponent.Tile childTile = ((TreeMapComponent.Tile) component);
                assertEquals(expectedColor, childTile.getColor());
                checkTileColorSetup(childTile, expectedColor, colors);
            }
        }
    }

    private void checkTileColorConsistency(List<TreeMapNode> path, TreeMapComponent.Tile rootTile) {
        if (path.isEmpty()) {
            throw new AssertionError("Path is empty.");
        }

        // don't need the root
        path.remove(0);
        assertEquals(TreeMapComponent.colors[0], rootTile.getColor());
        for (Component component : Arrays.asList(rootTile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                TreeMapComponent.Tile childTile = (TreeMapComponent.Tile) component;

                if (childTile.getNode() == path.get(0)) {
                    // found the deep branch
                    checkTileColorConsistencyHelper(path, childTile);
                }
            }
        }
    }

    private void checkTileColorConsistencyHelper(List<TreeMapNode> path, TreeMapComponent.Tile tile) {
        if (path.isEmpty()) {
            return;
        }

        treeMap.generateTreeMap(path.get(0));
        assertNotSame(tile, treeMap.getRootTile());
        assertEquals(tile.getColor(), treeMap.getRootTile().getColor());
        for (Component component : Arrays.asList(tile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                TreeMapComponent.Tile childTile = (TreeMapComponent.Tile) component;
                path.remove(0);
                checkTileColorConsistencyHelper(path, childTile);
            }
        }
    }

    private List<TreeMapNode> generateDeepTree(int depth) {
        /*
         * This tree is used as a test case.  The long branch down the right has a variable depth
         * to accommodate potential changes in the number of colors available.  Weights in
         * parentheses.
         *
         *                         ______________(16)______________
         *                        /                |               \
         *               _______(12)_______       (3)              (1)
         *              /                  \                         \
         *       _____(6)______            (6)                       (1)
         *      /      |       \                                       .
         *    (2)     (1)      (3)                                      .
         *                                                               .
         */
        TreeMapNode root = generateSimpleTree();
        root.setRealWeight(root.getRealWeight() + 1);

        List<TreeMapNode> path = new ArrayList<>();
        path.add(root);

        TreeMapNode parent = root;
        for (int i = 0; i < (depth - 1); i++) {
            TreeMapNode child = new TreeMapNode(1);
            parent.addChild(child);
            path.add(child);
            parent = child;
        }

        return path;
    }

    private TreeMapNode generateSimpleTree() {
        /*
         * This tree is used as a test case.  Weights in parentheses.
         *
         *                         __________(15)_________
         *                        /                       \
         *               _______(12)_______               (3)
         *              /                  \
         *       _____(6)______            (6)
         *      /      |       \
         *    (2)     (1)      (3)
         *
         */

        TreeMapNode root = new TreeMapNode(15);
        root.addChild(new TreeMapNode(3));

        TreeMapNode child = new TreeMapNode(12);
        root.addChild(child);
        child.addChild(new TreeMapNode(6));

        TreeMapNode grandchild = new TreeMapNode(6);
        child.addChild(grandchild);

        grandchild.addChild(new TreeMapNode(2));
        grandchild.addChild(new TreeMapNode(1));
        grandchild.addChild(new TreeMapNode(3));

        return root;
    }

    @Test
    public final void testTileAreasWhenResizing() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final int numSiblings = 5;

                TreeMapNode root = new TreeMapNode(SOME_WEIGHT);
                generateSiblingTree(root, numSiblings);

                treeMap.setSize(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
                // generateTreeMap is called when the model is set
                treeMap.setModel(root);
                checkSiblingAreaRatios(treeMap.getRootTile(), numSiblings);

                // now resize smaller
                treeMap.setSize(SMALLER_DIMENSION, SMALLER_DIMENSION);
                treeMap.generateTreeMap(root);
                checkSiblingAreaRatios(treeMap.getRootTile(), numSiblings);

                // now resize back to original size
                treeMap.setSize(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
                treeMap.generateTreeMap(root);
                // check that resizing did not result in a significant loss of precision
                checkSiblingAreaRatios(treeMap.getRootTile(), numSiblings);
            }
        });
    }

    private void generateSiblingTree(TreeMapNode root, int numSiblings) {
        assertTrue(numSiblings > 1);
        root.addChild(new TreeMapNode(1.0)); //this is not a random weight
        for (int i = 0; i < (numSiblings - 1); i++) {
            root.addChild(new TreeMapNode(Math.pow(BASE, i)));
        }
    }

    private void checkSiblingAreaRatios(TreeMapComponent.Tile rootTile, int numSiblings) {
        List<TreeMapComponent.Tile> children = new ArrayList<>();
        for (Component component : Arrays.asList(rootTile.getComponents())) {
            if (component instanceof TreeMapComponent.Tile) {
                children.add((TreeMapComponent.Tile) component);
            }
        }

        assertEquals(numSiblings, children.size());
        sortDescending(children);

        Rectangle refBounds = children.get(0).getBounds();
        double refArea = refBounds.getHeight() * refBounds.getWidth();

        for (int i = 1; i < numSiblings - 1; i++) {
            Rectangle currBounds = children.get(i).getBounds();
            double currArea = currBounds.getHeight() * currBounds.getWidth();
            // check that the ratio between areas is approximately 2 (which is the BASE)
            assertEquals(BASE, refArea / currArea, DELTA);
            refArea = currArea;
        }
    }

    private void sortDescending(List<TreeMapComponent.Tile> tiles) {
        Comparator<TreeMapComponent.Tile> c = new Comparator<TreeMapComponent.Tile>() {
            @Override
            public int compare(TreeMapComponent.Tile o1, TreeMapComponent.Tile o2) {
                return -(Double.compare(o1.getNode().getRealWeight(), o2.getNode().getRealWeight()));
            }
        };
        Collections.sort(tiles, c);
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
            TreeMapComponent.Tile tile = treeMap.new Tile();
            tile.setNode(node1);
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
                assertThat(treeMap.getClickedTile(), is(equalTo(null)));
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
                assertThat(treeMap.getClickedTile(), is(equalTo(null)));
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
                assertThat(treeMap.getClickedTile(), is(equalTo(null)));
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
                    assertThat(treeMap.getClickedTile(), is(equalTo(null)));
                }
            }
        );
    }

    private static void findAllNodes(List<TreeMapNode> allNodes, final TreeMapNode node) {
        for(TreeMapNode child : node.getChildren()) {
            findAllNodes(allNodes, child);
        }

        allNodes.add(node);
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

                final TreeMapNode modelC = new TreeMapNode("C", 1.0);
                modelC.addChild(new TreeMapNode("CA", 2.0));
                modelC.addChild(new TreeMapNode("CB", 3.0));

                final TreeMapNode modelB = new TreeMapNode("B", 5.0);
                final Random generator = new Random();
                for (int i = 0; i < 100; i++) {
                    List<TreeMapNode> allNodes = new ArrayList<>();
                    findAllNodes(allNodes, modelB);
                    int parentInt = generator.nextInt(allNodes.size());
                    TreeMapNode parent = allNodes.get(parentInt);
                    double weight = Math.pow(10, generator.nextInt(4));
                    parent.addChild(new TreeMapNode("n" + i,  weight));
                }

                // FIXME this hack should not be needed
                UIManager.put("thermostat-default-font", Font.decode(Font.MONOSPACED));

                JPanel container = new JPanel(new BorderLayout());
                final JTabbedPane tabbedPane = new JTabbedPane();

                final TreeMapComponent treeMapA = new TreeMapComponent();
                treeMapA.setToolTipRenderer(new WeightRenderer());
                treeMapA.setModel(modelA);

                final TreeMapComponent treeMapC = new TreeMapComponent();
                treeMapC.setToolTipRenderer(new WeightRenderer());
                treeMapC.setModel(modelC);

                JPanel buttonPanel = new JPanel();
                JButton changeModelButton = new JButton("Change model");
                changeModelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (tabbedPane.getSelectedIndex() == 0) {
                            TreeMapNode newModel = treeMapA.getTreeMapRoot() == modelA ? modelB : modelA;
                            treeMapA.setModel(newModel);
                        } else {
                            TreeMapNode newModel = treeMapC.getTreeMapRoot() == modelC ? modelB : modelC;
                            treeMapC.setModel(newModel);
                        }
                    }
                });
                buttonPanel.add(changeModelButton);

                JButton addChildToRootButton = new JButton("Add child to root");
                addChildToRootButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (tabbedPane.getSelectedIndex() == 0) {
                            TreeMapNode currentModel = treeMapA.getTreeMapRoot();
                            currentModel.addChild(new TreeMapNode("new", 5.0));
                            treeMapA.setModel(currentModel);
                        } else {
                            TreeMapNode currentModel = treeMapC.getTreeMapRoot();
                            currentModel.addChild(new TreeMapNode("new", 5.0));
                            treeMapC.setModel(currentModel);
                        }
                    }
                });
                buttonPanel.add(addChildToRootButton);

                JButton addRandomNodeButton = new JButton("Add random node");
                addRandomNodeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (tabbedPane.getSelectedIndex() == 0) {
                            TreeMapNode currentModel = treeMapA.getTreeMapRoot();
                            List<TreeMapNode> allNodes = new ArrayList<>();
                            findAllNodes(allNodes, currentModel);
                            TreeMapNode parent = allNodes.get(generator.nextInt(allNodes.size()));
                            double weight = Math.pow(10, generator.nextInt(4));
                            parent.addChild(new TreeMapNode("rand", weight));

                            treeMapA.setModel(currentModel);
                        } else {
                            TreeMapNode currentModel = treeMapC.getTreeMapRoot();
                            List<TreeMapNode> allNodes = new ArrayList<>();
                            findAllNodes(allNodes, currentModel);
                            TreeMapNode parent = allNodes.get(generator.nextInt(allNodes.size()));
                            double weight = Math.pow(10, generator.nextInt(4));
                            parent.addChild(new TreeMapNode("rand", weight));

                            treeMapC.setModel(currentModel);
                        }
                    }
                });
                buttonPanel.add(addRandomNodeButton);

                container.add(buttonPanel, BorderLayout.PAGE_START);
                tabbedPane.addTab("TMA", treeMapA);
                tabbedPane.addTab("TMC", treeMapC);

                container.add(tabbedPane, BorderLayout.CENTER);
                mainWindow.add(container, BorderLayout.CENTER);

                mainWindow.setSize(500, 200);
                mainWindow.setVisible(true);
            }
        });
    }

    public static class WeightRenderer implements TreeMapComponent.ToolTipRenderer {
        @Override
        public String render(TreeMapNode node) {
            return node.getLabel() + " RW:" + node.getRealWeight();
        }
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
