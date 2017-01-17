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

package com.redhat.thermostat.thread.client.swing.experimental.components;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.Palette;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import javax.swing.border.AbstractBorder;

/**
 * A configurable border that separates two components.
 */
public class Separator extends AbstractBorder {

    public enum Type {
        SOLID,
        SMOOTH,
    }

    public enum Side {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
    }

    private Side side;

    private UIDefaults defaults;
    private Type type;

    public Separator(UIDefaults defaults) {
        this(defaults, Side.TOP, Type.SOLID);
    }

    public Separator(UIDefaults defaults, Side side, Type type) {
        this.side = side;
        this.defaults = defaults;
        this.type = type;
    }

    private void paintSolidBorder(Graphics2D graphics, int x, int y, int width, int height) {

        Rectangle fillRect = new Rectangle(x, y, width, height);

        switch (side) {

        default:
        case TOP:
            graphics.setColor(Palette.DARK_GRAY.getColor());
            graphics.drawLine(x, y + 1, x + width - 1, y + 1);
            graphics.setPaint(Palette.WHITE.getColor());
            graphics.drawLine(x, y, x + width - 1, y);
            break;

        case BOTTOM:
            graphics.setColor(Palette.WHITE.getColor());
            graphics.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            graphics.setPaint(Palette.DARK_GRAY.getColor());//defaults.getComponentBGColor());
            graphics.drawLine(x, y + height - 2, x + width - 1, y + height - 2);
            break;

        case LEFT:
            graphics.setColor(Palette.WHITE.getColor());
            graphics.drawLine(x, y, x, y + height - 1);
            graphics.setPaint(Palette.DARK_GRAY.getColor());
            graphics.drawLine(x + 1, y, x + 1, y + height - 1);
            break;

        case RIGHT:
            graphics.setColor(Palette.WHITE.getColor());
            graphics.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
            graphics.setPaint(Palette.DARK_GRAY.getColor());
            graphics.drawLine(x + width - 2, y, x + width - 2, y + height - 1);
            break;
        }
    }

    private void paintSmoothBorder(Graphics2D graphics, int x, int y, int width, int height) {

        final float[] fractions = {.0f, .4f, .6f, 1.f};
        final Color[] colors = {
                (Color) defaults.getComponentBGColor(),
                (Color) defaults.getReferenceFieldIconColor(),
                (Color) defaults.getReferenceFieldIconColor(),
                (Color) defaults.getComponentBGColor(),
        };

        Rectangle gradientRect = new Rectangle(x, y, width, height);
        Rectangle fillRect = new Rectangle(x, y, width, height);

        switch (side) {
        default:
        case TOP:
            gradientRect.y = 0;
            gradientRect.height = 0;
            fillRect.height = 1;
            break;

        case BOTTOM:
            gradientRect.y = 0;
            gradientRect.height = 0;
            fillRect.y = y + height - 1;
            fillRect.height = 1;
            break;

        case LEFT:
            gradientRect.x = 0;
            gradientRect.width = 0;
            fillRect.width = 1;
            break;

        case RIGHT:
            gradientRect.x = 0;
            gradientRect.width = 0;
            fillRect.x = x + width - 1;
            fillRect.width = 1;
            break;
        }

        LinearGradientPaint paint =
                new LinearGradientPaint(gradientRect.x,
                                        gradientRect.y, gradientRect.width,
                                        gradientRect.height, fractions, colors);
        graphics.setPaint(paint);
        graphics.fill(fillRect);

        graphics.dispose();
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
        switch (type) {
        case SOLID:
            paintSolidBorder(graphics, x, y, width, height);
            break;

        case SMOOTH:
            paintSmoothBorder(graphics, x, y, width, height);
            break;
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {

        insets.top = 2;
        insets.left = 0;
        insets.right = 0;
        insets.bottom = 0;

        return insets;
    }
}
