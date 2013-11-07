/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.model.Range;

/**
 * A time line with an total and a detail range.
 */
public class EventTimelineModel {

    private final List<EventTimelineRangeChangeListener> rangeListeners = new CopyOnWriteArrayList<>();
    private final List<EventTimelineDataChangeListener> dataListeners = new CopyOnWriteArrayList<>();

    private Range<Long> totalRange;
    private Range<Long> detailRange;;
    private List<Event> events = new ArrayList<>();

    private boolean isAdjusting = false;

    public Range<Long> getTotalRange() {
        return totalRange;
    }

    private void setTotalRange(Range<Long> newRange) {
        if (totalRange != null && totalRange.equals(newRange)) {
            return;
        }

        this.totalRange = newRange;
        fireRangeChanged();
    }

    public void addEvent(long eventTimeStamp, String description) {
        addEvent(new Event(eventTimeStamp, description));
    }

    public void addEvent(Event event) {
        long eventTimeStamp = event.getTimeStamp();

        if (totalRange == null) {
            // some heuristics to get sane initial ranges automagically
            setTotalRange(new Range<>(eventTimeStamp - TimeUnit.MINUTES.toMillis(1), eventTimeStamp + TimeUnit.MINUTES.toMillis(1)));
            setDetailRange(new Range<>(eventTimeStamp - TimeUnit.MINUTES.toMillis(1), eventTimeStamp + TimeUnit.MINUTES.toMillis(1)));
        } else {
            long delta = (long) ((totalRange.getMax() - totalRange.getMin()) * 0.1);

            if (totalRange.getMax() < eventTimeStamp + delta) {
                setTotalRange(new Range<>(totalRange.getMin(), eventTimeStamp + delta));
            } else if (totalRange.getMin() + delta > eventTimeStamp) {
                setTotalRange(new Range<>(eventTimeStamp - delta, totalRange.getMax()));
            }
        }

        events.add(event);
        fireEventDataChanged();
    }

    public List<Event> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }

    public void setDetailRange(Range<Long> range) {
        if (detailRange != null && detailRange.equals(range)) {
            return;
        }

        this.detailRange = range;

        fireRangeChanged();
    }

    public Range<Long> getDetailRange() {
        return detailRange;
    }

    public void addRangeChangeListener(EventTimelineRangeChangeListener listener) {
        rangeListeners.add(listener);
    }

    public void removeRangeChangeListener(EventTimelineRangeChangeListener listener) {
        rangeListeners.remove(listener);
    }

    private void fireRangeChanged() {
        for (EventTimelineRangeChangeListener listener : rangeListeners) {
            listener.rangeChanged(this.totalRange, this.detailRange);
        }
    }

    public void addDataChangeListener(EventTimelineDataChangeListener listener) {
        dataListeners.add(listener);
    }

    public void removeEventDataChangeListener(EventTimelineDataChangeListener listener) {
        dataListeners.remove(listener);
    }

    private void fireEventDataChanged() {
        for (EventTimelineDataChangeListener listener : dataListeners) {
            listener.dataChanged();
        }
    }

    public static class Event {

        private final long timeStamp;
        private final String description;

        public Event(long timeStamp, String description) {
            this.timeStamp = timeStamp;
            this.description = description;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public String getDescription() {
            return description;
        }
    }

}
