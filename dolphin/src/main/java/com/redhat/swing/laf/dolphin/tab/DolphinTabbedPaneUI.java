/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.swing.laf.dolphin.tab;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

import com.redhat.swing.laf.dolphin.themes.DolphinTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

public class DolphinTabbedPaneUI extends MetalTabbedPaneUI {

    private static final boolean keepBG;
    static {
        keepBG = DolphinThemeUtils.getCurrentTheme().getTabPaneKeepBackgroundColor();
    }
    
    private static final int MAGIC_NUMBER = 3;
    
    public static ComponentUI createUI(JComponent tabPane) {
        return new DolphinTabbedPaneUI();
    }
    
    private Shape createTabShape(int x, int y, int w, int h) {
        int x2Points[] = { x, x + MAGIC_NUMBER, x + w - MAGIC_NUMBER, x + w };
        int y2Points[] = { y + h, y , y, y + h };
        GeneralPath tab = 
                new GeneralPath(GeneralPath.WIND_EVEN_ODD, x2Points.length);

        tab.moveTo(x2Points[0], y2Points[0]);

        for (int index = 1; index < x2Points.length; index++) {
            tab.lineTo(x2Points[index], y2Points[index]);
        };
        tab.closePath();
        
        return tab;
    }
    
    private boolean bgSet;
    @Override
    public void paint(Graphics g, JComponent c) {
       if (!keepBG && !bgSet) {
           if (c instanceof JTabbedPane) {
               // well, who else...
               JTabbedPane tabPane = (JTabbedPane) c;
               
               DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
               
               for (int i = 0; i < tabPane.getComponentCount(); i++) {
                   tabPane.getComponent(i).setBackground(theme.getTabAreaBackground());
               }
               bgSet = true;
           }
       }
       super.paint(g, c);
    }
    
    @Override
    protected void paintTopTabBorder(int tabIndex, Graphics g, int x, int y,
                                     int w, int h, int btm, int rght, boolean isSelected)
    {
        Graphics2D graphics = (Graphics2D) g.create();
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        
        Color topColor = theme.getBorderGradientTopColor();
        Color bottomColor = theme.getBorderGradientBottomColor();
        DolphinThemeUtils.setAntialiasing(graphics);

        if (isSelected) {
            graphics.setStroke(new BasicStroke(1.5f));
            topColor = theme.getSelectionColor();
            DolphinThemeUtils.setGradientPaint(graphics, 0, y + h, topColor, bottomColor);
            graphics.draw(createTabShape(x - 2, y - 1, w + 4, h + 2));
        } else {
            DolphinThemeUtils.setGradientPaint(graphics, 0, y + h/2, topColor, bottomColor);
            graphics.draw(createTabShape(x, y, w, h));
        }
        
        graphics.dispose();
    }
    
    private void paintContentVerticalEdge(Graphics g, int x, int y,
                                          int w, int h)
    {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.translate(x, y);
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        
        LinearGradientPaint paint =
                new LinearGradientPaint(new Point2D.Float(0, 0),
                                        new Point2D.Float(0, h),
                                        new float[] { 0.0f, 0.2f, 1.0f },
                                        new Color[] { theme.getTabTopHedgeColor(),
                                                      theme.getTabTopHedgeColor(),
                                                      theme.getTabBottomHedgeColor()});
        graphics.setPaint(paint);
        graphics.drawLine(0, 0, 0, h);
        
        graphics.dispose();
    }
    
    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
                                              int selectedIndex, int x, int y,
                                              int w, int h)
    {
        if (tabPlacement == LEFT) {
            System.err.println("LEFT");
            super.paintContentBorderLeftEdge(g, tabPlacement, selectedIndex,
                                             x, y, w, h);
        }
        
        paintContentVerticalEdge(g, x, y, w, h);
    }

    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex, int x, int y,
                                               int w, int h)
    {
        if (tabPlacement == RIGHT) {
            super.paintContentBorderRightEdge(g, tabPlacement, selectedIndex,
                                              x, y, w, h);
        }
        
        paintContentVerticalEdge(g, x + w - 1, y, w, h);
    }
    
    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                                int selectedIndex, int x, int y,
                                                int w, int h)
    {
        if (tabPlacement == BOTTOM) {
            super.paintContentBorderBottomEdge(g, tabPlacement, selectedIndex,
                                               x, y, w, h);
        }
    }
    
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
                                             int selectedIndex, int x, int y,
                                             int w, int h)
    {
        // This is a copy of the paintContentBorderTopEdge from
        // MetalTabbedPaneUI with the x starting and end points of the line
        // fine tuned to the shape of our tab
        Graphics2D graphics = (Graphics2D) g.create();
        
        Rectangle selRect = selectedIndex < 0 ? null : getTabBounds(
                selectedIndex, calcRect);

        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        graphics.setColor(theme.getTabTopHedgeColor());
        
        graphics.setStroke(new BasicStroke(1.0f));
        DolphinThemeUtils.setAntialiasing(graphics);
        
        // Draw unbroken line if tabs are not on TOP, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != TOP || selectedIndex < 0
                || (selRect.y + selRect.height + 1 < y)
                || (selRect.x < x || selRect.x > x + w)) {
            graphics.drawLine(x, y, x + w - 2, y);
        } else {
            // Break line to show visual connection to selected tab
            graphics.drawLine(x, y, selRect.x - 2, y);
            if (selRect.x + selRect.width < x + w - 2) {
                graphics.drawLine(selRect.x + selRect.width + 2, y, x + w - 2, y);
            } else {
                graphics.setColor(shadow);
                graphics.drawLine(x + w - 2, y, x + w - 2, y);
            }
        }

        graphics.dispose();
    }
    
    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement,
            int selectedIndex) {
        super.paintContentBorder(g, tabPlacement, selectedIndex);
    }
    
    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect) {
        super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);
    }
    
    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected)
    {
        switch (tabPlacement) {
        case TOP: {
            Graphics2D graphics = (Graphics2D) g.create();
            
            DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();

            Color topColor = null;
            Color bottomColor = null;
            if (isSelected) {
                topColor = theme.getTabTopGradient();
                bottomColor = theme.getTabBottomGradient();
            } else {
                topColor = theme.getUnselectedTabTopGradient();
                bottomColor = theme.getUnselectedTabTopGradient();
            }
            
            DolphinThemeUtils.setAntialiasing(graphics);
            DolphinThemeUtils.setGradientPaint(graphics, 0, y + h/2, topColor, bottomColor);
            
            if (isSelected) {
                graphics.fill(createTabShape(x - 2, y - 1, w + 4, h + 2));
            } else {
                graphics.fill(createTabShape(x, y, w, h));
            }
            
            graphics.dispose();
        } break;
            
        default:
            super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            break;
        }
    }
    
    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                       int tabIndex, Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected)
    {
        switch (tabPlacement) {
        case TOP: {
            // TODO
        
        } break;
            
        default:
            super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected);
            break;
        }
    }
}
