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

package com.redhat.thermostat.test;

import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;

public class TestTimerFactory implements TimerFactory {

    private class TestTimer implements Timer {

        @Override
        public void start() {
            isActive = true;
        }

        @Override
        public void stop() {
            isActive = false;
        }

        @Override
        public void setAction(Runnable action) {
            TestTimerFactory.this.action = action;
        }

        @Override
        public void setInitialDelay(long initialDelay) {
            TestTimerFactory.this.initialDelay = initialDelay;
        }

        @Override
        public void setDelay(long delay) {
            TestTimerFactory.this.delay = delay;
        }

        @Override
        public void setSchedulingType(SchedulingType schedulingType) {
            TestTimerFactory.this.schedulingType = schedulingType;
        }

        @Override
        public void setTimeUnit(TimeUnit timeUnit) {
            TestTimerFactory.this.timeUnit = timeUnit;
        }
        
    }

    private Timer timer = new TestTimer();

    private Runnable action;

    private long initialDelay;

    private long delay;

    private Timer.SchedulingType schedulingType;

    private TimeUnit timeUnit;

    private boolean isActive;

    private boolean shutdown;

    public long getInitialDelay() {
        return initialDelay;
    }

    public long getDelay() {
        return delay;
    }

    public Timer.SchedulingType getSchedulingType() {
        return schedulingType;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Runnable getAction() {
        return action;
    }

    @Override
    public Timer createTimer() {
        return timer;
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isShutdown() {
        return shutdown;
    }

}

