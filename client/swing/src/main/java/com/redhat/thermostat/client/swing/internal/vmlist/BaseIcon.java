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

package com.redhat.thermostat.client.swing.internal.vmlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.beans.Transient;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.Icon;

@SuppressWarnings("serial")
class BaseIcon extends Icon {

    private boolean selected;
    private Icon source;
    
    public BaseIcon(boolean selected, Icon source) {
        this.selected = selected;
        this.source = source;
    }
    
    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        graphics.setPaint(getPaint());
        graphics.fillRect(x, y, getIconWidth(), getIconHeight());
        graphics.dispose();
    }
    
    @Override
    public int getIconHeight() {
        return source.getIconHeight();
    }
    
    @Override
    public int getIconWidth() {
        return source.getIconWidth();
    }
    
    private Paint getPaint() {
        UIDefaults palette = UIDefaults.getInstance();
        Color color = palette.getComponentFGColor();
        if (selected) {
            color = palette.getSelectedComponentFGColor();
        }
        return color;
    }
    
    @Override
    @Transient
    public Image getImage() {
        BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }
}
