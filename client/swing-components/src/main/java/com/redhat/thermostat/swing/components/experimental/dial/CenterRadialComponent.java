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

package com.redhat.thermostat.swing.components.experimental.dial;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
class CenterRadialComponent extends RadialComponent {

    protected boolean needRepaint;

    public CenterRadialComponent(int percentage, int zorder) {
        super(percentage, zorder);
        needRepaint = false;
    }
    
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        needRepaint = true;
    }
    
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        needRepaint = true;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        needRepaint = false;
    }
    
    protected boolean checkBuffers() {
        if (buffer == null                     ||
            buffer.getWidth()  != getWidth()   ||
            buffer.getHeight() != getHeight())
        {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            mask = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            
            Graphics2D maskGraphics = (Graphics2D) mask.getGraphics();
            maskGraphics.setColor(Palette.WHITE.getColor());
            maskGraphics.fillRect(0, 0, mask.getWidth(), mask.getHeight());
            maskGraphics.dispose();
            
            needRepaint = true;
        }
            
        return needRepaint;
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

        graphics.setColor(getBackground());
        Arc2D.Float circle = new Arc2D.Float(x, y, diameter, diameter, 0, 360, Arc2D.OPEN);
        graphics.fill(circle);
        
        maskGraphics.setColor(Palette.BLACK.getColor());
        maskGraphics.fill(circle);
        
        circle = new Arc2D.Float(x + 5, y + 5, diameter - 10, diameter - 10, 0, 360, Arc2D.OPEN);
        graphics.setColor(getForeground());
        graphics.fill(circle);
                
        graphics.dispose();
        maskGraphics.dispose();
    }
}
