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
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.model.timeline.Timeline;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

@SuppressWarnings("serial")
public class TimelineComponent extends TimelineBaseComponent {

    private static final int MIN_HEIGHT = 42;

    private TimelineLabel label;
    private Timeline timeline;

    public TimelineComponent(TimelineGroupThreadConverter timelinePageModel,
                             SwingTimelineDimensionModel dimensionModel,
                             String name)
    {
        // TODO: get those from color properties
        super(timelinePageModel, dimensionModel);

        setLayout(new BorderLayout());
        label = new TimelineLabel(name);
        add(label, BorderLayout.LINE_START);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);

        Range<Long> pageRange = timelinePageModel.getDataModel().getPageRange();

        int y = MIN_HEIGHT / 2;

        LongRangeNormalizer normalizer = new LongRangeNormalizer(pageRange, 0, getWidth());
        if (timeline != null) {
            for (TimelineInfo sample : timeline) {
                graphics.setColor(sample.getColor().getColor());
                Range<Long> sampleRange = sample.getRange();

                int x0 = (int) normalizer.getValueNormalized(sampleRange.getMin());
                int x1 = (int) normalizer.getValueNormalized(sampleRange.getMax());

                graphics.fillRect(x0, y, x1 - x0 + 1, 5);
            }
        }
        graphics.dispose();
    }

    @Override
    public int getHeight() {
        return MIN_HEIGHT;
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

    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
        if (timeline != null && timeline.size() > 0) {
            label.setForeground(timeline.last().getColor().getColor());
        }
    }

    public Timeline getTimeline() {
        return timeline;
    }
}

