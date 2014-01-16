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

package com.redhat.thermostat.vm.jmx.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.vm.jmx.common.JmxCommand;

public class JmxRequestListenerTest {

    private static final String HOST = "example.org";
    private static final int PORT = 0;

    private JmxRequestListener requestListener;
    private JmxBackend backend;

    @Before
    public void setUp() {
        backend = mock(JmxBackend.class);

        requestListener = new JmxRequestListener();
        requestListener.setBackend(backend);
    }

    @Test
    public void testEnableNotifications() {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(HOST, PORT));
        req.setParameter(JmxCommand.class.getName(), JmxCommand.ENABLE_JMX_NOTIFICATIONS.name());
        req.setParameter(JmxCommand.VM_PID, "42");
        req.setParameter(JmxCommand.VM_ID, "42");

        Response result = requestListener.receive(req);

        verify(backend).enableNotificationsFor("42", 42);

        assertEquals(ResponseType.OK, result.getType());
    }

    @Test
    public void testDisableNotifications() {
        Request req = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(HOST, PORT));
        req.setParameter(JmxCommand.class.getName(), JmxCommand.DISABLE_JMX_NOTIFICATIONS.name());
        req.setParameter(JmxCommand.VM_PID, "42");
        req.setParameter(JmxCommand.VM_ID, "42");

        Response result = requestListener.receive(req);

        verify(backend).disableNotificationsFor("42", 42);

        assertEquals(ResponseType.OK, result.getType());
    }
}

