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

import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FontAwesomeIconTest {

    @Test
    public void testIcon() {
        FontAwesomeIcon icon = new FontAwesomeIcon('\uf004', 20);
        
        assertEquals(20, icon.getIconHeight());
        assertEquals(20, icon.getIconWidth());
        
        BufferedImage buffer = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffer.getGraphics();
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 25, 25);
        
        icon.paintIcon(null, g, 0, 0);
        
        g.dispose();
        
        int rgb = buffer.getRGB(0, 0);
        
        assertEquals(0xFFFFFFFF, rgb);
        
        rgb = buffer.getRGB(12, 8);
        assertEquals(0xFF000000, rgb);
        
        rgb = buffer.getRGB(12, 12);
        assertEquals(0xFF000000, rgb);
        
        rgb = buffer.getRGB(10, 16);
        assertEquals(0xFF000000, rgb);

        // on the border of the image, this is some gray due to antialiasing
        rgb = buffer.getRGB(10, 18);
        assertFalse(0xFF000000 == rgb);
        assertFalse(0xFFFFFFFF == rgb);

        rgb = buffer.getRGB(10, 20);
        assertEquals(0xFFFFFFFF, rgb);

        rgb = buffer.getRGB(20, 20);
        assertEquals(0xFFFFFFFF, rgb);
    }

}

