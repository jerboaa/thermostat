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

import com.redhat.thermostat.client.swing.ThermostatSwingCursors;
import com.redhat.thermostat.client.swing.components.ShadowLabel;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

/**
 * This class directs the representation of a tree model as a graphical TreeMap. It extends
 * {@link JComponent} and as such can be used like a typical Swing object.
 *
 */
public class TreeMapComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    /**
     * The tile representing the root of the tree model.  Graphically, tiles representing the
     * root's children appear layered atop (but within) the root tile.
     */
    private Tile rootTile;

    /**
     * Label Object to clone for faster initialization.
     */
    private ShadowLabel cachedLabel;

    /**
     * The tree to render as TreeMap.
     */
    private TreeMapNode tree;

    /**
     * Horizontal and vertical padding for nested tiles.
     */
    private static final int X_PADDING = 15;
    private static final int Y_PADDING = 20;

    /**
     * Min size for rectangles' sides. rectangles having one or both sides less
     * than MIN_SIDE pixels will be not drawn.
     */
    private final int MIN_SIDE = 1;

    /**
     * Default value for a TreeMap component.
     */
    private static final String TITLE = "";

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /*
         * TreeMap border styles
         */
    public static final int BORDER_SIMPLE = 0;
    public static final int BORDER_FLAT = 1;
    public static final int BORDER_ETCHED_LOWERED = 2;
    public static final int BORDER_ETCHED_RAISED = 3;

    private int borderStyle = BORDER_SIMPLE;

    /**
     * Font and size for this component's label.
     */
    private int FONT_SIZE = 8;
    private Font FONT = (Font) UIManager.get("thermostat-default-font");


    /**
     * Variable in which store last resize dimension.
     */
    private Dimension lastDim;

    /**
     * Variable in which store last resize event call time.
     */
    private long lastCall = 0;

    /**
     * Wait time in millisec to resize the TreeMap.
     */
    private final int MIN_DRAGGING_TIME = 60;


    /**
     * Stack containing the zoom calls on the TreeMap.
     */
    private Stack<TreeMapNode> zoomStack;

    /**
     * The tile that was most recently clicked.
     */
    private Tile lastClicked;
    
    /**
     * List of objects observing this.
     */
    private List<TreeMapObserver> observers;

    private ToolTipRenderer tooltipRenderer = new SimpleRenderer();

    static final Color[] colors = {
            Color.decode("#FACED2"), // red
            Color.decode("#B9D6FF"), // blue
            Color.decode("#E5E5E5"), // grey
            Color.decode("#FFE7C7"), // orange
            Color.decode("#ABEBEE"), // aqua
            Color.decode("#E4D1FC"), // purple
            Color.decode("#FFFFFF"), // white
            Color.decode("#CDF9D4")  // green
    };
    static final Color[] borderColors = {
            Color.decode("#EBC2C6"), // red
            Color.decode("#A8C2E7"), // blue
            Color.decode("#D6D6D6"), // grey
            Color.decode("#E3BCA9"), // orange
            Color.decode("#94CDDE"), // aqua
            Color.decode("#D1C0E7"), // purple
            Color.decode("#EEEEEE"), // white
            Color.decode("#BEE7C5")  // green
    };

    public static final Color START_COLOR = colors[0];

    public TreeMapComponent() {
        this(null);
    }

    /**
     * Constructor that sets up a TreeMapComponent using the specified {@TreeMapNode} tree.
     */
    public TreeMapComponent(TreeMapNode tree) {
        super();
        this.tree = tree;
        lastDim = getSize();
        this.zoomStack = new Stack<>();
        this.observers = new ArrayList<>();

        if (tree != null) {
            this.zoomStack.push(this.tree);
            generateTreeMap(this.tree);
        }

        addResizeListener(this);
        addKeyBindings(this);
    }

    Tile getRootTile() {
        return rootTile;
    }

    /**
     * This method returns the root of the tree showed ad TreeMap.
     * @return the TreeMap's root node.
     */
    public TreeMapNode getTreeMapRoot() {
        return this.tree;
    }

    public void setModel(TreeMapNode tree) {
        this.tree = Objects.requireNonNull(tree);
        this.zoomStack.clear();
        this.zoomStack.push(this.tree);
        generateTreeMap(this.tree);
    }

    public void setToolTipRenderer(ToolTipRenderer renderer) {
        this.tooltipRenderer = renderer;
    }

    /**
     * This method generates a hierarchy of tiles that represent the tree model rooted at
     * {@param root}.
     *
     * Package-private for testing.
     */
    void generateTreeMap(TreeMapNode root) {
        tree = Objects.requireNonNull(root);

        if (getSize().width == 0 || getSize().height == 0) {
            return;
        }

        removeAll();

        Rectangle2D.Double region = new Rectangle2D.Double(0, 0, getSize().width, getSize().height);
        createRootTile(region);
        setBorderStyle(borderStyle);

        processSubtree(tree, region, rootTile);
        prepareGUI();
    }

    private void createRootTile(Rectangle2D.Double rectangle) {
        rootTile = new Tile();
        rootTile.setLayout(null);
        rootTile.setBounds(rectangle.getBounds());
        rootTile.setNode(tree);
        rootTile.setColor(colors[tree.getDepth() % colors.length]);
        rootTile.setBorderColor(borderColors[tree.getDepth() % borderColors.length]);

        rootTile.setToolTipText(
                Objects.requireNonNull(this.tooltipRenderer).render(tree));
        cachedLabel = createLabel(TITLE + tree.getLabel());
        addLabelIfPossible(TITLE + tree.getLabel(), rootTile);
    }

    /**
     * This method, with the aid of {@link #processNode(TreeMapNode, Rectangle2D.Double, Color, Color, Tile)}
     * Tile)}, generates (if appropriate) the tile representing each node in the subtree rooted at
     * {@param root}.
     *
     * @param root root of the subtree to be processed.
     * @param rectangle the squarified rectangle corresponding to {@param root}.
     * @param rootTile the tile that represents {@param root}.
     */
    private void processSubtree(TreeMapNode root, Rectangle2D.Double rectangle, Tile rootTile) {

        // generate squarified rectangles corresponding to the children of the root node
        LinkedList<TreeMapNode> elements = new LinkedList<>();
        elements.addAll(Objects.requireNonNull(root.getChildren()));
        Map<TreeMapNode, Rectangle2D.Double> squarifiedMap =
                getSquarifiedRectangles(elements, rectangle);

        // any children will all have the same color
        Color nextColor = getNextColor(rootTile.getColor(), colors);
        Color nextBorderColor = getNextColor(rootTile.getBorderColor(), borderColors);

        for (int i = 0; i < elements.size(); i++) {
            TreeMapNode child = elements.get(i);

            if (rectangleHasDrawableSides(squarifiedMap.get(child))) {
                // attempt to add a tile for this node
                processNode(child, squarifiedMap.get(child), nextColor,
                            nextBorderColor, rootTile);
            }
        }
    }

    /**
     * This method attempts to create a tile to represent the supplied {@param node}.  If
     * successful, it calls the {@link #processSubtree(TreeMapNode, Rectangle2D.Double, Tile)}
     * method to continue generating the TreeMap for the subtree rooted at {@param node}.
     *
     * @param node to be represented with a tile, if appropriate.
     * @param rectangle the squarified rectangle corresponding to {@param node}.
     * @param color the color of the tile.
     * @param parentTile the parent to which a tile, if created, is added.
     */
    private void processNode(TreeMapNode node, Rectangle2D.Double rectangle,
                             Color color, Color borderColor, Tile parentTile)
    {
        Tile tile = addTileIfPossible(rectangle, node.getLabel(), parentTile);

        if (tile != null) {
            tile.setNode(node);
            tile.setColor(color);
            tile.setBorderColor(borderColor);
            tile.setToolTipText(
                    Objects.requireNonNull(this.tooltipRenderer).render(node));
            processSubtree(node, rectangle, tile);
        }
    }

    private Map<TreeMapNode, Rectangle2D.Double> getSquarifiedRectangles(
            LinkedList<TreeMapNode> elements, Rectangle2D.Double region) {

        TreeMapNode.sort(elements);
        SquarifiedTreeMap algorithm = new SquarifiedTreeMap(getAvailableRegion(region), elements);
        return algorithm.squarify();
    }

    /**
     * Package-private for testing.
     */
    static Color getNextColor(Color currentColor, Color[] colors) {
        if (currentColor != null) {
            for (int i = 0; i < colors.length; i++) {
                if (currentColor.equals(colors[i])) {
                    return colors[(i + 1) % colors.length];
                }
            }
        }
        return START_COLOR;
    }

    /**
     * Package-private for testing.
     */
    static Rectangle2D.Double getAvailableRegion(Rectangle2D.Double parentRegion) {
        Rectangle2D.Double subRegion = new Rectangle2D.Double();
        subRegion.setRect(parentRegion);

        subRegion.width = Math.max(0, (subRegion.width - 2 * X_PADDING));
        subRegion.height = Math.max(0, (subRegion.height - 1.5 * Y_PADDING));
        return subRegion;
    }

    private boolean rectangleHasDrawableSides(Rectangle2D.Double rectangle) {
        if (rectangle.getWidth() <= MIN_SIDE || rectangle.getHeight() <= MIN_SIDE) {
            return false;
        }
        return true;
    }

    /**
     * This method adds a tile to the specified parent if there is enough space, and attempts
     * to label it.
     *
     * @param rectangle the squarified rectangle used to set the bounds of a new tile.
     * @param label the String with which to label the created tile.
     * @param parentTile the parent tile to which the new tile will be added.
     * @return the new tile if it was created, else null.
     */
    private Tile addTileIfPossible(Rectangle2D.Double rectangle, String label, Container parentTile) {
        if (parentTile.getWidth() > rectangle.getWidth() + X_PADDING &&
                parentTile.getHeight() > rectangle.getHeight() + Y_PADDING) {

            Tile tile = new Tile(rootTile);
            tile.setBounds(rectangle.getBounds());
            addLabelIfPossible(TITLE + label, tile);

            Point loc = tile.getLocation();
            loc.x += X_PADDING;
            loc.y += Y_PADDING;
            tile.setLocation(loc);

            parentTile.add(tile);
            return tile;
        }
        return null;
    }

    /**
     * This method checks if the given container has enough space to instantiate
     * a Label in it. If yes, a Label is cloned from an existing one, in order
     * to improve performance. If not, it exits.
     *
     * @param s the label text.
     * @param cont the parent container which will contain the new label.
     * @return the cloned label.
     */
    private ShadowLabel addLabelIfPossible(String s, Container cont) {
        if (s == null || s.equals("")) {
            return null;
        }
        int componentW = cont.getSize().width;
        int componentH = cont.getSize().height;
        // get the rectangle associated to the area needed for the label's text
        Rectangle fontArea = FONT.getStringBounds(s,
                new FontRenderContext(FONT.getTransform(),
                        false, false)).getBounds();

        // if the container is greater than the label, add it to the container
        if (componentW > fontArea.width && componentH > fontArea.height) {
            ShadowLabel label = createLabel(cachedLabel);
            label.setBounds(5, 1, cont.getWidth(), fontArea.height);
            label.setText(s);
            cont.add(label);
            return label;
        }
        return null;
    }

    /**
     * This method prepares the layout for this component.
     */
    private void prepareGUI() {
        setLayout(new BorderLayout());
        setBounds(rootTile.getBounds());
        setBorder(null);
        add(rootTile, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Create and add to the {@link Container} given in input a
     * {@link java.awt.event.ComponentListener} listener.
     * @param container the container in to assign the listener.
     */
    private void addResizeListener(final Container container) {
        ComponentAdapter adapter = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // if enough time is passed from the last call, redraw the TreeMap
                if (canResize(MIN_DRAGGING_TIME)) {
                    Dimension newDim = container.getSize();

                    if (isChangedSize(newDim)) {
                        generateTreeMap(Objects.requireNonNull(tree));
                    }
                }
            }
        };
        container.addComponentListener(adapter);
    }

    private void addKeyBindings(final JComponent component) {
        final int NO_MODIFIERS = 0;
        final String ZOOM_OUT = "zoomOut";
        final String ZOOM_FULL = "zoomFull";
        final String ZOOM_IN = "zoomIn";
        InputMap inputMap = component.getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, NO_MODIFIERS), ZOOM_OUT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, NO_MODIFIERS), ZOOM_FULL);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, NO_MODIFIERS), ZOOM_OUT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, NO_MODIFIERS), ZOOM_IN);

        actionMap.put(ZOOM_OUT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                zoomOut();
                lastClicked = null;
            }
        });
        actionMap.put(ZOOM_FULL, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                zoomFull();
                lastClicked = null;
            }
        });
        actionMap.put(ZOOM_IN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (lastClicked != null) {
                    zoomIn(lastClicked.getNode());
                }
            }
        });
    }

    boolean isZoomInEnabled(TreeMapNode node) {
        return !(node == null
                || node.equals(Objects.requireNonNull(this.tree))
                || node.isLeaf());
    }

    public void zoomIn(TreeMapNode node) {
        if (isZoomInEnabled(node)) {
            fillZoomStack(node.getAncestors());
            generateTreeMap(node);
            notifyZoomInToObservers(zoomStack.peek());
        } 
    }

    private void fillZoomStack(LinkedList<TreeMapNode> ancestors) {
        zoomStack.clear();
        while (!ancestors.isEmpty()) {
            zoomStack.push(ancestors.removeLast());
        }
    }

    public void zoomOut() {
        // if the actual root element is not the tree's original root
        if (zoomStack.size() > 1) {
            zoomStack.pop();
            generateTreeMap(zoomStack.peek());
            notifyZoomOutToObservers();
        }
    }

    /**
     * Zoom out the view directly to the original root.
     */
    public void zoomFull() {
        if (zoomStack.size() > 1) {
            clearZoomCallsStack();
            generateTreeMap(zoomStack.peek());
            notifyZoomFullToObservers();
        }
    }
    
    /**
     * Add the object in input to the list of registered objects to this TreeMap.
     * @param observer the Notifiable object to register to this object.
     */
    public void register(TreeMapObserver observer) {
        this.observers.add(observer);
    }
    
    /**
     * Remove the object in input from the list of registered objects to this TreeMap.
     * @param observer the Notifiable object to unregister from this object.
     */
    public void unregister(TreeMapObserver observer) {
        this.observers.remove(observer);
    }

    /**
     * Notify observers that an object in the TreeMap has been selected.
     */
    private void notifySelectionToObservers(TreeMapNode node) {
        for (TreeMapObserver observer : observers) {
            observer.notifySelection(node);
        }
    }

    /**
     * Notify observers that TreeMap has been zoomed.
     */
    private void notifyZoomInToObservers(TreeMapNode node) {
        for (TreeMapObserver observer : observers) {
            observer.notifyZoomIn(node);
        }
    }
    
    /**
     * Notify observers that  TreeMap has been zoomed.
     */
    private void notifyZoomOutToObservers() {
        for (TreeMapObserver observer : observers) {
            observer.notifyZoomOut();
        }
    }
    
    /**
     * Notify observers that  TreeMap has been zoomed.
     */
    private void notifyZoomFullToObservers() {
        for (TreeMapObserver observer : observers) {
            observer.notifyZoomFull();
        }
    }

    /**
     * Returns the list of zoom operation calls.
     * @return the stack that holds the zoom calls.
     */
    public Stack<TreeMapNode> getZoomCallsStack() {
        return zoomStack;
    }

    /**
     * Clear the zoom calls of this object leaving the stack with just the root.
     */
    public void clearZoomCallsStack() {
        while (zoomStack.size() > 1) {
            zoomStack.pop();
        }
    }

    /**
     * check if last resize operation was called too closer to this
     * one. If so, ignore it: the container is being dragged. 
     * 
     * @return true if this method is invoked at distance of 
     * MIN_DRAGGING_TIME millisec, else false. 
     */
    private boolean canResize(int millisec) {
        long time = System.currentTimeMillis();
        if (time - lastCall >= millisec) {
            lastCall = time;
            return true;
        }
        return false;
    }


    /**
     * Check if the dimension given in input differs from the last one stored
     * by 2. 
     * @param newDim the new dimension to check.
     * @return true if the dimensions are different, else false.
     */
    private boolean isChangedSize(Dimension newDim) {
        int minResizeDim = 2;
        int deltaX = Math.abs(newDim.width - lastDim.width);
        int deltaY = Math.abs(newDim.height - lastDim.height);

        if (deltaX > minResizeDim || deltaY > minResizeDim) {
            lastDim = newDim;
            return true;
        }
        return false;
    }

    /**
     * Switch the component's border style to the one given in input.
     *
     * @param newBorderStyle the border style to use
     */
    public void setBorderStyle(int newBorderStyle) {
        Border border;
        switch (newBorderStyle) {
            case BORDER_SIMPLE : {
                border = new EmptyBorder(0, 0, 0, 0);
                break;
            }    
            case BORDER_FLAT : {
                border = new LineBorder(Color.black, 1);
                break;
            }
            case BORDER_ETCHED_LOWERED : {
                border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, Color.white, Color.darkGray);
                break;
            }
            case BORDER_ETCHED_RAISED : {
                border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.white, Color.darkGray);
                 break;
             }
            default : {
                throw new IllegalArgumentException("Unknown border style: " + newBorderStyle);
            }
        }
        this.borderStyle = newBorderStyle;
        rootTile.setBorder(border);
    }
    
    public Tile getClickedTile() {
        return lastClicked;
    }

    private ShadowLabel createLabel(ShadowLabel other) {
        ShadowLabel label = createLabel(other.getText());
        label.setFont(other.getFont());
        label.setBackground(other.getBackground());
        label.setBounds(other.getBounds());
        label.setBorder(other.getBorder());
        return label;
    }

    private ShadowLabel createLabel(String text) {
        ShadowLabel label = new ShadowLabel();
        label.setText(text);
        label.setFont(FONT);
        label.setBounds(0, 0, getPreferredSize().width, FONT_SIZE);
        return label;
    }

    /**
     * This class describes a graphical representation for a {@link TreeMapNode} and enables
     * zooming in the TreeMap.
     */
    class Tile extends JComponent {

        private static final long serialVersionUID = 1L;

        /**
         * The node represented by this component.
         */
        private TreeMapNode node;

        /**
         * The background color. It depends by the node's depth.
         */
        private Color color;

        /**
         * The border color. It depends by the node's depth, and only
         * painted if the border is "simple".
         */
        private Color borderColor;

        /**
         * Reference to this.
         */
        private Tile thisComponent;

        public Tile() {
            super();
            thisComponent = this;
            addClickListener(this);
            addMouseListener(this);
        }

        public Tile(Tile other) {
            this();
            this.setBounds(other.getBounds());
            this.setBorder(other.getBorder());
            this.setLayout(other.getLayout());
            this.setOpaque(true);
        }

        public void setNode(TreeMapNode node) {
            this.node = node;
        }
        
        public TreeMapNode getNode() {
            return this.node;
        }

        public Color getColor() {
            return this.color;
        }

        public void setColor(Color c) {
            this.color = c;
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g); 
            if (this.color != null) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (borderStyle == BORDER_SIMPLE) {
                    // paint a very subtle border using the current color
                    // as a base
                    g.setColor(borderColor);
                    g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            }
        }   

        /**
         * Add a mouse listener to this component. It allows to select it and
         * zoom it. 
         * @param component the component which will have the mouse listener.
         */
        private void addClickListener(final JComponent component) {
            MouseListener click = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    TreeMapComponent.this.requestFocusInWindow();
                    // one left click select the rectangle
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectTile();
                    }
                    // double left click to zoom in (on non-leaf nodes only)
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        zoomIn(getNode());
                    }
                    // one right click to zoom out
                    if (SwingUtilities.isRightMouseButton(e)) {
                        zoomOut();
                    }
                    // one middle click to reset zoom
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        zoomFull();
                    }
                }
            };
            component.addMouseListener(click);
        }

        /**
         * Add a mouse motion listener to this component. This allows for the mouse cursor to be
         * changed into a magnifying glass icon when the cursor enters a zoomable component, and
         * back to a default cursor when it exits a zoomable component.
         *
         * @param component the component which will have the mouse motion listener.
         */
        private void addMouseListener(final JComponent component) {
            MouseListener listener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (getNode().isLeaf()) {
                        setDefaultCursor();
                    } else {
                        setZoomableCursor();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!getNode().isLeaf()) {
                        setDefaultCursor();
                    } else {
                        setZoomableCursor();
                    }
                }

                private void setZoomableCursor() {
                    component.setCursor(ThermostatSwingCursors.getZoomIconCursor());
                }

                private void setDefaultCursor() {
                    component.setCursor(Cursor.getDefaultCursor());
                }
            };
            component.addMouseListener(listener);
        }

        /**
         * This method darkens the color of this component and restores the previously selected
         * component to its nominal shade.
         */
        private void selectTile() {
            if (lastClicked != null) {
                if (!lastClicked.getNode().isLeaf()) {
                    lastClicked.setColor(lastClicked.getColor().brighter());
                }
                lastClicked.repaint();
            } 
            lastClicked = thisComponent;
            if (!getNode().isLeaf()) {
                setColor(getColor().darker());
                setBorderColor(getColor().darker());
            }
            repaint();
            notifySelectionToObservers(node);
        }
    }

    public static interface ToolTipRenderer {
        public String render(TreeMapNode node);
    }

    public static class SimpleRenderer implements ToolTipRenderer {
        @Override
        public String render(TreeMapNode node) {
            return node.getLabel();
        }
    }
}





