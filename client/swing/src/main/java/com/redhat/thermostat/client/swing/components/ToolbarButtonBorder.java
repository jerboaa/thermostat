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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.plaf.UIResource;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
class ToolbarButtonBorder extends DebugBorder implements UIResource, Serializable {

    public ToolbarButtonBorder(ToolbarButton button) {
        // This class should only be used with a ToolbarButton
    }
    
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        
        Color colour = Palette.GRAY.getColor();
        if (model.isPressed()) {
            colour = Palette.PALE_GRAY.getColor();
        } else if (model.isRollover()) {
            colour = Palette.DARK_GRAY.getColor();
        }
        
        if (model.isRollover() || model.isPressed() || model.isSelected()) {
            GraphicsUtils graphicsUtils = GraphicsUtils.getInstance();
            Graphics2D graphics = graphicsUtils.createAAGraphics(g);            

            Shape shape = graphicsUtils.getRoundShape(width - 1, height - 1);

            graphics.setColor(colour);
            graphics.translate(x, y);
            graphics.draw(shape);

            graphics.dispose();
        }
    }
}

