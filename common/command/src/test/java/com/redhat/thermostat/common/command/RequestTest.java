/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.common.command.Request.RequestType;

public class RequestTest {

    private class RequestResponseListenerImpl implements RequestResponseListener {
        @Override
        public void fireComplete(Request request, Response response) {
            // Won't actually be used.
        }
    }

    private static final int PORT = 123;
    private static final String HOST = "test.example.com";

    @Test
    public void testGetTypeAndTarget() {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(HOST, PORT));
        RequestType type = (RequestType) request.getType();
        assertEquals(RequestType.RESPONSE_EXPECTED, type);

        InetSocketAddress target = (InetSocketAddress) request.getTarget();
        assertEquals(PORT, target.getPort());
        assertEquals(HOST, target.getHostString());
    }

    @Test
    public void testAddListener() {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(HOST, PORT));
        RequestResponseListener listener1 = new RequestResponseListenerImpl();
        RequestResponseListener listener2 = new RequestResponseListenerImpl();
        request.addListener(listener1);
        request.addListener(listener2);
        Collection<RequestResponseListener> listeners = request.getListeners();
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));
    }

    @Test
    public void testRemoveListener() {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(HOST, PORT));
        RequestResponseListener listener1 = new RequestResponseListenerImpl();
        RequestResponseListener listener2 = new RequestResponseListenerImpl();
        request.addListener(listener1);
        request.addListener(listener2);
        Collection<RequestResponseListener> listeners = request.getListeners();
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));
        request.removeListener(listener1);
        listeners = request.getListeners();
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener2));
        assertFalse(listeners.contains(listener1));
    }
    
    @Test
    public void canGetHostname() {
        // unresolved hostname
        InetSocketAddress addr = new InetSocketAddress(HOST, PORT);
        assertTrue(addr.isUnresolved());
        Request request = new Request(RequestType.RESPONSE_EXPECTED, addr);
        assertEquals(HOST, request.getTarget().getHostString());
        
    }
    
    @Test
    public void testToString() {
        InetSocketAddress addr = new InetSocketAddress(1234);
        Request request = new Request(RequestType.RESPONSE_EXPECTED, addr);
        assertEquals("{ Request: {target = "+ addr.toString() + "}, {type = RESPONSE_EXPECTED} }", request.toString());
    }
}

