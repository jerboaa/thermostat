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
import com.redhat.thermostat.client.ui.Palette;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;

public class DataPane extends ContentPane {

    private final Palette top;
    private final Palette bottom;

    public DataPane() {
        this(Palette.WHITE, Palette.LIGHT_GRAY);
    }

    public DataPane(Palette top, Palette bottom) {
        this.top = top;
        this.bottom = bottom;
        setLayout(new BorderLayout());
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Color top = this.top.getColor();
        Color bottom = this.bottom.getColor();
        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
        final float[] fractions = {.0f, .2f, .4f, 1.f};
        final Color[] colors = {
                top,
                top,
                top,
                bottom,
        };
        LinearGradientPaint paint = new LinearGradientPaint(0, 0, 0,
                                                            getHeight(),
                                                            fractions, colors);
        graphics.setPaint(paint);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        graphics.dispose();
    }
}
