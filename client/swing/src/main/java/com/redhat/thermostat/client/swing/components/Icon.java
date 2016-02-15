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

import com.redhat.thermostat.client.ui.IconDescriptor;
import com.redhat.thermostat.client.ui.PlatformIcon;

import javax.swing.ImageIcon;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.Transient;

/**
 */
@SuppressWarnings("serial")
public class Icon extends ImageIcon implements PlatformIcon {

    public Icon(IconDescriptor descriptor) {
        super(descriptor.getData().array());
    }

    public Icon() {
        super();
    }
    
    public Icon(byte[] data) {
        super(data);
    }
    
    public Icon(Image image) {
        super(image);
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
    
    /**
     * Returns a {@link Dimension} object with this {@link Icon}
     * {@code width} and {@code height}.
     */
    public Dimension getDimension() {
        return new Dimension(getIconWidth(), getIconHeight());
    }
}

