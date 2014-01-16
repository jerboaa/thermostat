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

package com.redhat.thermostat.client.swing.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Test;

import com.redhat.thermostat.client.swing.components.experimental.EventTimelineDataChangeListener;
import com.redhat.thermostat.client.swing.components.experimental.EventTimelineModel;
import com.redhat.thermostat.client.swing.components.experimental.EventTimelineRangeChangeListener;
import com.redhat.thermostat.client.swing.components.experimental.EventTimelineModel.Event;
import com.redhat.thermostat.common.model.Range;

public class EventTimelineModelTest {

    @Test
    public void testEvent() {
        EventTimelineModel model = new EventTimelineModel();
        assertEquals(0, model.getEvents().size());

        model.addEvent(0, "event1");

        assertEquals(1, model.getEvents().size());
    }

    @Test
    public void testAddEventObjects() {
        EventTimelineModel model = new EventTimelineModel();
        assertEquals(0, model.getEvents().size());

        Event event1 = new EventTimelineModel.Event(0, "event1");
        Event event2 = new EventTimelineModel.Event(2, "event2");
        model.addEvent(event1);
        model.addEvent(event2);

        assertEquals(Arrays.asList(event1, event2), model.getEvents());
    }

    @Test
    public void testGetTotalRange() {
        long START = 0;
        long END = 2;

        EventTimelineModel model = new EventTimelineModel();

        Event event1 = new EventTimelineModel.Event(START, "event1");
        Event event2 = new EventTimelineModel.Event(END, "event2");
        model.addEvent(event1);
        model.addEvent(event2);

        assertTrue(model.getTotalRange().getMin() <= START);
        assertTrue(model.getTotalRange().getMax() >= END);
    }

    @Test
    public void testDataListeners() {
        long START = 0;
        long END = 2;

        EventTimelineDataChangeListener listener = mock(EventTimelineDataChangeListener.class);

        EventTimelineModel model = new EventTimelineModel();
        model.addDataChangeListener(listener);

        Event event1 = new EventTimelineModel.Event(START, "event1");
        Event event2 = new EventTimelineModel.Event(END, "event2");
        model.addEvent(event1);
        model.addEvent(event2);

        verify(listener, times(2)).dataChanged();
    }

    @Test
    public void testRangeListeners() {
        long START = 0;
        long END = 20000000;

        EventTimelineRangeChangeListener listener = mock(EventTimelineRangeChangeListener.class);

        EventTimelineModel model = new EventTimelineModel();
        model.addRangeChangeListener(listener);

        Event event1 = new EventTimelineModel.Event(START, "event1");
        Event event2 = new EventTimelineModel.Event(END, "event2");

        model.addEvent(event1);
        model.addEvent(event2);

        verify(listener, times(2)).rangeChanged(isA(Range.class), isA(Range.class));
    }
}

