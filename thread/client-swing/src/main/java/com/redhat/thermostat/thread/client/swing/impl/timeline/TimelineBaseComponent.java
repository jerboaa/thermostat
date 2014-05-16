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
import com.redhat.thermostat.client.swing.components.GradientPanel;
import com.redhat.thermostat.client.ui.Palette;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 */
public class TimelineBaseComponent extends GradientPanel {

    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);

    private boolean selected;

    /**
     * This value defines the gap between two labels, in terms of units
     * (i.e. every TIME_GAP_BETWEEN_LABELS there will be a label displayed).
     */
    static final long TIME_GAP_BETWEEN_LABELS = 10;

    static final int FIXED_HEIGHT = 20;

    protected TimelineGroupThreadConverter timelinePageModel;
    protected SwingTimelineDimensionModel dimensionModel;

    public TimelineBaseComponent(TimelineGroupThreadConverter timelinePageModel,
                                 SwingTimelineDimensionModel dimensionModel)
    {
        super(Palette.LIGHT_GRAY.getColor(), Palette.WHITE.getColor());
        setFont(FONT);

        this.timelinePageModel = timelinePageModel;
        this.dimensionModel = dimensionModel;

        this.selected = false;
    }

    protected long getRound() {
        long min = timelinePageModel.getDataModel().getPageRange().getMin();
        long timeIncrement = dimensionModel.getIncrementInMillis();
        return min % (TIME_GAP_BETWEEN_LABELS * timeIncrement);
    }

    protected int getRoundedStartingX(Rectangle bounds) {

        long timeIncrement = dimensionModel.getIncrementInMillis();
        long round = getRound();
        long roundDelta = round / timeIncrement;

        int pixelIncrement = dimensionModel.getIncrementInPixels();
        long pixelDelta = roundDelta * pixelIncrement;

        return (int) (bounds.x - pixelDelta);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
        Rectangle bounds = graphics.getClipBounds();

        Color lines = Palette.GRAY.getColor();
        Color tickLines = Palette.THERMOSTAT_BLU.getColor();

        if (!isSelected()) {
            super.paintComponent(g);
        } else {
            graphics.setColor(Palette.ADWAITA_BLU.getColor());
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            lines = Palette.DARK_GRAY.getColor();
            tickLines = Palette.ROYAL_BLUE.getColor();
        }

        int pixelIncrement = dimensionModel.getIncrementInPixels();
        int x = getRoundedStartingX(bounds);
        int upperBound = bounds.x + bounds.width;

        graphics.setColor(Palette.GRAY.getColor());

        boolean resetColor = false;
        for (int i = x, j = 0; i < upperBound; i += pixelIncrement, j++) {
            if (j % 10 == 0) {
                graphics.setColor(tickLines);
                resetColor = true;
            }

            graphics.drawLine(i, 0, i, bounds.height);

            if (resetColor) {
                graphics.setColor(lines);
                resetColor = false;
            }
        }

        graphics.dispose();
    }

    @Override
    public int getHeight() {
        return FIXED_HEIGHT;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        pref.height = getHeight();
        return pref;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
