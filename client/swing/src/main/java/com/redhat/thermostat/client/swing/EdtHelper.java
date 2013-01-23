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

package com.redhat.thermostat.client.swing;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

/**
 * Allows operations to be performed consistently on the Swing EDT
 * irrespective of whether the caller is running on the EDT or not.
 * 
 * @see SwingUtilities#invokeAndWait(Runnable)
 * @see SwingUtilities#invokeLater(Runnable)
 */
public class EdtHelper {

    @SuppressWarnings("serial")
    private static class CallableException extends RuntimeException {

        private CallableException(Exception ex) {
            super(ex);
        }
        
    }

    private static class CallableWrapper<T> implements Runnable {

        private Callable<T> callable;
        private T result;

        private CallableWrapper(Callable<T> c) {
            callable = c;
        }

        @Override
        public void run() {
            try {
                result = callable.call();
            } catch (Exception ex) {
                throw new CallableException(ex);
            }
        }

        private T getResult() {
            return result;
        }
    }

    /**
     * Invoke the supplied {@link Runnable} on the EDT.
     * @param r encapsulates the code to run
     * @throws InvocationTargetException encapsulates the actual exception
     * that occurs when executing this code.
     * @throws InterruptedException
     */
    public void callAndWait(Runnable r) throws InvocationTargetException, InterruptedException {
        if (EventQueue.isDispatchThread()) {
            try {
                r.run();
            } catch (Exception ex) {
                throw new InvocationTargetException(ex);
            }
        } else {
            EventQueue.invokeAndWait(r);
        }
    }

    /**
     * Invokes the supplied {@link Callable} on the EDT, waits until it is
     * finished execution and returns the result of invoking {@link Callable#call()}.
     * @param c encapsulates the code to execute
     * @return the result produce by c
     * @throws InvocationTargetException indicates an exception occurred when executing the callable
     * @throws InterruptedException
     */
    public <T> T callAndWait(Callable<T> c) throws InvocationTargetException, InterruptedException {
        CallableWrapper<T> w = new CallableWrapper<>(c);
        callAndWait(w);
        return w.getResult();
    }

}

