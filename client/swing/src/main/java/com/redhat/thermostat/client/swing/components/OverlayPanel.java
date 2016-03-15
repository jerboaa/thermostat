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

package com.redhat.thermostat.client.swing.components;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.EventListener;
import java.util.EventObject;

/**
 * A panel meant to be stacked on top of existing components to display
 * informations in an floating overlay.
 * 
 * <br /><br />
 * 
 * <strong>Note</strong>: By default, the panel is invisible even when the
 * component it belongs sets all its children to be visible.
 * For this reason, the {@link #setVisible(boolean)}
 * method is a no-op when setting the panel visible and the user should
 * only use {@link #setOverlayVisible(boolean)} to turn on and off the
 * visibility of this panel.
 */
@SuppressWarnings("serial")
public class OverlayPanel extends JPanel {

    private static final char CLOSE_ICON_ID = '\uf00d';
    private static final char CLOSE_HOVERSTATE_ICON_ID = '\uf057';
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private JPanel content;
    private JPanel titlePane;
    private JButton closeButton;

    private ShadowLabel overlayTitle;
    
    private boolean displayArrow;
    private boolean showCloseButton;
    
    /**
     * Creates a new {@link OverlayPanel}, with an arrow facing upward.
     */
    public OverlayPanel(LocalizedString title) {
        this(title, true);
    }
    
    /**
     * Creates a new {@link OverlayPanel}. The panel will display an up facing
     * arrow if {@code displayArrow} is {@code true}.
     */
    public OverlayPanel(LocalizedString title, boolean displayArrow) {
        this(title, displayArrow, false);
    }

    public OverlayPanel(LocalizedString title, boolean displayArrow, boolean showCloseButton) {
        this.displayArrow = displayArrow;
        this.showCloseButton = showCloseButton;
        
        setOpaque(false);
        setBorder(new OverlayBorder());
        setLayout(new BorderLayout(0, 10));
        
        setName(OverlayPanel.class.getName());

        titlePane = new JPanel();
        titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
        titlePane.setOpaque(true);

        overlayTitle = new ShadowLabel(title);

        titlePane.setBorder(new TitleBorder());
        titlePane.setBackground(Palette.ROYAL_BLUE.getColor());
        overlayTitle.setForeground(Palette.WHITE.getColor());

        closeButton = new JButton();
        closeButton.setIcon(getCloseButtonInvisibleIcon());
        closeButton.setBackground(TRANSPARENT);

        if (showCloseButton) {
            titlePane.add(closeButton);
        }
        titlePane.add(Box.createHorizontalGlue());
        titlePane.add(overlayTitle);
        titlePane.add(Box.createHorizontalGlue());
        if (showCloseButton) {
            Component closeButtonPlaceholder = Box.createRigidArea(closeButton.getPreferredSize());
            titlePane.add(closeButtonPlaceholder);
        }

        content = new JPanel();
        content.setOpaque(false);
        
        super.add(titlePane, BorderLayout.NORTH);
        super.add(content, BorderLayout.CENTER);

        // a bit more useful layout than the default
        content.setLayout(new BorderLayout());
        
        setOverlayVisible(false);
        
        installListeners();
    }

    private FontAwesomeIcon getCloseButtonHoverStateIcon() {
        return getCloseButtonIcon(CLOSE_HOVERSTATE_ICON_ID, overlayTitle.getForeground());
    }

    private FontAwesomeIcon getCloseButtonVisibleIcon() {
        return getCloseButtonIcon(CLOSE_ICON_ID, overlayTitle.getForeground());
    }

    private FontAwesomeIcon getCloseButtonInvisibleIcon() {
        return getCloseButtonIcon(CLOSE_ICON_ID, titlePane.getBackground());
    }

    private FontAwesomeIcon getCloseButtonIcon(char iconId, Color color) {
        return new FontAwesomeIcon(iconId, (int) (overlayTitle.getPreferredSize().getHeight() * 0.6), color);
    }
    
