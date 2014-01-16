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

package com.redhat.thermostat.common.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class MessagesTest {
    
    @Test
    public void testRequestsEqual() {
        // self
        Request req1 = new Request(RequestType.RESPONSE_EXPECTED, null);
        assertTrue(Messages.equal(req1, req1));
        req1.setParameter("test", "blah");
        assertTrue(Messages.equal(req1, req1));
        
        req1 = new Request(RequestType.RESPONSE_EXPECTED, null);
        // basics
        assertFalse(Messages.equal((Request)null, (Request)null));
        assertFalse(Messages.equal(req1, null));
        assertFalse(Messages.equal(req1, new Request(RequestType.MULTIPART_RESPONSE_EXPECTED, null)));
        
        Request req2 = new Request(RequestType.RESPONSE_EXPECTED, null);
        String receiverClassName = "com.example.receivers.MyReceiver";
        req1.setReceiver(receiverClassName);
        req2.setReceiver(receiverClassName);
        // receivers are parameters
        assertTrue(Messages.equal(req1, req2));
        
        // add parameters
        req1.setParameter("fluff", "foo");
        req2.setParameter("fluff", "foo");
        assertTrue(Messages.equal(req1, req1));
        
        // one key is different
        req2.setParameter("test", "false");
        assertFalse(Messages.equal(req1, req2));
        
        req1.setParameter("test", "false");
        assertTrue(Messages.equal(req1, req2));
        req2.setParameter("test", "true");
        assertFalse(Messages.equal(req1, req2));
    }
    
    @Test
    public void testResponsesEqual() {
        Response r = new Response(ResponseType.NOK);
        assertTrue(Messages.equal(r, r));
        Response r2 = new Response(ResponseType.NOK);
        assertTrue(Messages.equal(r, r2));
        Response r3 = new Response(ResponseType.OK);
        assertFalse(Messages.equal(r2, r3));
        assertFalse(Messages.equal(r, r3));
    }
}

