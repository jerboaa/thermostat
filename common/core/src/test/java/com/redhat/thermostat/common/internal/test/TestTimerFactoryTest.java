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

package com.redhat.thermostat.common.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Timer;

public class TestTimerFactoryTest {

    private TestTimerFactory timerFactory;
    private Timer timer;

    @Before
    public void setUp() {
        timerFactory = new TestTimerFactory();
        timer = timerFactory.createTimer();
    }

    @After
    public void tearDown() {
        timer = null;
        timerFactory = null;
    }

    @Test
    public void testStart() {
        assertFalse(timerFactory.isActive());
        timer.start();
        assertTrue(timerFactory.isActive());
    }

    @Test
    public void testStop() {
        assertFalse(timerFactory.isActive());
        timer.start();
        assertTrue(timerFactory.isActive());
        timer.stop();
        assertFalse(timerFactory.isActive());
    }

    @Test
    public void testSetAction() {
        final boolean[] run = new boolean[1];
        Runnable action = new Runnable() {
            @Override
            public void run() {
                run[0] = true;
            }
        };
        timer.setAction(action);
        assertSame(action, timerFactory.getAction());
        timerFactory.getAction().run();
        assertTrue(run[0]);
    }

    @Test
    public void testSetInitialDelay() {
        timer.setInitialDelay(123);
        assertEquals(123l, timerFactory.getInitialDelay());
    }

    @Test
    public void testSetDelay() {
        timer.setDelay(123);
        assertEquals(123l, timerFactory.getDelay());
    }

    @Test
    public void testSetSchedulingType() {
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        assertEquals(Timer.SchedulingType.FIXED_RATE, timerFactory.getSchedulingType());
    }

    @Test
    public void testSetTimeUnit() {
        timer.setTimeUnit(TimeUnit.HOURS);
        assertEquals(TimeUnit.HOURS, timerFactory.getTimeUnit());
    }

    @Test
    public void testShutdown() {
        assertFalse(timerFactory.isShutdown());
        timerFactory.shutdown();
        assertTrue(timerFactory.isShutdown());
    }
}

