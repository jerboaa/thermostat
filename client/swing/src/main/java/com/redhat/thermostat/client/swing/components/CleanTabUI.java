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

package com.redhat.thermostat.client.swing.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;
import com.redhat.thermostat.client.ui.Palette;

class CleanTabUI extends BasicTabbedPaneUI {
    
    private Insets cleantabInsets;
    private UIDefaults uiDefaults = UIDefaultsImpl.getInstance();

    @SuppressWarnings("serial")
    private class ArrowButton extends ActionButton implements UIResource, SwingConstants {

        public ArrowButton(Icon icon) {
            super(icon);
            setRequestFocusEnabled(false);
        }

        public boolean isFocusTraversable() {
            return false;
        }
    }
    
    public CleanTabUI() {
        cleantabInsets = new Insets(5, 10, 5, 10);
    }
    
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        tabPane.setOpaque(true);
    }

    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected void installDefaults() {
        UIManager.getDefaults().put("TabbedPane.tabsOverlapBorder", true);
        UIManager.getDefaults().put("TabbedPane.contentAreaColor", uiDefaults.getComponentBGColor());
        UIManager.getDefaults().put("TabbedPane.shadow", Palette.GRAY.getColor());

        super.installDefaults();
        lightHighlight = Palette.GRAY.getColor();

        contentBorderInsets = new Insets(1, 0, 0, 0);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(1, 0, 1, 0);
        tabInsets = cleantabInsets;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        lightHighlight = Palette.GRAY.getColor();

        g.setColor((Color) uiDefaults.getComponentBGColor());
        g.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());

        g.setColor(lightHighlight);
        super.paint(g, c);

        if (tabPane.getTabPlacement() == TOP) {
            g.setColor(Palette.DARK_GRAY.getColor());
            g.drawLine(0, 0, tabPane.getWidth(), 0);
            g.setColor(Palette.GRAY.getColor());
            g.drawLine(0, 1, tabPane.getWidth(), 1);
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int width, int height,
                                  boolean isSelected)
    {
        g.setColor(Palette.DARK_GRAY.getColor());

        if (tabIndex > 0) {
            g.drawLine(x, y, x, y + height - 1);
        }

        if (tabIndex == tabPane.getTabCount() - 1) {
            g.drawLine(x + width, y, x + width, y + height - 1);
        }

        if (isSelected && tabPlacement == TOP) {
            g.setColor((Color) uiDefaults.getComponentBGColor());
            g.drawLine(x + (tabIndex > 0 ? 1 : 0), y - 1, x + width - 1, y - 1);
        }
    }
    
    protected JButton createScrollButton(int direction) {

        FontAwesomeIcon resource = null;
        switch (direction) {
        case EAST:
        case NORTH:
            resource = new FontAwesomeIcon('\uf105', 20, uiDefaults.getSelectedComponentBGColor());
            break;

        case WEST:
        case SOUTH:
        default:
            resource = new FontAwesomeIcon('\uf104', 20, uiDefaults.getSelectedComponentBGColor());
            break;
        }
        
        return new ArrowButton(resource);
    }

    private void paintTabAreaTopInset(Graphics2D graphics2D) {
        if (tabPane.getTabPlacement() == TOP) {
            graphics2D.setColor(Palette.DARK_GRAY.getColor());
            graphics2D.drawLine(0, 0, tabPane.getWidth(), 0);

            graphics2D.setColor(Palette.GRAY.getColor());
            graphics2D.drawLine(0, 1, tabPane.getWidth(), 1);
        }
    }

    private void paintTabAreaBottomInset(Graphics2D graphics2D) {
        if (tabPane.getTabPlacement() == TOP) {
            graphics2D.setColor(Palette.DARK_GRAY.getColor());
            Rectangle tabBounds = rects[0].getBounds();
            int h = tabBounds.height + tabAreaInsets.bottom;
            graphics2D.drawLine(tabPane.getX(), h, tabPane.getWidth(), h);
        }
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {

        Graphics2D graphics2D = (Graphics2D) g;

        graphics2D.setColor((Color) uiDefaults.getComponentBGColor());
        graphics2D.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());

        // this is a bit weird, we need to do it twice with and without the
        // affine transform because the caller sometime shift the content
        // depending if the scroll buttons are visible or not but unfortunately
        // we can't access this information
        paintTabAreaTopInset(graphics2D);
        AffineTransform transform = graphics2D.getTransform();
        graphics2D.setTransform(new AffineTransform());
        paintTabAreaTopInset(graphics2D);

        graphics2D.setTransform(transform);
        super.paintTabArea(g, tabPlacement, selectedIndex);

        paintTabAreaBottomInset(graphics2D);
        transform = graphics2D.getTransform();
        graphics2D.setTransform(new AffineTransform());
        paintTabAreaBottomInset(graphics2D);
        graphics2D.setTransform(transform);
    }

    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                                int selectedIndex, int x, int y,
                                                int w, int h)
    {
        // no border
    }
    
    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
                                              int selectedIndex, int x, int y,
                                              int w, int h)
    {
        // no border
    }
    
    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex, int x, int y,
                                               int w, int h)
    {
        // no border
    }
    
    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
                                             int selectedIndex, int x, int y,
                                             int w, int h)
    {
        g.setColor(Palette.DARK_GRAY.getColor());
        g.drawLine(x, y, x + w, y);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int width, int height,
                                      boolean isSelected)
    {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        if (isSelected) {
            Paint gradient = new GradientPaint(x, y, (Color) uiDefaults.getComponentBGColor(),
                                               x, height, Palette.GRAY.getColor());
            graphics.setPaint(gradient);

        } else {
            graphics.setColor(Palette.GRAY.getColor());
        }

        graphics.fillRect(x, y, width, height);
        graphics.dispose();
    }
        
    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected)
    {
        // no-op
    }
    
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return cleantabInsets;
    }
}

