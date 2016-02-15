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

package com.redhat.thermostat.backend;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.Version;

/*
 * Convenience {@link Backend} class for implementations that will take action
 * on a fixed interval.  This is package private, and plugins should instead
 * extend child classes {@link HostPollingBackend}
 * or {@link VmPollingBackend} as appropriate.
 */
abstract class PollingBackend extends BaseBackend {

    static final long DEFAULT_INTERVAL = 1000; // TODO make this configurable.

    private ScheduledExecutorService executor;
    private boolean isActive;

    PollingBackend(String name, String description, String vendor,
            Version version,
            ScheduledExecutorService executor) {
        super(name, description, vendor,
              version.getVersionNumber(), true);
        this.executor = executor;
    }

    @Override
    public final synchronized boolean activate() {
        if (!isActive) {
            preActivate();
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doScheduledActions();
                }
            }, 0, DEFAULT_INTERVAL, TimeUnit.MILLISECONDS);

            isActive = true;
        }
        return isActive;
    }

    @Override
    public final synchronized boolean deactivate() {
        if (isActive) {
            executor.shutdown();
            postDeactivate();
            isActive = false;
        }
        return !isActive;
    }

    @Override
    public final boolean isActive() {
        return isActive;
    }

    // Test hook.
    final void setActive(boolean active) {
        isActive = active;
    }

    // Give child classes a chance to specify what should happen at each polling interval.
    abstract void doScheduledActions();

    // An opportunity for child classes to do some setup upon activation.
    // Will execute before any actions are scheduled.
    void preActivate() {}

    // An opportunity for child classes to do some cleanup upon deactivation.
    // Will execute after all actions are unscheduled.
    void postDeactivate() {}

}
