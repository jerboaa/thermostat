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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Convenience {@link Backend} class for implementations that will take some
 * action for each monitored JVM process on a regular interval.  Simply
 * extend this class, implement any missing methods, and register one or
 * more {@link VmPollingAction} implementations during instantiation.
 */
public abstract class VmPollingBackend extends PollingBackend implements VmStatusListener {

    private final Set<VmPollingAction> actions;
    private final Map<String, Pair<Integer, VmPollingAction>> badActions;
    private final Map<Integer, String> pidsToMonitor = new ConcurrentHashMap<>();
    private final VmStatusListenerRegistrar registrar;
    private static final Logger logger = LoggingUtils.getLogger(VmPollingBackend.class);
    private static final int EXCEPTIONS_THRESHOLD = 10;

    public VmPollingBackend(String name, String description,
            String vendor, Version version, ScheduledExecutorService executor,
            VmStatusListenerRegistrar registrar) {
        super(name, description, vendor, version, executor);
        this.registrar = registrar;
        this.actions = new CopyOnWriteArraySet<>();
        this.badActions = new HashMap<>();
    }

    @Override
    final void preActivate() {
        registrar.register(this);
    }

    @Override
    final void postDeactivate() {
        registrar.unregister(this);
    }

    @Override
    final void doScheduledActions() {
        for (Entry<Integer, String> entry : pidsToMonitor.entrySet()) {
            int pid = entry.getKey();
            String vmId = entry.getValue();
            for (VmPollingAction action : actions) {
                try {
                    action.run(vmId, pid);
                } catch (Throwable t) {
                    handleActionException(action, vmId);
                }
            }
        }
    }

    private synchronized void handleActionException(VmPollingAction action, String vmId) {
        final String actionName = action.getClass().getName();
        final String actionKey = actionName + vmId;
        Pair<Integer, VmPollingAction> actionPair = badActions.remove(actionKey);
        if (actionPair == null) {
            actionPair = new Pair<>(Integer.valueOf(1), action);
        }
        int exceptionsPerAction = actionPair.getFirst();
        if (exceptionsPerAction < EXCEPTIONS_THRESHOLD) {
            exceptionsPerAction++;
            logger.info(VmPollingAction.class.getSimpleName() + " " +
                    actionName + " threw an exception");
            badActions.put(actionKey, new Pair<>(exceptionsPerAction, action));
        } else {
            logger.fine("Removing " + actionName + " due to too many repeated exceptions.");
            unregisterAction(action);
        }
    }

    /**
     * Register an action to be performed at each polling interval.  It is
     * recommended that implementations register all such actions during
     * instantiation.
     */
    protected final void registerAction(VmPollingAction action) {
        actions.add(action);
    }

    /**
     * Unregister an action so that it will no longer be performed at each
     * polling interval.  If no such action has been registered, this
     * method has no effect.  Depending on thread timing issues, the action
     * may be performed once even after this method has been called.
     */
    protected final void unregisterAction(VmPollingAction action) {
        actions.remove(action);
    }

    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            if (getObserveNewJvm()) {
                pidsToMonitor.put(pid, vmId);
            } else {
                logger.log(Level.FINE, "skipping new vm " + pid);
            }
            break;
        case VM_STOPPED:
            pidsToMonitor.remove(pid);
            break;
        }
    }
}
