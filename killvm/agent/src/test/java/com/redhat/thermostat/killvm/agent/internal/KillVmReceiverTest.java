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

package com.redhat.thermostat.killvm.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.redhat.thermostat.service.process.ProcessHandler;
import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class KillVmReceiverTest {

    @Test
    public void receiverReturnsOk() {
        ProcessHandler proc = mock(ProcessHandler.class);
        KillVmReceiver receiver = new KillVmReceiver(proc);
        Request req = mock(Request.class);
        when(req.getParameter("vm-pid")).thenReturn("12345");
        Response response = receiver.receive(req);
        assertEquals(ResponseType.OK, response.getType());
    }
    
    @Test
    public void receiverReturnsErrorNoPid() {
        ProcessHandler proc = mock(ProcessHandler.class);
        KillVmReceiver receiver = new KillVmReceiver(proc);
        Request req = mock(Request.class);
        Response response = receiver.receive(req);
        assertEquals(ResponseType.ERROR, response.getType());
    }
    
    @Test
    public void receiverReturnsErrorBadPid() {
        ProcessHandler proc = mock(ProcessHandler.class);
        KillVmReceiver receiver = new KillVmReceiver(proc);
        Request req = mock(Request.class);
        when(req.getParameter("vm-pid")).thenReturn("hi");
        Response response = receiver.receive(req);
        assertEquals(ResponseType.ERROR, response.getType());
    }

    @Test
    public void receiverReturnsErrorNoProcessHandler() {
        KillVmReceiver receiver = new KillVmReceiver(null);
        Request req = mock(Request.class);
        Response response = receiver.receive(req);
        assertEquals(ResponseType.ERROR, response.getType());
    }

    /**
     * When a request is issued the fully qualified receiver class name is set
     * via {@link Request#setReceiver(String)}. This test makes sure that this
     * class is actually where it's supposed to be.
     * 
     * @throws Exception
     */
    @Test
    public void killVmReceiverIsInAppropriatePackage() {
        Class<?> receiver = null;
        try {
            // com.redhat.thermostat.client.killvm.internal.KillVMAction uses
            // this class name.
            receiver = Class
                    .forName("com.redhat.thermostat.killvm.agent.internal.KillVmReceiver");
        } catch (ClassNotFoundException e) {
            fail("com.redhat.thermostat.agent.killvm.internal.KillVmReceiver class not found, but used by some request!");
        }
        try {
            Constructor<?> constructor = receiver.getConstructor(ProcessHandler.class);
            ProcessHandler service = mock(ProcessHandler.class);
            Object instance = constructor.newInstance(service);
            Method m = receiver.getMethod("receive", Request.class);
            Request req = mock(Request.class);
            m.invoke(instance, req);
        } catch (Exception e) {
            e.printStackTrace();
            fail("cannot invoke receiver's receive method");
        }
    }
}

