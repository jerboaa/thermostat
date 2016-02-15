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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

class ThermostatScrollBarUI extends BasicScrollBarUI {

    // TODO: move to common UIResources class
    private static final Color FG_COLOR = Palette.EARL_GRAY.getColor();
    
    @SuppressWarnings("serial")
    private class EmptyButton extends JButton {

        public EmptyButton() {
            setOpaque(false);
            setMaximumSize(new Dimension(0, 0));
        }
        
        @Override
        public void paint(Graphics g) {}
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        
        scrollBarWidth = 16;
        incrGap = 0;
        decrGap = 0;
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
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if(thumbBounds.isEmpty() || !scrollbar.isEnabled())     {
            return;
        }

        int x = 0;
        int y = 0;
        
        int w = 0;
        int h = 0;
        
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            x = thumbBounds.x + 4;
            y = thumbBounds.y;

            w = 8;
            h = thumbBounds.height - 1;
            
        } else {
            x = thumbBounds.x;
            y = thumbBounds.y + 4;
            
            w = thumbBounds.width - 1;
            h = 8;            
        }
        
        graphics.translate(x, y);

        Shape shape =  new RoundRectangle2D.Double(0, 0, w, h, 4, 4);

        graphics.setColor(FG_COLOR);
        graphics.fill(shape);
        
        graphics.dispose();
    }
}

