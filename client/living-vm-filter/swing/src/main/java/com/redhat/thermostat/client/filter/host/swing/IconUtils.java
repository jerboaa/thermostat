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

package com.redhat.thermostat.client.filter.host.swing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.redhat.thermostat.client.swing.components.EmptyIcon;
import com.redhat.thermostat.client.swing.components.Icon;

public class IconUtils {
    public static Icon resizeIcon(Icon source, int size) {
        Icon canvas = new EmptyIcon(size, size);
        
        float v1 = canvas.getIconWidth() / 2;
        float v0 = source.getIconWidth() / 2; 
                
        int x = (int) (v1 - v0 + 0.5);
                
        v1 = canvas.getIconHeight() / 2;
        v0 = source.getIconHeight() / 2; 
        
        int y = (int) (v1 - v0 + 0.5);

        BufferedImage image = new BufferedImage(canvas.getIconWidth(),
                                                canvas.getIconHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.drawImage(source.getImage(), x, y, null);
        
        return new Icon(image);
    }

    public static Icon overlay(Icon canvas, Icon overlay, int x, int y) {
        BufferedImage image = new BufferedImage(canvas.getIconWidth(),
                                                canvas.getIconHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        canvas.paintIcon(null, graphics, 0, 0);
        
        graphics.drawImage(overlay.getImage(), x, y, null);
        graphics.dispose();
        
        return new Icon(image);
    }
}
