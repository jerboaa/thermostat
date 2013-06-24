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

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.lang.reflect.Field;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.ui.Palette;

class CleanTabUI extends BasicTabbedPaneUI {
    
    private Insets cleantabInsets;
    
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
        cleantabInsets = new Insets(5, 10, 2, 10);
    }
    
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        tabPane.setOpaque(true);
    }
    
    @Override
    protected void installDefaults() {
        super.installDefaults();
        lightHighlight = Palette.EARL_GRAY.getColor();
        contentBorderInsets = new Insets(1, 1, 1, 1);
        setTabsOverlap();
    }
    
    private void setTabsOverlap() {
        try {
            Field tabsOverlapBorderField = getClass().getSuperclass().getDeclaredField("tabsOverlapBorder");
            tabsOverlapBorderField.setAccessible(true);
            tabsOverlapBorderField.set(this, true);
            
        } catch (IllegalArgumentException | IllegalAccessException |
                 NoSuchFieldException | SecurityException ignore) {}
    }
    
    @Override
    public void paint(Graphics g, JComponent c) {
        lightHighlight = Palette.EARL_GRAY.getColor();
        super.paint(g, c);
    }
    
    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int width, int height,
                                  boolean isSelected)
    {
        if (!isSelected) {
            // TODO: implement for all orientations
            g.setColor(Palette.PALE_GRAY.getColor().darker());
            g.drawRect(x, y, width, height);

            g.setColor(lightHighlight);
            g.drawLine(x, y + height, x + width + 1, y + height);
     
        } else {
            g.setColor(lightHighlight);
            g.drawRect(x, y, width, height - 2);
            
            g.setColor(Palette.GRANITA_ORANGE.getColor());
            g.fillRect(x, y, width + 1, y + 1);
            
            g.setColor(Palette.LIGHT_GRAY.getColor());
            g.drawLine(x + 1, y + height - 2, x + width - 1, y + height - 2);
        }
    }
    
    protected JButton createScrollButton(int direction) {

        IconResource resource = IconResource.ARROW_LEFT;
        switch (direction) {
        case EAST:
        case NORTH:
            resource = IconResource.ARROW_RIGHT;
            break;

        case WEST:
        case SOUTH:
        default:
            resource = IconResource.ARROW_LEFT;
            break;
        }
        
        return new ArrowButton(resource.getIcon());
    }
    
    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                                int selectedIndex, int x, int y,
                                                int w, int h)
    {
        g.setColor(lightHighlight);
        if (tabPlacement != BOTTOM) {
            g.drawLine(x, y + h - 1, x + w, y + h - 1);
        } else {
            // TODO
        }
    }
    
    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
                                              int selectedIndex, int x, int y,
                                              int w, int h)
    {
        g.setColor(lightHighlight);
        if (tabPlacement != LEFT) {
            g.drawLine(x, y, x, y + h - 1);
        } else {
            // TODO
        }
    }
    
    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex, int x, int y,
                                               int w, int h)
    {
        g.setColor(lightHighlight);
        if (tabPlacement != RIGHT) {
            g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        } else {
            // TODO
        }
    }
    
    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
                                             int selectedIndex, int x, int y,
                                             int w, int h)
    {
        Rectangle bounds = selectedIndex < 0 ? null : getTabBounds(selectedIndex, calcRect);

        g.setColor(lightHighlight);
        if (tabPlacement != TOP || bounds == null) {
            g.drawLine(x, y, x + w - 1, y);

        } else {
            g.drawLine(x, y, bounds.x, y);
            g.drawLine(bounds.x + bounds.width, y, x + w - 1, y);
            
            g.setColor(Palette.LIGHT_GRAY.getColor());
            g.drawLine(bounds.x + 1, y, bounds.x + bounds.width - 1, y);
        }
    }
        
    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int width, int height,
                                      boolean isSelected)
    {
        g.setColor(Palette.LIGHT_GRAY.getColor());
        int delta = 2;
        if (!isSelected) {
            delta = 1;
        }
        g.fillRect(x, y + 1, width, height - delta);
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
