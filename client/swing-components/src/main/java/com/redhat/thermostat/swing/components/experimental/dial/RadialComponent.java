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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
abstract class RadialComponent extends JComponent implements RadialOrder {

    protected BufferedImage mask;
    protected BufferedImage buffer;
    
    private int percentage;
    private int zorder;

    private Point2D.Float center;
    
    protected RadialComponent(int percentage, int zorder) {
        this.percentage = percentage;
        this.zorder = zorder;
    }
    
    @Override
    public int getOrderValue() {
        return zorder;
    }
    
    @Override
    public int getAreaPercentage() {
        return percentage;
    }
    
    public void setRadialCenter(Point2D.Float center) {
        this.center = center;
    }
    
    public Point2D.Float getRadialCenter() {
        return center;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        paintBackBufferAndMask();
        
        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
        
        graphics.drawImage(buffer, 0, 0, null);
        
        graphics.dispose();
    }
    
    protected abstract void paintBackBufferAndMask();
    
    protected boolean isTargetForEvents() {
        return true;
    }
    
    public BufferedImage getMask() {
        paintBackBufferAndMask();
        return mask;
    }
}
