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

package com.redhat.thermostat.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTimerFactory implements TimerFactory {

    public ThreadPoolTimerFactory(int poolSize) {
        this(poolSize, Thread.currentThread().getThreadGroup());
    }

    ThreadPoolTimerFactory(int poolSize, final ThreadGroup group) {
        timerThreadPool = Executors.newScheduledThreadPool(poolSize, new ThreadFactory() {
            
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(group, r);
            }
        });
    }

    private class ThreadPoolTimer implements Timer {

        private Runnable action;

        private long delay;

        private long period;

        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        private SchedulingType schedulingType = SchedulingType.ONCE;

        private ScheduledFuture<?> timerTask;

        @Override
        public void start() {
            if (action != null) {
                startScheduling();
            }          
        }

        private void startScheduling() {
            switch (schedulingType) {
            case FIXED_RATE:
                timerTask = timerThreadPool.scheduleAtFixedRate(action, delay, period, timeUnit);
                break;
            case FIXED_DELAY:
                timerTask = timerThreadPool.scheduleWithFixedDelay(action, delay, period, timeUnit);
                break;
            case ONCE:
            default:
                timerTask = timerThreadPool.schedule(action, delay, timeUnit);
            }
        }

        @Override
        public void stop() {
            if (timerTask != null) {
                timerTask.cancel(false);
            }
        }

        @Override
        public void setAction(Runnable action) {
            this.action = action;
        }

        @Override
        public void setInitialDelay(long delay) {
            this.delay = delay;
        }

        @Override
        public void setDelay(long period) {
            this.period = period;
        }

        @Override
        public void setSchedulingType(SchedulingType schedulingType) {
            if (schedulingType == null) {
                throw new NullPointerException();
            }
            this.schedulingType = schedulingType;
        }

        @Override
        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }
        
    }

    private ScheduledExecutorService timerThreadPool;

    @Override
    public Timer createTimer() {
        return new ThreadPoolTimer();
    }

    @Override
    public void shutdown() {
        timerThreadPool.shutdown();
    }

}

