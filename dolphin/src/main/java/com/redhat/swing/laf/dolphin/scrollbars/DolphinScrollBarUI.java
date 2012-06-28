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

package com.redhat.swing.laf.dolphin.scrollbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollBarUI;

import com.redhat.swing.laf.dolphin.themes.DolphinTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

public class DolphinScrollBarUI extends MetalScrollBarUI {
        
    private class EmptyButton extends JButton {
        
        public EmptyButton() {
            setMaximumSize(new Dimension(0, 0));
        }
        
        @Override
        public void paint(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            DolphinThemeUtils.setAntialiasing(graphics);
            
            DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
            
            graphics.setColor(theme.getScrollBarTrackColor());
            
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    public static ComponentUI createUI(JComponent b) {       
        return new DolphinScrollBarUI();
    }
    
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return new EmptyButton();
    }
    
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return new EmptyButton();
    }
    
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
    
        Graphics2D graphics = (Graphics2D) g.create();
        DolphinThemeUtils.setAntialiasing(graphics);
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        
        graphics.translate(trackBounds.x, trackBounds.y);
        graphics.setColor(theme.getScrollBarTrackColor());
        
        graphics.fillRect(0, 0, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        
        // NOTE: those displacements are meant to ensure that always 3 pixels
        // are left between the thumb and the track

        int x = 0;
        int y = 0;
        
        int w = 0;
        int h = 0;
        
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            x = thumbBounds.x + 3;
            y = thumbBounds.y;

            w = thumbBounds.width - 4;
            h = thumbBounds.height - 1;
            
        } else {
            x = thumbBounds.x;
            y = thumbBounds.y + 3;
            
            w = thumbBounds.width - 1;
            h = thumbBounds.height - 5;
        }
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        Color thumbColour = null;
        if (scrollbar.getModel().getValueIsAdjusting()) {
            thumbColour = theme.getThumbMovingColor();
        } else if (checkFocus(new Rectangle(x, y, w, h))) {
            thumbColour = theme.getThumbFocusedColor();
        } else {
            thumbColour = theme.getThumbColor();
        }
        
        Graphics2D graphics = (Graphics2D) g.create();
        DolphinThemeUtils.setAntialiasing(graphics);
       
        graphics.translate(x, y);
        graphics.setColor(thumbColour);
        
        Shape shape = DolphinThemeUtils.getRoundShape(w, h, 8, 8);
        graphics.fill(shape);
        
        graphics.dispose();
    }

    private boolean checkFocus(Rectangle rectangle) {
        Point mouseCoords = scrollbar.getMousePosition(true);
        if (mouseCoords != null) {
            return rectangle.contains(mouseCoords);
        }
        return false;
    }
}
