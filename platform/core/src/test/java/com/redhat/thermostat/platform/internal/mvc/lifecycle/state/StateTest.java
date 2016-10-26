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

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 */
public class StateTest {

    @Test
    public void testCorrectDispatchers() {
        assertTrue(State.INVALID.action instanceof Invalid);
        assertTrue(State.CREATE.action instanceof Create);
        assertTrue(State.INIT.action instanceof Init);
        assertTrue(State.START.action instanceof Start);
        assertTrue(State.STOP.action instanceof Stop);
        assertTrue(State.DESTROY.action instanceof Destroy);

        // so we know that we should re-check this test if we add a new state
        assertEquals(6, State.values().length);
    }

    @Test
    public void testCanGoToState_INVALID() {

        assertTrue(State.INVALID.canGoToState(State.CREATE));

        assertFalse(State.INVALID.canGoToState(State.INVALID));
        assertFalse(State.INVALID.canGoToState(State.INIT));
        assertFalse(State.INVALID.canGoToState(State.START));
        assertFalse(State.INVALID.canGoToState(State.STOP));
        assertFalse(State.INVALID.canGoToState(State.DESTROY));
    }

    @Test
    public void testCanGoToState_CREATE() {

        assertTrue(State.CREATE.canGoToState(State.INIT));
        assertTrue(State.CREATE.canGoToState(State.DESTROY));

        assertFalse(State.CREATE.canGoToState(State.CREATE));
        assertFalse(State.CREATE.canGoToState(State.START));
        assertFalse(State.CREATE.canGoToState(State.STOP));
        assertFalse(State.CREATE.canGoToState(State.INVALID));
    }

    @Test
    public void testCanGoToState_INIT() {

        assertTrue(State.INIT.canGoToState(State.START));
        assertTrue(State.INIT.canGoToState(State.DESTROY));

        assertFalse(State.INIT.canGoToState(State.CREATE));
        assertFalse(State.INIT.canGoToState(State.INIT));
        assertFalse(State.INIT.canGoToState(State.STOP));
        assertFalse(State.INIT.canGoToState(State.INVALID));
    }

    @Test
    public void testCanGoToState_START() {

        assertTrue(State.START.canGoToState(State.STOP));

        assertFalse(State.START.canGoToState(State.START));
        assertFalse(State.START.canGoToState(State.DESTROY));
        assertFalse(State.START.canGoToState(State.CREATE));
        assertFalse(State.START.canGoToState(State.INIT));
        assertFalse(State.START.canGoToState(State.INVALID));
    }

    @Test
    public void testCanGoToState_STOP() {

        assertTrue(State.STOP.canGoToState(State.START));
        assertTrue(State.STOP.canGoToState(State.DESTROY));

        assertFalse(State.STOP.canGoToState(State.STOP));
        assertFalse(State.STOP.canGoToState(State.CREATE));
        assertFalse(State.STOP.canGoToState(State.INIT));
        assertFalse(State.STOP.canGoToState(State.INVALID));
    }

    @Test
    public void testCanGoToState_DESTROY() {

        assertTrue(State.DESTROY.canGoToState(State.INVALID));

        assertFalse(State.DESTROY.canGoToState(State.START));
        assertFalse(State.DESTROY.canGoToState(State.STOP));
        assertFalse(State.DESTROY.canGoToState(State.CREATE));
        assertFalse(State.DESTROY.canGoToState(State.INIT));
        assertFalse(State.DESTROY.canGoToState(State.DESTROY));
    }
}
