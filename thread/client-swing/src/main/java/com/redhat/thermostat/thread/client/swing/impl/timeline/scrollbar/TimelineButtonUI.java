/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline.scrollbar;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

class TimelineButtonUI extends BasicButtonUI {

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                              Rectangle textRect, Rectangle iconRect)
    {
        // no focus :)
    }
    
    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        super.paintButtonPressed(g, b);
    }
    
    @Override
    public void paint(Graphics g, JComponent c) {
    
        JButton button = (JButton) c;
        
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        Rectangle clip = graphics.getClipBounds();
          
        Color top = Palette.WHITE.getColor();
        Color bottom = Palette.GRAY.getColor();
    
        ButtonModel model = button.getModel();
        if (model.isPressed()) {
            Color tmp = top;
            top = bottom;
            bottom = tmp;
        }
          
        Paint gradient = new GradientPaint(0, 0, top, 0, button.getHeight(), bottom);
          
        graphics.setPaint(gradient);
        graphics.fillRect(clip.x, clip.y, clip.width, clip.height);

        Icon icon = button.getIcon();
        if (icon != null) {
            int x = button.getWidth() / 2 - icon.getIconWidth() / 2;
            int y = button.getHeight() / 2 - icon.getIconHeight() / 2;
            icon.paintIcon(button, graphics, x, y);
        }

        super.paint(g, c);
    }
}
