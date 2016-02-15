/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import com.redhat.thermostat.client.swing.components.AbstractLayout;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangedTimelineProbe;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.Container;
import java.awt.Dimension;

/**
 *
 */
class RangeComponentLayoutManager extends AbstractLayout {

    private static final int STATE_COMPONENT_HEIGHT = 5;

    private static final boolean DEBUG_TIMELINES = false;

    @Override
    protected void doLayout(Container parent) {
        TimelineContainer container = (TimelineContainer) parent;

        Dimension size = getRealLayoutSize(container);
        TimelineModel model = container.getModel();

        LongRangeNormalizer normalizer = new LongRangeNormalizer(model.getRange());

        normalizer.setMinNormalized(0);
        normalizer.setMaxNormalized(size.width);

        if (DEBUG_TIMELINES) System.err.print(container.getName());

        for (RangeComponent rangeComponent : container) {

            RangedTimelineProbe info = rangeComponent.getInfo();
            Range<Long> range = info.getRange();
            int x = (int) normalizer.getValueNormalized(range.getMin());
            int width = (int) normalizer.getValueNormalized(range.getMax()) - x + 1;

            rangeComponent.setBounds(x, 0, width, 5);

            if (DEBUG_TIMELINES) System.err.print(" [" + range.getMin() +
                                                  " - " + range.getMax() + "]");
        }

        if (DEBUG_TIMELINES) System.err.println("");
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return getRealLayoutSize(parent);
    }

    public Dimension getRealLayoutSize(Container parent) {
        TimelineContainer container = (TimelineContainer) parent;

        TimelineModel model = container.getModel();

        Range<Long> range = model.getRange();

        double length = range.getMax() - range.getMin();
        double multiplier = model.getMagnificationRatio();

        return new Dimension((int) Math.round(length * multiplier),
                             STATE_COMPONENT_HEIGHT);
    }
}
