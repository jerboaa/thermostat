/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.metal.MetalButtonUI;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;

class ActionButtonUI extends MetalButtonUI {

    private BufferedImage sourceIcon;
    private BufferedImage rollOverIcon;
    private Image disabledIcon;
    
    ActionButtonUI() {}
    
    private BufferedImage getBrighterImage(BufferedImage source) {
        float[] kernel = new float[] { 0, 0, 0, 0, 1.2f, 0, 0, 0, 0 };

        BufferedImageOp brighterOp = new ConvolveOp(new Kernel(3, 3, kernel),
                ConvolveOp.EDGE_NO_OP, null);
        return brighterOp.filter(source, new BufferedImage(source.getWidth(),
                source.getHeight(), source.getType()));
    }
    
    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                              Rectangle textRect, Rectangle iconRect)
    {
        // nothing to do
    }
    
    @Override
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        
        Icon icon = button.getIcon();
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        if (sourceIcon == null) {
            sourceIcon = new BufferedImage(w + 1, h + 1,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics imageGraphics = sourceIcon.getGraphics();
            icon.paintIcon(null, imageGraphics, 0, 0);
        }

        if (rollOverIcon == null) {
            rollOverIcon = getBrighterImage(sourceIcon);
        }
        
        if (disabledIcon == null) {
            disabledIcon = GrayFilter.createDisabledImage(sourceIcon);
        }

        int x = 3;
        int y = button.getHeight() / 2 - h / 2;

        String text = button.getText();
        if (text == null || text.equals("")) {
            x = button.getWidth() / 2 - w / 2;
        }
        
        if (!model.isEnabled()) {
            g.drawImage(disabledIcon, x, y, null);
        } else if (model.isRollover()) {
            g.drawImage(rollOverIcon, x, y, null);
        } else {
            g.drawImage(sourceIcon, x, y, null);
        }
    }
    
    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        GraphicsUtils graphicsUtils = GraphicsUtils.getInstance();
        Graphics2D graphics = graphicsUtils.createAAGraphics(g);
        FontMetrics metrics = b.getFontMetrics(b.getFont());
        graphicsUtils.drawStringWithShadow(b, graphics, text, b.getForeground(), textRect.x,
                                           textRect.y + metrics.getAscent());
    }

    @Override
    public void paint(Graphics g, JComponent c) {

        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();

        if (model.isRollover() || model.isPressed() || model.isSelected()) {
            GraphicsUtils graphicsUtils = GraphicsUtils.getInstance();
            Graphics2D graphics = graphicsUtils.createAAGraphics(g);

            graphicsUtils.setGradientPaint(graphics, 0, button.getHeight(), button.getBackground(), Palette.WHITE.getColor());
            
            Shape shape = graphicsUtils.getRoundShape(button.getWidth() - 1, button.getHeight() - 1);
            graphics.fill(shape);
            
            graphics.dispose();
        }
        
        super.paint(g, c);
    }
}

