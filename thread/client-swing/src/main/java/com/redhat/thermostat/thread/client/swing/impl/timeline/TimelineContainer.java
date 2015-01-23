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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangeChangeEvent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangeChangeListener;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RatioChangeEvent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RatioChangeListener;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class TimelineContainer extends ContentPane implements Iterable<RangeComponent> {

    private List<RangeComponent> rangeComponents;
    private TimelineModel model;

    public TimelineContainer(TimelineModel model) {
        rangeComponents  = new ArrayList<>();

        setOpaque(false);
        setLayout(new RangeComponentLayoutManager());

        setModel(model);
    }

    public Component add(RangeComponent comp) {
        rangeComponents.add(comp);
        return super.add(comp);
    }

    @Override
    public Iterator<RangeComponent> iterator() {
        return rangeComponents.iterator();
    }

    public TimelineModel getModel() {
        return model;
    }

    public void setModel(TimelineModel model) {
        this.model = model;
        model.addRangeChangeListener(new RangeChangeListener() {
            @Override
            public void rangeChanged(RangeChangeEvent event) {
                revalidate();
                repaint();
            }
        });
        model.addRatioChangeListener(new RatioChangeListener() {
            @Override
            public void ratioChanged(RatioChangeEvent event) {
                revalidate();
                repaint();
            }
        });
    }

    public RangeComponent getLastRangeComponent() {
        return rangeComponents.isEmpty() ? null :
               rangeComponents.get(rangeComponents.size() - 1);
    }
}
