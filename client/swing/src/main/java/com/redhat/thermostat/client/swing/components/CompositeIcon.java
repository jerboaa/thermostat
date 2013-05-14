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

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.IconDescriptor;

/**
 * 
 */
@SuppressWarnings("serial")
public class CompositeIcon extends Icon {

    private Icon mask;
    
    public CompositeIcon(IconDescriptor mask, IconDescriptor thisIcon) {
        this(mask, new Icon(thisIcon));
    }

    public CompositeIcon(IconDescriptor mask, Icon thisIcon) {
        this(new Icon(mask), thisIcon);
    }
    
    public CompositeIcon(Icon mask, Icon thisIcon) {
        super(thisIcon.getImage());
        this.mask = mask;
    }
    
    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
      
        Graphics2D graphics = utils.createAAGraphics(g);
        
        int iconW = getIconWidth();
        int iconH = getIconHeight();
      
        BufferedImage imageBuffer = new BufferedImage(iconW, iconH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D buffer = imageBuffer.createGraphics();

        mask.paintIcon(null, buffer, 0, 0);
        
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_IN);
        buffer.setComposite(ac);

        super.paintIcon(null, buffer, 0, 0);
        
        buffer.dispose();

        graphics.drawImage(imageBuffer, x, y, null);
        graphics.dispose();
    }
}