    private void installListeners() {
        CloseButtonVisibilityListener closeButtonVisibilityListener = new CloseButtonVisibilityListener();
        titlePane.addMouseListener(closeButtonVisibilityListener);
        closeButton.addMouseListener(closeButtonVisibilityListener);
        closeButton.addMouseListener(new CloseButtonBackgroundColorListener());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean withinContentBounds = content.getBounds().contains(e.getPoint());
                boolean withinTitleBounds = titlePane.getBounds().contains(e.getPoint());
                boolean clickedBorder = !withinContentBounds && !withinTitleBounds;
                if (clickedBorder && isVisible()) {
                    fireCloseEvent();
                }
            }
        });

        // filter events, we don't want them to reach components through us
        addMouseMotionListener(new MouseMotionAdapter() {});
        addKeyListener(new KeyAdapter() {});
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fireCloseEvent();
            }
        });
        setFocusTraversalKeysEnabled(false);

        final int NO_MODIFIERS = 0;
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, NO_MODIFIERS);
        javax.swing.Action closeOverlay = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireCloseEvent();
            }
        };
        getActionMap().put("close", closeOverlay);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "close");
    }

    @Override
    public Component add(Component comp) {
        return content.add(comp);
    }
    
    @Override
    public void remove(Component comp) {
        content.remove(comp);
    }

    @Override
    public void removeAll() {
        content.removeAll();
    }

    /**
     * Provides a {@link MouseListener} intended for use in GlassPanes. This listener generates
     * close events if the overlay is visible, and notifies all registered
     * {@link CloseEventListener} instances on
     * this overlay of the close event. Then the click event is passed through to whichever component
     * would have received the event had the GlassPane not intercepted it. This allows for clicking outside
     * of an overlay to notify the hosting view that it should close the overlay.
     */
    public MouseListener getClickOutCloseListener(final Component parent) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isVisible()) {
                    fireCloseEvent();
                }
                Component component = SwingUtilities.getDeepestComponentAt(parent, e.getX(), e.getY());
                if (component != parent) {
                    MouseEvent converted = SwingUtilities.convertMouseEvent(parent, e, component);
                    component.dispatchEvent(converted);
                }
            }
        };
    }

    private void fireCloseEvent() {
        CloseEvent event = new CloseEvent(this);
        Object[] listeners = listenerList.getListeners(CloseEventListener.class);
        for (int i = listeners.length - 1; i >= 0; i--) {
            ((CloseEventListener) listeners[i]).closeRequested(event);
        }
    }

    /**
     * Add a listener which will decide what to do when this OverlayPanel is requested to be closed.
     * If the host view summons and hides the overlay using a toggle button, for example, then the view
     * should provide a listener which performs a click event upon the toggle button. If there are no other
     * UI elements which reflect the state of the overlay's visibility then it is safe for the listener
     * to simply call {@link #setOverlayVisible(boolean)} directly.
     */
    public void addCloseEventListener(CloseEventListener listener) {
        listenerList.add(CloseEventListener.class, listener);
    }

    public void removeCloseEventListener(CloseEventListener listener) {
        listenerList.remove(CloseEventListener.class, listener);
    }

    @Override
    protected void paintComponent(Graphics g) {

        GraphicsUtils utils = GraphicsUtils.getInstance();

        Graphics2D graphics = utils.createAAGraphics(g);
        graphics.setColor(utils.deriveWithAlpha(Palette.PALE_GRAY.getColor(), 200));
        
        Rectangle clip = graphics.getClipBounds();
        Insets borderInsets = getBorder().getBorderInsets(this);
        
        clip.height = getHeight() - borderInsets.bottom - borderInsets.top;
        clip.width = getWidth() - borderInsets.left - borderInsets.right;
        
        graphics.translate(borderInsets.left, borderInsets.top);
        graphics.fillRect(clip.x, clip.y, clip.width, clip.height);
        
        graphics.dispose();
    }
    
    /**
     * No-op when setting the panel visible.
     * Please, use {@link #setOverlayVisible(boolean)} instead.
     * 
     * @see #setOverlayVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        // no-op otherwise
        if (!visible) {
            setOverlayVisible(visible);
        }
    }
    
    /**
     * Sets the visibility of this panel.
     * Users of the OverlayPanel generally should not call this method directly. Instead,
     * use {@link #getClickOutCloseListener(Component)} and
     * {@link #addCloseEventListener(CloseEventListener)}.
     */
    public void setOverlayVisible(boolean visible) {
        super.setVisible(visible);
    }
    
    /**
     * Paints the border of the TitlePane
     */
    private class TitleBorder extends DebugBorder {
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(overlayTitle.getForeground());
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            
            insets.top = 2;
            insets.left = 2;
            insets.right = 2;
            insets.bottom = 2;
            
            return insets;
        }
    }
    
    /**
     * Paints the drop shadow around the overlay.
     */
    private class OverlayBorder extends DebugBorder {
        
        private BufferedImage buffer; 
        
        private static final boolean DEBUG = false;
                
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (DEBUG) {
                super.paintBorder(c, g, x, y, width, height);
            }
            
            if (buffer != null && buffer.getWidth() == getWidth() && buffer.getHeight() == getHeight()) {
                g.drawImage(buffer, 0, 0, null);
                return;
            }
                        
            GraphicsUtils utils = GraphicsUtils.getInstance();

            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics bufferGraphics = buffer.getGraphics();
            Graphics2D graphics = utils.createAAGraphics(bufferGraphics);
            bufferGraphics.dispose();
            
            Insets insets = getBorderInsets(c);
                                    
            Area clip = new Area(new Rectangle(x, y, width, height));
            BufferedImage dropShadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            x = x + insets.left;
            y = y + insets.top;
            
            width = width - insets.right - insets.left;
            height = height - insets.top - insets.bottom;
            
            Area inside = new Area(new Rectangle(x, y, width, height));
            clip.subtract(inside);
            graphics.setClip(clip);
            
            Graphics2D dropShadowGraphics = dropShadow.createGraphics();
            dropShadowGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            dropShadowGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            dropShadowGraphics.setColor(utils.deriveWithAlpha(Palette.BLACK.getColor(), 100));
            dropShadowGraphics.fillRoundRect(x - 5, y + 5, width + 10, height + 5, 15, 15);
            dropShadowGraphics.dispose();
            
            dropShadow = GaussianBlur.applyFilter(20, dropShadow);
            
            graphics.drawImage(dropShadow, 0, 0, null);
            
            if (displayArrow) {
                int x1Points[] = {0, 4, 4, 8};
                int y1Points[] = {4, 0, 0, 4};

                GeneralPath polyline =  new GeneralPath(GeneralPath.WIND_EVEN_ODD, x1Points.length);
            
                polyline.moveTo(x1Points[0], y1Points[0]);
                for (int index = 1; index < x1Points.length; index++) {
                    polyline.lineTo(x1Points[index], y1Points[index]);
                };
                polyline.closePath();
            
                graphics.setColor(Palette.BLACK.getColor());
                x += (width / 2) - 4;
                y -= 4;
                graphics.translate(x, y);
                graphics.fill(polyline);
            }
            
            graphics.dispose();
            
            g.drawImage(buffer, 0, 0, null);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            
            insets.top = 30;
            insets.left = 50;
            insets.right = 50;
            insets.bottom = 50;
            
            return insets;
        }
    }

    private class CloseButtonVisibilityListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            closeButton.setIcon(getCloseButtonVisibleIcon());
        }

        @Override
        public void mouseExited(MouseEvent e) {
            closeButton.setIcon(getCloseButtonInvisibleIcon());
        }
    }

    private class CloseButtonBackgroundColorListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            closeButton.setIcon(getCloseButtonHoverStateIcon());
        }

        @Override
        public void mouseExited(MouseEvent e) {
            closeButton.setIcon(getCloseButtonVisibleIcon());
        }
    }

    public interface CloseEventListener extends EventListener {
        void closeRequested(CloseEvent event);
    }

    public static class CloseEvent extends EventObject {

        public CloseEvent(OverlayPanel source) {
            super(source);
        }

        @Override
        public OverlayPanel getSource() {
            return (OverlayPanel) source;
        }
    }

}

