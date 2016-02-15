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

package com.redhat.thermostat.platform.event;

import com.redhat.thermostat.common.utils.LoggingUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple Event Queue implementation that queues runnable for execution
 * in a single thread.
 */
public class EventQueue {

    private static final Logger logger  = LoggingUtils.getLogger(EventQueue.class);

    private volatile boolean isRunning;
    private BlockingQueue<Runnable> eventQueue;
    private Thread eventQueueThread;

    private EventQueueDispatcher dispatcher;

    private String name;

    public EventQueue(String name) {
        this(name, null);
    }

    public EventQueue(String name, EventQueueDispatcher dispatcher) {
        eventQueue = new LinkedBlockingQueue<>();
        this.name = name;
        this.dispatcher = dispatcher;
    }

    public void runLater(Runnable runnable) {
        try {
            eventQueue.put(runnable);

        } catch (InterruptedException ignored) {}
    }

    public void start() {
        if (isRunning) {
            return;
        }

        if (dispatcher == null) {
            dispatcher = new DispatchImmediately();
        }

        isRunning = true;

        eventQueueThread = new Thread(new EventQueueThread());
        eventQueueThread.setName(name);
        eventQueueThread.setDaemon(true);

        eventQueueThread.start();
    }

    public void shutdown() {
        try {
            eventQueue.put(new Runnable() {
                @Override
                public void run() {
                    isRunning = false;
                }
            });

        } catch (InterruptedException ignored) {}
    }

    public boolean isEventDispatchThread() {
        return Thread.currentThread() == eventQueueThread;
    }

    public boolean isRunning() {
        return isRunning;
    }

    class EventQueueThread implements Runnable {
        @Override
        public void run() {
            do {
                try {
                    Runnable runnable = eventQueue.take();
                    dispatcher.dispatch(runnable);

                } catch (Throwable e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Exception while dispatching event", e);
                    }

                    Thread dispatcher = Thread.currentThread();
                    dispatcher.getUncaughtExceptionHandler().uncaughtException(dispatcher, e);
                }

            } while (isRunning);
        }
    }

    private class DispatchImmediately implements EventQueueDispatcher {
        @Override
        public void dispatch(Runnable runnable) {
            runnable.run();
        }
    }
}
