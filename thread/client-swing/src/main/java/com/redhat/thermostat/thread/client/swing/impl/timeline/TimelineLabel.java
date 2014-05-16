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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.experimental.TimelineUtils;
import com.redhat.thermostat.client.ui.Palette;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 */
public class TimelineLabel extends JPanel {

    private String text;

    public TimelineLabel(String text) {
        setOpaque(false);
        setBorder(new TimelineBorder(true));

        this.text = text;

        JLabel nameLabel = new JLabel(text);
        nameLabel.setFont(TimelineUtils.FONT);
        nameLabel.setForeground(Palette.THERMOSTAT_BLU.getColor());

        add(nameLabel);
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = getHeight();
        return dim;
    }

    @Override
    public Dimension getSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {

        GraphicsUtils utils = GraphicsUtils.getInstance();

        Graphics2D graphics = utils.createAAGraphics(g);

        Color up = utils.deriveWithAlpha(Palette.WHITE.getColor(), 200);
        Color bottom = utils.deriveWithAlpha(Palette.GRAY.getColor(), 200);

        Paint gradient = new GradientPaint(0, 0, up, 0, getHeight(), bottom);
        graphics.setPaint(gradient);

        graphics.fillRect(0, 0, getWidth(), getHeight());

        graphics.dispose();
    }

    public String getText() {
        return text;
    }
}
