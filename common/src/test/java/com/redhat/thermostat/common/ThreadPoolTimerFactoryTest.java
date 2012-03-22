/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.Timer.SchedulingType;

public class ThreadPoolTimerFactoryTest {

    private Timer timer;

    @Before
    public void setUp() {
        ThreadPoolTimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        timer = timerFactory.createTimer();
    }

    @After
    public void tearDown() {
        timer = null;
    }

    @Test
    public void testDefault() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.start();
        Thread.sleep(10);
        verify(action).run();
    }

    @Test
    public void testNullAction() {
        timer.setAction(null);
        timer.start();
        // Good when no NPE is thrown.
    }

    @Test
    public void testDefaultWithDelay() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.setDelay(50);
        timer.start();
        Thread.sleep(10);
        verify(action, never()).run();
        Thread.sleep(50);
        verify(action).run();
        Thread.sleep(50);
        verify(action).run();
    }

    @Test
    public void testTimeUnitSecond() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.setDelay(1);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.start();
        Thread.sleep(100);
        verify(action, never()).run();
        Thread.sleep(1000);
        verify(action).run();
        Thread.sleep(50);
        verify(action).run();
    }

    @Test(expected=NullPointerException.class)
    public void testNullType() throws InterruptedException {
        timer.setSchedulingType(null);
    }

    @Test
    public void testWithFixedRate() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(45);
                return null;
            }
            
        }).when(action).run();
        timer.setAction(action);
        timer.setDelay(50);
        timer.setPeriod(50);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.start();
        Thread.sleep(10);
        verify(action, never()).run();
        Thread.sleep(50);
        verify(action).run();
        Thread.sleep(50);
        verify(action, times(2)).run();
        timer.stop();
        Thread.sleep(50);
        verify(action, times(2)).run();
    }

    @Test
    public void testStopWithoutStart() {
        timer.stop();
        // Good when no exception is thrown.
    }

    @Test
    public void testWithFixedDelay() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(50);
                return null;
            }
            
        }).when(action).run();
        timer.setAction(action);
        timer.setDelay(50);
        timer.setPeriod(50);
        timer.setSchedulingType(SchedulingType.FIXED_DELAY);
        timer.start();
        Thread.sleep(10);
        verify(action, never()).run();
        Thread.sleep(50);
        verify(action).run();
        Thread.sleep(50);
        verify(action).run();
        Thread.sleep(50);
        verify(action, times(2)).run();
        timer.stop();
        Thread.sleep(50);
        verify(action, times(2)).run();
    }
}
