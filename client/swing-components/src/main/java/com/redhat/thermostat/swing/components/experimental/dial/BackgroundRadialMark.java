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

package com.redhat.thermostat.swing.components.experimental.dial;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
class BackgroundRadialMark extends CenterRadialComponent {

    public BackgroundRadialMark(int percentage, int zorder) {
        super(percentage, zorder);
        setForeground(Palette.LIGHT_GRAY.getColor());
    }
    
    @Override
    protected void paintBackBufferAndMask() {
        
        if (!checkBuffers()) {
            return;
        }

        Rectangle rect = new Rectangle(0, 0, getWidth(), getHeight());

        int diameter = rect.width;
        if (rect.width > rect.height) {
            diameter = rect.height;
        }

        float x = rect.width/2 - diameter/2;
        float y = rect.height/2 - diameter/2;

        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        Graphics2D maskGraphics = (Graphics2D) mask.getGraphics();
        
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        graphics.setColor(Palette.ADWAITA_BLU.getColor());
        Arc2D.Float circle = new Arc2D.Float(x, y, diameter, diameter, 312, 275, Arc2D.PIE);
        graphics.fill(circle);
        
        maskGraphics.setColor(Palette.BLACK.getColor());
        maskGraphics.fill(circle);
        
        circle = new Arc2D.Float(x + 5, y + 5, diameter - 10, diameter - 10, 315, 269, Arc2D.PIE);
        graphics.setColor(getForeground());
        graphics.fill(circle);

        graphics.dispose();
        maskGraphics.dispose();
    }
}

