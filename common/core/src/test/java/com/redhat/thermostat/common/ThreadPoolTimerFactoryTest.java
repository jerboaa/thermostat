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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.redhat.thermostat.test.Bug;

public class ThreadPoolTimerFactoryTest {

    private static final long DELAY = 200;

    private Timer timer;

    private ThreadGroup threadGroup;

    private TimerFactory timerFactory;

    @Before
    public void setUp() {
        threadGroup = new ThreadGroup("test");
        timerFactory = new ThreadPoolTimerFactory(1, threadGroup);
        timer = timerFactory.createTimer();
    }

    @After
    public void tearDown() {
        
        timer = null;
        timerFactory.shutdown();
        timerFactory = null;
        threadGroup = null;
    }

    @Test
    public void testDefault() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.start();
        Thread.sleep(DELAY / 2);
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
        timer.setInitialDelay(DELAY);
        timer.start();
        Thread.sleep(DELAY / 2);
        verify(action, never()).run();
        Thread.sleep(DELAY);
        verify(action).run();
        Thread.sleep(DELAY);
        verify(action).run();
    }

    @Test
    public void testTimeUnitSecond() throws InterruptedException {
        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.setInitialDelay(1);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.start();
        Thread.sleep(500);
        verify(action, never()).run();
        Thread.sleep(1000);
        verify(action).run();
        Thread.sleep(1000);
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
                // Wait a little less than delay to
                // 1. Verify more easily that the duration of the task does not push back the activation of next task.
                // 2. Not congest the timer thread (which would happen if we take >= DELAY).
                Thread.sleep(DELAY * 8 / 10 );
                return null;
            }
            
        }).when(action).run();
        timer.setAction(action);
        timer.setInitialDelay(DELAY);
        timer.setDelay(DELAY);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.start();
        Thread.sleep(DELAY / 2);
        verify(action, never()).run();
        Thread.sleep(DELAY);
        verify(action).run();
        Thread.sleep(DELAY);
        verify(action, times(2)).run();
        timer.stop();
        Thread.sleep(DELAY);
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
                Thread.sleep(DELAY / 2);
                return null;
            }
            
        }).when(action).run();
        timer.setAction(action);
        timer.setInitialDelay(DELAY);
        timer.setDelay(DELAY);
        timer.setSchedulingType(SchedulingType.FIXED_DELAY);
        timer.start();
        Thread.sleep(DELAY / 2);
        verify(action, never()).run();
        Thread.sleep(DELAY);
        verify(action).run();
        Thread.sleep(DELAY / 2);
        verify(action).run();
        Thread.sleep(DELAY);
        verify(action, times(2)).run();
        Thread.sleep(DELAY / 2);
        verify(action, times(2)).run();
        timer.stop();
        Thread.sleep(DELAY);
        verify(action, times(2)).run();
    }

    @Bug(id="957",
         summary="Thermostat GUI doesn't exit when closed, needs killing",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=957")
    @Test
    public void verifyShutdownKillsThreads() throws InterruptedException {

        Runnable action = mock(Runnable.class);
        timer.setAction(action);
        timer.setInitialDelay(DELAY / 2);
        timer.start();

        assertTrue(threadGroup.activeCount() > 0);

        timerFactory.shutdown();

        Thread.sleep(DELAY);

        assertEquals(0, threadGroup.activeCount());
    }
}

