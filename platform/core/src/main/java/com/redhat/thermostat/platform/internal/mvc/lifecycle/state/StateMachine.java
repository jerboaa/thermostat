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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.state;

import com.redhat.thermostat.beans.property.ObjectProperty;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.event.EventQueue;
import com.redhat.thermostat.platform.mvc.MVCProvider;

/**
 */
public class StateMachine {

    private Context context;
    private ObjectProperty<State> stateProperty;

    public StateMachine(MVCProvider provider, Platform platform,
                        PlatformServiceRegistrar registrar,
                        EventQueue eventQueue) {

        context = new Context();
        context.platform = platform;
        context.provider = provider;
        context.dispatcher = new StateMachineTransitionDispatcher(eventQueue, this);
        context.registrar = registrar;
        stateProperty = new ObjectProperty<>(State.INVALID);
    }

    public void start() {
        setState(State.CREATE);
    }

    public ObjectProperty<State> stateProperty() {
        return stateProperty;
    }

    boolean canGoToState(State state) {
        return stateProperty.get().canGoToState(state);
    }

    void setStateInternal(State newState) {
        stateProperty.set(newState);
    }

    public void setState(State newState) {
        if (canGoToState(newState)) {
            setStateInternal(newState);
            stateProperty.get().execute(context);
        }
    }

    public void stop() {
        State currentState = stateProperty.get();

        if (currentState.equals(State.INVALID) || currentState.equals(State.DESTROY)) {
            // do nothing
            return;
        }

        if (canGoToState(State.DESTROY)) {
            setState(State.DESTROY);

        } else if (stateProperty.get().equals(State.START)) {
            setState(State.STOP);
            setState(State.DESTROY);
        }
    }
}
