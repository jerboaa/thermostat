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
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.thread.client.swing.experimental.components.DataPane;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 */
public class RulerComponent extends DataPane {

    protected class RenderingInfo {
        long startRendering;
        long stopRendering;

        long startMark;
        long increment;
    }

    protected final UIDefaults uiDefaults;
    protected TimelineModel model;
    private boolean selected;

    public RulerComponent(UIDefaults defaults, TimelineModel model) {

        super(Palette.LIGHT_GRAY, Palette.WHITE);

        this.uiDefaults = defaults;
        this.model = model;
        setFont(defaults.getDefaultFont().deriveFont(Font.PLAIN, 10.f));
        setName("ruler");
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        pref.height = getHeight();
        return pref;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getSize() {
        return getPreferredSize();
    }

    private RenderingInfo getRenderingInfo(Rectangle bounds) {

        RenderingInfo info = new RenderingInfo();
        info.stopRendering = bounds.x + bounds.width;
        info.increment = 10;

        long start = model.getRange().getMin();
        long visibleStartTime = start + model.getScrollBarModel().getValue();

        // small mark, 1 second boundary
        long mark = visibleStartTime - (1_000 * (visibleStartTime/1_000));
        info.startRendering = -mark;

        return info;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);

        Rectangle bounds = graphics.getClipBounds();
        if (!isSelected()) {
            super.paintComponent(g);
        } else {
            graphics.setPaint(uiDefaults.getSelectedComponentBGColor());
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        Color lines = Palette.PALE_GRAY.getColor();
        Color tickLines = Palette.DARK_GRAY.getColor();

        RenderingInfo info = getRenderingInfo(bounds);
        int height = bounds.y + bounds.height;

        boolean resetColor = false;
        long mark = 10l;

        graphics.setColor(lines);
        for (int i = (int) info.startRendering, j = (int) info.startMark;
             i < info.stopRendering; i += info.increment, j++)
        {
            if (j % mark == 0) {
                graphics.setColor(tickLines);
                resetColor = true;
            }

            graphics.drawLine(i, bounds.y, i, height);

            if (resetColor) {
                graphics.setColor(lines);
                resetColor = false;
            }
        }

        // TODO
//        if (drawLabels) {
//            drawLabels(graphics, info);
//        }

        graphics.dispose();
    }
}
