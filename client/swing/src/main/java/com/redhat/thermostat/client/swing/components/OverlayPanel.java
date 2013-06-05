/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

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
    
    private JPanel content;
    private JPanel titlePane;

    private ShadowLabel overlayTitle;
    
    private boolean displayArrow;
    
    /**
     * Creates a new {@link OverlayPanel}, with an arrow facing upward.
     */
    public OverlayPanel(String title) {
        this(title, true);
    }
    
    /**
     * Creates a new {@link OverlayPanel}. The panel will display an up facing
     * arrow if {@code displayArrow} is {@code true}.
     */
    public OverlayPanel(String title, boolean displayArrow) {
        
        this.displayArrow = displayArrow;
        
        setOpaque(false);
        setBorder(new OverlayBorder());
        setLayout(new BorderLayout(0, 10));
        
        titlePane = new JPanel();
        titlePane.setOpaque(true);

        overlayTitle = new ShadowLabel(title);
        
        titlePane.setBorder(new TitleBorder());
        titlePane.setBackground(Palette.ROYAL_BLUE.getColor());
        overlayTitle.setForeground(Palette.WHITE.getColor());

        titlePane.add(overlayTitle);
        
        content = new JPanel();
        content.setOpaque(false);
        
        super.add(titlePane, BorderLayout.NORTH);
        super.add(content, BorderLayout.CENTER);

        // a bit more useful layout than the default
        content.setLayout(new BorderLayout());
        
        setOverlayVisible(false);
        
        // filter events, we don't want them to reach components below us
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseMotionAdapter() {});
        addKeyListener(new KeyAdapter() {});
        setFocusTraversalKeysEnabled(false);
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
    
    @Override
    protected void paintComponent(Graphics g) {

        GraphicsUtils utils = GraphicsUtils.getInstance();

        Graphics2D graphics = utils.createAAGraphics(g);
        graphics.setColor(utils.deriveWithAlpha(Palette.PALE_GRAY.getColor(), 200));
 
        Rectangle clip = graphics.getClipBounds();
        Insets borderInsets = getBorder().getBorderInsets(this);
        
        clip.height = clip.height - borderInsets.bottom - borderInsets.top;
        clip.width = clip.width - borderInsets.left - borderInsets.right;
        
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
                return;
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
}
