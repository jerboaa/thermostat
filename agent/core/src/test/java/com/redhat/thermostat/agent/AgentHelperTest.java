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

package com.redhat.thermostat.agent;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.storage.model.BackendInformation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentHelperTest {

    public static final String NAME = "fluff";
    public static final String DESCRIPTION = "fluff description";
    public static final int ORDER_VALUE = 42;
    public static final String WRITER_ID = "42";

    @Test
    public void testCreateBackendInformation() throws Exception {

        Backend backend = mock(Backend.class);
        when(backend.getName()).thenReturn(NAME);
        when(backend.getDescription()).thenReturn(DESCRIPTION);
        when(backend.getObserveNewJvm()).thenReturn(true);
        when(backend.isActive()).thenReturn(true);
        when(backend.getOrderValue()).thenReturn(ORDER_VALUE);

        BackendInformation information =
                AgentHelper.createBackendInformation(backend, WRITER_ID);

        assertEquals(NAME, information.getName());
        assertEquals(DESCRIPTION, information.getDescription());
        assertEquals(true, information.isObserveNewJvm());
        assertEquals(true, information.isActive());
        assertEquals(ORDER_VALUE, information.getOrderValue());
        assertEquals(WRITER_ID, information.getAgentId());
    }

    @Test
    public void testCreateBackendInformationNotActiveNotMonitoring() throws Exception {

        Backend backend = mock(Backend.class);
        when(backend.getName()).thenReturn(NAME);
        when(backend.getDescription()).thenReturn(DESCRIPTION);
        when(backend.getObserveNewJvm()).thenReturn(false);
        when(backend.isActive()).thenReturn(false);
        when(backend.getOrderValue()).thenReturn(ORDER_VALUE);

        BackendInformation information =
                AgentHelper.createBackendInformation(backend, WRITER_ID);

        assertEquals(NAME, information.getName());
        assertEquals(DESCRIPTION, information.getDescription());
        assertEquals(false, information.isObserveNewJvm());
        assertEquals(false, information.isActive());
        assertEquals(ORDER_VALUE, information.getOrderValue());
        assertEquals(WRITER_ID, information.getAgentId());
    }
}
