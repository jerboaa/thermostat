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

package com.redhat.thermostat.event;

import com.redhat.thermostat.platform.event.EventQueueDispatcher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class EventNotifierTest {

    private class TestEvent extends Event {

        public TestEvent(Object source) {
            super(source);
        }

        String getName() {
            return (String) source;
        }
    }

    private class DispatchImmediately implements EventQueueDispatcher {
        @Override
        public void dispatch(Runnable runnable) {
            runnable.run();
        }
    }

    @Test
    public void testFireEvent() throws Exception {

        final TestEvent[] result = new TestEvent[1];
        final int[] calls = new int[1];
        EventNotifier<TestEvent> notifier = new EventNotifier<>(new DispatchImmediately());
        notifier.addEventHandler(new EventHandler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                result[0] = event;
                calls[0]++;
            }
        });

        TestEvent event = new TestEvent("test");
        notifier.fireEvent(event);

        assertEquals("test", result[0].getName());
        assertEquals(1, calls[0]);
    }

    @Test
    public void testMultipleEventHandlers() throws Exception {

        final TestEvent[] result = new TestEvent[2];
        final int[] calls = new int[2];

        EventNotifier<TestEvent> notifier = new EventNotifier<>(new DispatchImmediately());
        notifier.addEventHandler(new EventHandler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                result[0] = event;
                calls[0]++;
            }
        });

        notifier.addEventHandler(new EventHandler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                result[1] = event;
                calls[1]++;
            }
        });

        TestEvent event = new TestEvent("test");
        notifier.fireEvent(event);

        assertEquals("test", result[0].getName());
        assertEquals("test", result[1].getName());

        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
    }

    @Test
    public void testMultipleEventHandlersWithConsume() throws Exception {

        final TestEvent[] result = new TestEvent[2];
        final int[] calls = new int[2];

        EventNotifier<TestEvent> notifier = new EventNotifier<>(new DispatchImmediately());
        notifier.addEventHandler(new EventHandler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                result[0] = event;
                calls[0]++;

                event.consume();
            }
        });

        notifier.addEventHandler(new EventHandler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                result[1] = event;
                calls[1]++;
            }
        });

        TestEvent event = new TestEvent("test");
        notifier.fireEvent(event);

        assertEquals("test", result[0].getName());
        assertEquals(null, result[1]);

        assertEquals(1, calls[0]);
        assertEquals(0, calls[1]);
    }
}