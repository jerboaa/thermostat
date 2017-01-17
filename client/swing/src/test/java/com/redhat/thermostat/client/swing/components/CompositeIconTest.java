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

package com.redhat.thermostat.client.swing.components;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.Transient;

import org.junit.Test;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

public class CompositeIconTest {
    
    @Test
    public void testDisplayIcon() {

        CompositeIcon icon = new CompositeIcon(new MaskIcon(), new SourceIcon());
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        
        int pixel = image.getRGB(0, 0);
        
        int a = (pixel >> 24) & 0xFF;
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >>  8) & 0xFF;
        int b = pixel & 0xFF;
        
        assertEquals(0, a);
        assertEquals(0, r);
        assertEquals(0, g);
        assertEquals(0, b);
        
        pixel = image.getRGB(image.getWidth() - 1, 0);
        
        a = (pixel >> 24) & 0xFF;
        r = (pixel >> 16) & 0xFF;
        g = (pixel >>  8) & 0xFF;
        b = pixel & 0xFF;
        
        assertEquals(0, a);
        assertEquals(0, r);
        assertEquals(0, g);
        assertEquals(0, b);
        
        pixel = image.getRGB(image.getWidth() - 1, image.getHeight() - 1);
        
        a = (pixel >> 24) & 0xFF;
        r = (pixel >> 16) & 0xFF;
        g = (pixel >>  8) & 0xFF;
        b = pixel & 0xFF;
        
        assertEquals(0, a);
        assertEquals(0, r);
        assertEquals(0, g);
        assertEquals(0, b);
        
        pixel = image.getRGB(0, image.getHeight()/2);
        
        a = (pixel >> 24) & 0xFF;
        r = (pixel >> 16) & 0xFF;
        g = (pixel >>  8) & 0xFF;
        b = pixel & 0xFF;
        
        // alpha of the source image is 200
        assertEquals(200, a);
        assertEquals(0, r);
        assertEquals(0, g);
        assertEquals(0, b);
        
        pixel = image.getRGB(image.getWidth() - 1, image.getHeight()/2);
        
        a = (pixel >> 24) & 0xFF;
        r = (pixel >> 16) & 0xFF;
        g = (pixel >>  8) & 0xFF;
        b = pixel & 0xFF;
        
        // alpha of the source image is 200
        assertEquals(200, a);
        assertEquals(255, r);
        assertEquals(255, g);
        assertEquals(255, b);
    }
    
    private static class SourceIcon extends Icon {
        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            
            GraphicsUtils utils = GraphicsUtils.getInstance();
            
            Color left = utils.deriveWithAlpha(Palette.BLACK.getColor(), 200);
            Color right = utils.deriveWithAlpha(Palette.WHITE.getColor(), 200);
            
            g.setColor(left);
            g.fillRect(x, y, getIconWidth()/2, getIconHeight());
            
            g.setColor(right);
            g.fillRect(x + getIconWidth()/2, y, getIconWidth()/2, getIconHeight());
        }
        
        @Override
        public int getIconHeight() {
            return 48;
        }
        
        @Override
        public int getIconWidth() {
            return 48;
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
    
    private static class MaskIcon extends Icon {
        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {

            // draw a black line in the middle
            int w = getIconWidth()/2;
            int h = getIconHeight()/2;
            y = y + h;
            
            g.setColor(Color.BLACK);
            g.drawLine(x, y, x + getIconWidth() - 1, y);
        }
        
        @Override
        public int getIconHeight() {
            return 48;
        }
        
        @Override
        public int getIconWidth() {
            return 48;
        }
    }
}
