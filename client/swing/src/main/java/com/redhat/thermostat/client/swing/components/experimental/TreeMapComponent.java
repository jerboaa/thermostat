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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

import com.redhat.thermostat.client.swing.ThermostatSwingCursors;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Size.Unit;

/**
 * This class allows to represent a hierarchical data structure as a TreeMap.
 * It extends {@link JComponent} so it can be used like usual Swing objects.
 *
 */
public class TreeMapComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    /**
     * TreeMap's graphic root.
     */
    Comp mainComp;

    /**
     * Label Object to clone for faster initialization.
     */
    private Label cachedLabel;

    /**
     * The tree to render as TreeMap.
     */
    private TreeMapNode tree;

    /**
     * Horizontal and vertical padding for nested component.
     */
    private final int X_PADDING = TreeProcessor.X_PADDING;
    private final int Y_PADDING = TreeProcessor.Y_PADDING;

    /**
     * Min size for rectangles' sides. rectangles having one or both sides less
     * than MIN_SIDE pixels will be not drawn.
     */
    private final int MIN_SIDE = 1;

    /**
     * Default value for a TreeMap component.
     */
    private static final String TITLE = "";

    /*
     * TreeMap border styles
     */
    public static final int BORDER_SIMPLE = 0;
    public static final int BORDER_FLAT = 1;
    public static final int BORDER_ETCHED_LOWERED = 2;
    public static final int BORDER_ETCHED_RAISED = 3;

    private int borderStyle = BORDER_ETCHED_LOWERED;

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
    private static long lastCall = 0;

    /**
     * Wait time in millisec to resize the TreeMap.
     */
    private final int MIN_DRAGGING_TIME = 60;


    /**
     * Stack containing the zoom calls on the TreeMap.
     */
    private Stack<TreeMapNode> zoomStack;

    /**
     * This object stores the last clicked rectangle in the TreeMap, in order to 
     * repaint it when another rectangle will be selected.
     */
    private static Comp lastClicked;
    
    /**
     * List of objects observing this.
     */
    private List<TreeMapObserver> observers;

    private ToolTipRenderer tooltipRenderer = new SimpleRenderer();

    public TreeMapComponent() {
        this(null);
    }

    /**
     * Constructor. It draw a TreeMap of the given tree in according to the 
     * {@Dimension} object in input.
     * 
     * @param tree the tree to represent as TreeMap.
     */
    public TreeMapComponent(TreeMapNode tree) {
        super();
        this.tree = tree;
        lastDim = getSize();
        this.zoomStack = new Stack<>();
        this.observers = new ArrayList<>();

        if (tree != null) {
            this.zoomStack.push(this.tree);
            processAndDrawTreeMap(this.tree);
        }

        addResizeListener(this);
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
        resetTreeMapNodeWeights(this.tree);
    }

    private void resetTreeMapNodeWeights(TreeMapNode node) {
        Objects.requireNonNull(node);
        node.setWeight(node.getRealWeight());

        for(TreeMapNode child : node.getChildren()) {
            resetTreeMapNodeWeights(child);
        }
    }

    public void setToolTipRenderer(ToolTipRenderer renderer) {
        this.tooltipRenderer = renderer;
    }

    /**
     * This method is responsible for the TreeMap drawing process.
     * @param tree the tree to represent as TreeMap.
     */
    private void drawTreeMap(TreeMapNode tree) {
        // draw root
        drawMainComp(tree);
        setBorderStyle(borderStyle);
        
        // draw subtrees nested in children 
        for (TreeMapNode child : tree.getChildren()) {
            drawSubTree(child, mainComp);
        }
        // setup this component
        prepareGUI();
    }

    /**
     * This method prepares the layout for this component. 
     */
    private void prepareGUI() {
        setLayout(new BorderLayout());
        setBounds(mainComp.getBounds());
        setBorder(null);
        add(mainComp, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * This method prepares the main component which is the parent object where 
     * sub components will be placed. 
     * @param tree the tree's root used to prepare the main component.
     */
    private void drawMainComp(TreeMapNode tree) {
        mainComp = new Comp();
        mainComp.setLayout(null);
        mainComp.setBounds(tree.getRectangle().getBounds());        
        mainComp.setNode(tree);
        cachedLabel = new Label(TITLE + tree.getLabel());        
        addLabelIfPossible(TITLE + tree.getLabel(), mainComp);
    }

    /**
     * Create a TreeMapComp from the given node. The component is not 
     * instantiated as a new component but is cloned from an existing one, in 
     * order to improve performance.
     * 
     * @param node the node to represent as a component.
     * @return the component representing the given node.
     */
    private Comp renderizeNode(TreeMapNode node) {
        // if the rectangle's node is too small to be viewed, don't draw it.
        if (node.getRectangle().getWidth() <= MIN_SIDE || 
                node.getRectangle().getHeight() <= MIN_SIDE) {
            return null;
        }

        Comp comp = new Comp(mainComp);
        comp.setBounds(node.getRectangle().getBounds());

        return comp;
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
    private Label addLabelIfPossible(String s, Container cont) {
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
            Label label = new Label(cachedLabel);
            label.setBounds(5, 1, cont.getWidth(), fontArea.height);
            label.setText(s);
            cont.add(label);
            return label;
        }
        return null;
    }

    /**
     * Draw the whole {@param tree}'s subtree inside the given component.
     * @param tree the tree to draw
     * @param parent the component in which build the tree.
     */
    private void drawSubTree(TreeMapNode tree, JComponent parent) {
        Comp comp = addCompIfPossible(tree, parent);

        // if space was enough to draw a component, try to draw its children
        if (comp != null) {
            comp.setNode(tree);
            for (TreeMapNode child : tree.getChildren()) {
                drawSubTree(child, comp);
            }
        }
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
                        processAndDrawTreeMap(Objects.requireNonNull(tree));
                    }
                } 
            }            
        };
        container.addComponentListener(adapter);
    }

    /**
     * This method checks if the given container has enough space to instantiate
     * a TreeMapComp object in it. If yes, a Label is cloned from an existing 
     * one, in order to improve performance. If not, it exits.
     * 
     * @param node the node to draw and add to the given container.
     * @param cont the parent container which will contain the new component.
     * @return true if the component was created and added, else false.
     */
    private Comp addCompIfPossible(TreeMapNode node, Container cont) {
        Rectangle2D rect = node.getRectangle();
        // if the ndoe's rectangle is smaller than the container, it is added
        if (cont.getWidth() > rect.getWidth() + X_PADDING && 
                cont.getHeight() > rect.getHeight() + Y_PADDING) {

            Comp toReturn = renderizeNode(node);
            if (toReturn == null) {
                return null;
            }
            addLabelIfPossible(TITLE + node.getLabel(), toReturn);

            // leaves some space from the parent's origin location
            Point loc = toReturn.getLocation();
            loc.x += X_PADDING;
            loc.y += Y_PADDING;
            toReturn.setLocation(loc);

            cont.add(toReturn);
            return toReturn;
        }
        return null;
    }

    public void processAndDrawTreeMap(TreeMapNode root) {
        tree = Objects.requireNonNull(root);
        Rectangle2D.Double newArea = tree.getRectangle();
        // give to the root node the size of this object so it can be recalculated
        newArea.width = getSize().width;
        newArea.height = getSize().height;

        // recalculate the tree
        TreeProcessor.processTreeMap(tree, newArea);

        removeAll();
        drawTreeMap(tree);
    }

    boolean isZoomInEnabled(TreeMapNode node) {
        return !(node == null
                || node.equals(Objects.requireNonNull(this.tree))
                || node.isLeaf());
    }

    public void zoomIn(TreeMapNode node) {
        if (isZoomInEnabled(node)) {
            fillZoomStack(node.getAncestors());
            processAndDrawTreeMap(node);
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
            processAndDrawTreeMap(zoomStack.peek());
            notifyZoomOutToObservers();
        }
    }

    /**
     * Zoom out the view directly to the original root.
     */
    public void zoomFull() {
        if (zoomStack.size() > 1) {
            clearZoomCallsStack();
            processAndDrawTreeMap(zoomStack.peek());
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
     * @param borderStyle the border style to use
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
        applyBorderToSubtree(mainComp, border);
    }
    
    /**
     * Traverse recursively the tree from the given component applying to it 
     * the default border.
     * @param comp the subtree's root from which apply the border style.
     */
    private void applyBorderToSubtree(Comp comp, Border border) {
        comp.setBorder(border);
        Component[] children = comp.getComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Comp) {
                applyBorderToSubtree((Comp) children[i], border);
            }
        }
    }

    /**
     * Return the last clicked component inside the TreeMap.
     * @return the last clicked {@Comp} object.
     */
    public Comp getClickedComponent() {
        return lastClicked;
    }

    class Label extends JLabel {

        private static final long serialVersionUID = 1L;

        public Label(String s) {
            super(s);
            setFont(FONT);
            setBounds(0, 0, getPreferredSize().width, FONT_SIZE);
        }

        protected Label(Label other) {
            this.setFont(other.getFont());
            this.setText(other.getText());
            this.setBackground(other.getBackground());
            this.setBounds(other.getBounds());
            this.setBorder(other.getBorder());
        }
    }    

    /**
     * This class provides some action listeners that allow to select it, performing
     * zoom operations for the treemap.
     */
    class Comp extends JComponent {

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
         * Reference to this.
         */
        private Comp thisComponent;

        public Comp() {
            super();
            thisComponent = this;
            addClickListener(this);
            addMouseListener(this);
        }

        public Comp(Comp other) {
            this.setBounds(other.getBounds());
            this.setBorder(other.getBorder());
            this.setLayout(other.getLayout());
            this.setOpaque(true);
        }

        public void setNode(TreeMapNode node) {
            this.node = node;
            this.color = node.getColor();
            this.setToolTipText(Objects.requireNonNull(TreeMapComponent.this.tooltipRenderer).render(node));
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

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g); 
            if (this.color != null) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());
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
                    // one left click select the rectangle
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectComp();
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
         * Add a mouse motion listener to this component. This allows for the mouse cursor to be changed into a
         * magnifying glass icon when the cursor enters a zoomable component, and back to a default cursor when it
         * exits a zoomable component.
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
         * This method gives a darker color to this component and restore the
         * original color to the last selected component.
         */
        private void selectComp() {
            if (lastClicked != null) {
                if (!lastClicked.getNode().isLeaf()) {
                    lastClicked.setColor(lastClicked.getColor().brighter());
                }
                lastClicked.repaint();
            } 
            lastClicked = thisComponent;
            if (!getNode().isLeaf()) {
                setColor(getColor().darker());
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

    public static class WeightAsSizeRenderer implements ToolTipRenderer {
        @Override
        public String render(TreeMapNode node) {
            Size size = new Size(node.getRealWeight(), Unit.B);
            return node.getLabel() + " - " + size.toString("%.2f");
        }
    }
}





