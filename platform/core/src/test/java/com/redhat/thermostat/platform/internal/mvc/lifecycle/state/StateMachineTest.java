/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.event.EventQueue;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers.MVCExtensionLinker;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.mock;

/**
 */
public class StateMachineTest {

    @Test
    public void stateMachine_canGoToState() {
        MVCProvider provider = mock(MVCProvider.class);
        Platform platform = mock(Platform.class);
        EventQueue eventQueue = mock(EventQueue.class);
        PlatformServiceRegistrar registrar = mock(PlatformServiceRegistrar.class);
        MVCExtensionLinker linker = mock(MVCExtensionLinker.class);

        StateMachine stateMachine =
                new StateMachine(provider, platform, registrar, eventQueue, linker);

        assertTrue(stateMachine.canGoToState(State.CREATE));

        assertFalse(stateMachine.canGoToState(State.INVALID));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.START));
        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.DESTROY));

        stateMachine.setStateInternal(State.CREATE);
        assertTrue(stateMachine.canGoToState(State.INIT));
        assertTrue(stateMachine.canGoToState(State.DESTROY));

        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.START));
        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.INVALID));

        stateMachine.setStateInternal(State.INIT);
        assertTrue(stateMachine.canGoToState(State.START));
        assertTrue(stateMachine.canGoToState(State.DESTROY));

        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.INVALID));

        stateMachine.setStateInternal(State.START);
        assertTrue(stateMachine.canGoToState(State.STOP));

        assertFalse(stateMachine.canGoToState(State.START));
        assertFalse(stateMachine.canGoToState(State.DESTROY));
        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.INVALID));

        stateMachine.setStateInternal(State.STOP);
        assertTrue(stateMachine.canGoToState(State.START));
        assertTrue(stateMachine.canGoToState(State.DESTROY));

        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.INVALID));

        stateMachine.setStateInternal(State.STOP);
        assertTrue(stateMachine.canGoToState(State.START));
        assertTrue(stateMachine.canGoToState(State.DESTROY));

        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.INVALID));

        stateMachine.setStateInternal(State.DESTROY);
        assertTrue(stateMachine.canGoToState(State.INVALID));

        assertFalse(stateMachine.canGoToState(State.START));
        assertFalse(stateMachine.canGoToState(State.STOP));
        assertFalse(stateMachine.canGoToState(State.CREATE));
        assertFalse(stateMachine.canGoToState(State.INIT));
        assertFalse(stateMachine.canGoToState(State.DESTROY));
    }
}
