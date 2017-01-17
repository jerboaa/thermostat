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

package com.redhat.thermostat.swing.components.experimental.dial;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

import com.redhat.thermostat.client.ui.Palette;

/**
 *
 */
@SuppressWarnings("serial")
class RadialTicks extends BackgroundRadialMark {

    private BackgroundRadialMark background;
    
    public RadialTicks(int percentage, int zorder, BackgroundRadialMark background) {
        super(percentage, zorder);
        this.background = background;
        setForeground(Palette.EARL_GRAY.getColor());
    }
    
    @Override
    protected boolean isTargetForEvents() {
        return false;
    }
    
    @Override
    protected void paintBackBufferAndMask() {
        if (!checkBuffers()) {
            return;
        }

        // we won't paint the mask here, since this isn't target for events
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        Rectangle rect = new Rectangle(0, 0, getWidth(), getHeight());

        int parentDiameter = background.getWidth();
        int diameter = rect.width;
        if (rect.width > rect.height) {
            parentDiameter = background.getHeight();
            diameter = rect.height;
        }

        float x = rect.width/2 - diameter/2;
        float y = rect.height/2 - diameter/2;        
        graphics.setColor(Palette.ADWAITA_BLU.getColor());
        
        Arc2D.Float circle = new Arc2D.Float(x, y, diameter, diameter, 312, 275, Arc2D.PIE);
        graphics.fill(circle);
        
        float radius = diameter / 2;
        float parentRadius = parentDiameter / 2;

        float delta = (radius - parentRadius)/2;
        
        parentRadius += delta;

        float centerX = (float) getWidth()/2;
        float centerY = (float) getHeight()/2;
        
        int the100W = graphics.getFontMetrics().stringWidth("100") / 4;
        int the100H = graphics.getFontMetrics().getHeight() / 6;

        graphics.translate(centerX - the100W, centerY - the100H);
        graphics.setColor(Palette.LIGHT_GRAY.getColor());
        
        int j = 0;
        
        // TODO: those should be exported
        double teta = 225;
        double endAngle = 315;
        
        double increment = Math.abs((360 + teta - endAngle) / 10);
        for (int i = 0; i < 11; i++) {            

            double tetaRadiants = Math.toRadians(teta);
            
            x = parentRadius * (float) Math.cos(tetaRadiants); 
            y = parentRadius * (float) Math.sin(tetaRadiants);
            
            graphics.drawString("" + j, x, -y);
            j += 10;
            
            teta -= increment;
        }
        
        graphics.dispose();
    }   
}

