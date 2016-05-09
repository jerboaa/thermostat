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

package com.redhat.thermostat.vm.byteman.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class BytemanRequestResponseListenerTest {

    private static final String EMPTY_STRING = "";
    private BytemanRequestResponseListener listener;
    private ByteArrayOutputStream bout;
    private ByteArrayOutputStream berr;
    private CountDownLatch latch;
    
    @Before
    public void setup() {
        bout = new ByteArrayOutputStream();
        berr = new ByteArrayOutputStream();
        CommandContext ctx = mock(CommandContext.class);
        Console console = mock(Console.class);
        when(ctx.getConsole()).thenReturn(console);
        PrintStream outStream = new PrintStream(bout);
        PrintStream errStream = new PrintStream(berr);
        when(console.getError()).thenReturn(errStream);
        when(console.getOutput()).thenReturn(outStream);
        latch = mock(CountDownLatch.class);
        listener = new BytemanRequestResponseListener(latch, ctx);
    }
    
    @Test
    public void testAuthIssue() {
        listener.fireComplete(mock(Request.class), new Response(ResponseType.AUTH_FAILED));
        verify(latch).countDown();
        String stdOut = getOutAsString();
        String errOut = getErrAsString(); 
        assertEquals(EMPTY_STRING, stdOut);
        assertTrue(errOut.contains("authentication"));
        assertTrue(errOut.contains("issue"));
    }
    
    private String getOutAsString() {
        return new String(bout.toByteArray());
    }
    
    private String getErrAsString() {
        return new String(berr.toByteArray());
    }

    @Test
    public void testSuccess() {
        listener.fireComplete(mock(Request.class), new Response(ResponseType.OK));
        verify(latch).countDown();
        String stdOut = getOutAsString();
        String errOut = getErrAsString(); 
        assertEquals("Request submitted successfully.\n", stdOut);
        assertEquals(EMPTY_STRING, errOut);
    }
    
    @Test
    public void testUnknownError() {
        listener.fireComplete(mock(Request.class), new Response(ResponseType.ERROR));
        verify(latch).countDown();
        String stdOut = getOutAsString();
        String errOut = getErrAsString(); 
        assertEquals(EMPTY_STRING, stdOut);
        assertTrue(errOut.contains("unknown"));
        assertTrue(errOut.contains("reason"));
    }
    
    @Test
    public void testUnknownType() {
        listener.fireComplete(mock(Request.class), new Response(ResponseType.NOK));
        verify(latch).countDown();
        String stdOut = getOutAsString();
        String errOut = getErrAsString(); 
        assertEquals(EMPTY_STRING, stdOut);
        assertEquals("Unknown response: NOK\n", errOut);
    }
}
