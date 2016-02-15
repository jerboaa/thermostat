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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Version;

/**
 * Convenience {@link Backend} class for implementations that will take some
 * action at the Host or system level on a regular interval.  Simply
 * extend this class, implement any missing methods, and register one or
 * more {@link HostPollingAction} implementations during instantiation.
 */
public abstract class HostPollingBackend extends PollingBackend {

    private final Set<HostPollingAction> actions;

    public HostPollingBackend(String name, String description,
            String vendor, Version version, ScheduledExecutorService executor) {
        super(name, description, vendor, version, executor);
        actions = new CopyOnWriteArraySet<>();
    }

    final void doScheduledActions() {
        for (HostPollingAction action : actions) {
            action.run();
        }
    }

    /**
     * Register an action to be performed at each polling interval.  It is
     * recommended that implementations register all such actions during
     * instantiation.
     */
    protected final void registerAction(HostPollingAction action) {
        actions.add(action);
    }

    /**
     * Unregister an action so that it will no longer be performed at each
     * polling interval.  If no such action has been registered, this
     * method has no effect.  Depending on thread timing issues, the action
     * may be performed once even after this method has been called.
     */
    protected final void unregisterAction(HostPollingAction action) {
        actions.remove(action);
    }

    @Override
    public void setObserveNewJvm(boolean newValue) {
        throw new NotImplementedException("This backend does not observe jvms!");
    }

    // Intentionally final do-nothing.
    final void preActivate() {};
    final void postDeactivate() {};
}
