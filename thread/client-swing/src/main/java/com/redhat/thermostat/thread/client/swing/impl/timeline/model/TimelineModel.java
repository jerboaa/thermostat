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

package com.redhat.thermostat.thread.client.swing.impl.timeline.model;

import com.redhat.thermostat.common.model.Range;
import javax.swing.BoundedRangeModel;
import javax.swing.event.EventListenerList;

/**
 *
 */
public class TimelineModel {

    public static final double DEFAULT_RATIO = 1./100.;

    private EventListenerList listenerList;

    private double magnificationRatio;

    private Range<Long> range;
    private BoundedRangeModel scrollBarModel;

    private TimelineModel model;

    public TimelineModel() {
        this.listenerList = new EventListenerList();
        magnificationRatio = DEFAULT_RATIO;
    }

    public Range<Long> getRange() {
        return range;
    }

    public void setRange(Range<Long> range) {
        this.range = range;
        fireRangeChangeEvent();
    }

    public void setScrollBarModel(BoundedRangeModel scrollBarModel) {
        this.scrollBarModel = scrollBarModel;
    }

    public BoundedRangeModel getScrollBarModel() {
        return scrollBarModel;
    }

    public void addRangeChangeListener(RangeChangeListener listener) {
        listenerList.add(RangeChangeListener.class, listener);
    }

    public void addRatioChangeListener(RatioChangeListener listener) {
        listenerList.add(RatioChangeListener.class, listener);
    }

    public void removeRatioChangeListener(RatioChangeListener listener) {
        listenerList.remove(RatioChangeListener.class, listener);
    }

    public void removeRangeChangeListener(RangeChangeListener listener) {
        listenerList.remove(RangeChangeListener.class, listener);
    }

    public double getMagnificationRatio() {
        return magnificationRatio;
    }

    public void setMagnificationRatio(double magnificationRatio) {
        this.magnificationRatio = magnificationRatio;
        fireRatioChangeEvent();
    }

    private void fireRatioChangeEvent() {
        Object[] listeners = listenerList.getListenerList();
        RatioChangeEvent event =
                new RatioChangeEvent(this, magnificationRatio);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RatioChangeListener.class) {
                ((RatioChangeListener) listeners[i + 1]).ratioChanged(event);
            }
        }
    }

    private void fireRangeChangeEvent() {
        Object[] listeners = listenerList.getListenerList();
        RangeChangeEvent event =
                new RangeChangeEvent(this,
                                     new Range<>(range.getMin(),
                                                 range.getMax()));
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RangeChangeListener.class) {
                ((RangeChangeListener) listeners[i + 1]).rangeChanged(event);
            }
        }
    }
}
