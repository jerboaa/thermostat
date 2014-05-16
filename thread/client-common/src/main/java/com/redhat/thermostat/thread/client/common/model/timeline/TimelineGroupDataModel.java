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

package com.redhat.thermostat.thread.client.common.model.timeline;

import com.redhat.thermostat.common.model.Range;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 */
public class TimelineGroupDataModel {

    public enum RangeChangeProperty {
        TOTAL_RANGE,
        PAGE_RANGE
    }

    private Range<Long> totalRange;
    private Range<Long> pageRange;

    private PropertyChangeSupport propertyChangeSupport;

    public TimelineGroupDataModel() {
        this(new Range<>(0L, Long.MAX_VALUE), new Range<Long>(0L, Long.MAX_VALUE));
    }

    public TimelineGroupDataModel(Range<Long> totalRange, Range<Long> pageRange) {
        this.totalRange = totalRange;
        this.pageRange = pageRange;
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public Range<Long> getTotalRange() {
        return totalRange;
    }

    public void setTotalRange(Range<Long> totalRange) {
        Range<Long> old = this.totalRange;
        this.totalRange = totalRange;
        fireRangeChangeEvent(RangeChangeProperty.TOTAL_RANGE, old, this.totalRange);
    }

    public Range<Long> getPageRange() {
        return pageRange;
    }

    public void setPageRange(Range<Long> pageRange) {
        Range<Long> old = this.pageRange;
        this.pageRange = pageRange;
        fireRangeChangeEvent(RangeChangeProperty.PAGE_RANGE, old, this.pageRange);
    }

    public void addPropertyChangeListener(RangeChangeProperty property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
    }

    private void fireRangeChangeEvent(RangeChangeProperty property, Range<Long> oldRange, Range<Long> newRange) {
        propertyChangeSupport.firePropertyChange(property.name(), oldRange, newRange);
    }
}
