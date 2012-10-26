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

package com.redhat.thermostat.client.swing.components;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EdtHelperTest {

    private class ExceptionCallable implements Callable<Object>  {

        @Override
        public Object call() throws Exception {
            throw new Exception("fluff");
        }
        
    }

    private class ResultCallable implements Callable<Object> {
    
        private Object result;
        private ResultCallable(Object r) {
            result = r;
        }
        @Override
        public Object call() throws Exception {
            // By waiting here, we make sure the EDTHelper actually waits for the call.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (EventQueue.isDispatchThread()) {
                calledOnEDT = true;
            }
            return result;
        }
        
    }

    private class TestRunnable implements Runnable {

        @Override
        public void run() {
            // By waiting here, we make sure the EDTHelper actually waits for the call.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (EventQueue.isDispatchThread()) {
                calledOnEDT = true;
            }
        }
        
    }

    private volatile boolean calledOnEDT;

    @Before
    public void setUp() {
        calledOnEDT = false;
    }

    @After
    public void tearDown() {
        calledOnEDT = false;
    }

    @Test
    public void testCallRunnableFromNonEDT() throws InvocationTargetException, InterruptedException {
        Runnable r = new TestRunnable();
        new EdtHelper().callAndWait(r);
        assertTrue(calledOnEDT);
    }

    @Test
    public void testCallRunnableFromEDT() throws InvocationTargetException, InterruptedException {
        final Runnable r = new TestRunnable();
        SwingUtilities.invokeAndWait(new Runnable() {
            
            @Override
            public void run() {
                try {
                    new EdtHelper().callAndWait(r);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(calledOnEDT);
    }

    @Test
    public void testCallCallableFromNoEDT() throws InvocationTargetException, InterruptedException {
        final Object expected = new Object();
        Callable<Object> c = new ResultCallable(expected);
        Object result = new EdtHelper().callAndWait(c);
        assertTrue(calledOnEDT);
        assertSame(expected, result);
    }

    @Test
    public void testCallCallableFromEDT() throws InvocationTargetException, InterruptedException {
        final Object expected = new Object();
        final Callable<Object> c = new ResultCallable(expected);
        final Object[] result = new Object[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            
            @Override
            public void run() {
                try {
                    result[0] = new EdtHelper().callAndWait(c);
                } catch (InvocationTargetException | InterruptedException e) {
                    throw new RuntimeException();
                }
            }
        });
        assertTrue(calledOnEDT);
        assertSame(expected, result[0]);
    }

    @Test(expected=InvocationTargetException.class)
    public void testCallCallableFromNoEDTThrowingException() throws InvocationTargetException, InterruptedException {
        Callable<Object> c = new ExceptionCallable();
        new EdtHelper().callAndWait(c);
    }

    @Test
    public void testCallCallableFromEDTThrowingException() throws InvocationTargetException, InterruptedException {
        final boolean[] exceptionThrown = new boolean[1];
        final Callable<Object> c = new ExceptionCallable();
        SwingUtilities.invokeAndWait(new Runnable() {
            
            @Override
            public void run() {
                try {
                    new EdtHelper().callAndWait(c);
                } catch (InvocationTargetException e) {
                    exceptionThrown[0] = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(exceptionThrown[0]);
    }
}
