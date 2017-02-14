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

package com.redhat.thermostat.client.swing.internal.components;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import sun.swing.SwingUtilities2;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

class ThermostatButtonUI extends MetalButtonUI {

    private GraphicsUtils graphicsUtils = GraphicsUtils.getInstance();

    private Shape getShape(ThermostatButton button) {
        return new Rectangle.Double(0, 0, button.getWidth(), button.getHeight());
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        ThermostatButton button = (ThermostatButton) b;

        Graphics2D graphics = getGraphics(g);

        Paint paint = button.getSelectedBackgroundPaint();
        graphics.setPaint(paint);

        Shape shape = getShape(button);
        graphics.fill(shape);

        graphics.dispose();
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ThermostatButton button = (ThermostatButton) b;
        FontMetrics metrics = b.getFontMetrics(b.getFont());

        Graphics2D graphics = getGraphics(g);

        ButtonModel model = button.getModel();
        Paint paint = null;
        if (model.isPressed()) {
            paint = button.getSelectedForegroundPaint();
        } else if (model.isRollover()) {
            paint = button.getBackgroundPaint();
        } else {
            paint = button.getForegroundPaint();
        }

        // FIXME: we should add this to GraphicsUtils
        graphics.setPaint(new Color(0f, 0f, 0f, 0.1f));
        SwingUtilities2.drawString(b, graphics, text, textRect.x,
                                   textRect.y + metrics.getAscent() + 1);
        graphics.setPaint(paint);
        SwingUtilities2.drawString(b, graphics, text, textRect.x,
                                   textRect.y + metrics.getAscent());
        graphics.dispose();
    }

    @Override
    public void paint(Graphics g, JComponent c) {

        ThermostatButton button = (ThermostatButton) c;

        ButtonModel model = button.getModel();
        Graphics2D graphics = getGraphics(g);

        Shape shape = getShape(button);

        if (!model.isPressed()) {
            Paint paint = button.getBackgroundPaint();
            graphics.setPaint(paint);

            graphics.fill(shape);

            if (model.isRollover()) {
                paint = button.getForegroundPaint();

                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, button.getBlend()));
                graphics.setPaint(paint);
                graphics.fill(shape);
            }
        }

        graphics.dispose();

        super.paint(g, c);
    }

    private Graphics2D getGraphics(Graphics g) {
        Graphics2D graphics = graphicsUtils.createAAGraphics(g);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return graphics;
    }
}
