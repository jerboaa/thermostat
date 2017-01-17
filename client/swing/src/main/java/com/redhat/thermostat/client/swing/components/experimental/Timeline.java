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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.GradientPanel;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;

/**
 * Displays a timeline between the specified start and stop ranges, dynamically
 * adjusting itself as needed. The values of the ranges are interpreted as
 * timestamps in milliseconds.
 */
@SuppressWarnings("serial")
public class Timeline extends GradientPanel {

    /** Default height of this component. Subclasses may use different values */
    public static final int DEFAULT_HEIGHT = 25;

    private Range<Long> range;

    public Timeline(Range<Long> range) {

        super(Palette.LIGHT_GRAY.getColor(), Palette.WHITE.getColor());
        setFont(TimelineUtils.FONT);

        this.range = range;
    }

    public Range<Long> getRange() {
        return range;
    }

    public void setRange(Range<Long> newRange) {
        this.range = newRange;
        repaint();
    }

    @Override
    public int getHeight() {
        return DEFAULT_HEIGHT;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension dim = super.getMaximumSize();
        dim.height = getHeight();
        return dim;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension dim = super.getMinimumSize();
        dim.height = getHeight();
        return dim;
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
        super.paintComponent(g);

        if (range == null) {
            return;
        }

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);

        Rectangle bounds = g.getClipBounds();

        TimeUnit timeUnitForTicks = getBestTimeUnit();
        
        drawTicks(graphics, bounds, timeUnitForTicks);

        if (isEnabled()) {
            graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
        } else {
            graphics.setColor(Color.GRAY);
        }
        graphics.drawLine(bounds.x, bounds.height - 1, bounds.width, bounds.height - 1);

        graphics.dispose();
    }

    /**
     * Returns a TimeUnit and the number of subticks needed for that TimeUnit
     * need to display the current range
     */
    private TimeUnit getBestTimeUnit() {
        long min = range.getMin();
        long max = range.getMax();

        List<TimeUnit> units = new ArrayList<>();
        units.add(TimeUnit.DAYS);
        units.add(TimeUnit.HOURS);
        units.add(TimeUnit.MINUTES);
        units.add(TimeUnit.SECONDS);
        units.add(TimeUnit.MILLISECONDS);

        /* Find the largest unit of time suitable for the range */
        for (TimeUnit unit: units) {
            long millis = unit.toMillis(1);
            if (Math.abs(max - min) >= millis) {
                return unit;
            }
        }

        return null;
    }

    private void drawTicks(Graphics2D graphics, Rectangle bounds, TimeUnit tickUnit) {
        Font font = graphics.getFont();

        int widthOfOneCharacter = (int) font.getStringBounds("0", graphics.getFontRenderContext()).getWidth();

        long deltaInMilliseconds = Math.max(1, tickUnit.toMillis(1) / 10);

        // some heuristics for spacing
        int targetNumberOfIntervals = (int) (getWidth() / (10 * widthOfOneCharacter) / 1.3);

        while ((range.getMax() - range.getMin()) / deltaInMilliseconds > targetNumberOfIntervals) {
            deltaInMilliseconds *= 2;

        }

        long start = (range.getMin() / deltaInMilliseconds) * deltaInMilliseconds;
        long end = range.getMax();

        DateFormat df = null;

        switch (tickUnit) {
        case DAYS:
            df = new SimpleDateFormat("YY-MM-dd");
            break;
        case HOURS:
            df = new SimpleDateFormat("hh:mm a");
            break;
        case MINUTES:
            df = new SimpleDateFormat("hh:mm a");
            break;
        case SECONDS:
            df = new SimpleDateFormat("mm.ss");
            break;
        default:
            df = new SimpleDateFormat("hh:mm:ss a");
        }

        Paint gradient = new GradientPaint(0, 0, Palette.WHITE.getColor(), 0,
                getHeight(), Palette.GRAY.getColor());

        LongRangeNormalizer normalizer = new LongRangeNormalizer(range, new Range<Long>(0l, (long)bounds.width));

        for (long i = start; i < end; i += deltaInMilliseconds) {
            int x = (int) normalizer.getValueNormalized(i);

            if (isEnabled()) {
                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
            } else {
                graphics.setColor(Color.GRAY);
            }

            graphics.drawLine(x, 0, x, bounds.height);

            graphics.setPaint(gradient);
            String value = df.format(new Date(i));

            int stringWidth = (int) font.getStringBounds(value,
                    graphics.getFontRenderContext()).getWidth() - 1;
            int stringHeight = (int) font.getStringBounds(value,
                    graphics.getFontRenderContext()).getHeight();
            graphics.fillRect(x + 1, bounds.y + 5, stringWidth + 4, stringHeight +
                    4);

            if (isEnabled()) {
                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
            } else {
                graphics.setColor(Color.GRAY);
            }

            graphics.drawString(value, x + 1, bounds.y + stringHeight + 5);
        }
    }

}

