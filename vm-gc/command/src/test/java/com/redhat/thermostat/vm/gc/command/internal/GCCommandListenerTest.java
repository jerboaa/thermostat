/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class GCCommandListenerTest {

    private final GCCommandListener listener = new GCCommandListener();
    private static final Logger logger = LoggingUtils
            .getLogger(GCCommandListener.class);

    private PrintStream ps;
    private ByteArrayOutputStream baos;


    @Before
    public void setup() {
        baos = new ByteArrayOutputStream();
        ps = new PrintStream(baos);
        listener.setOut(ps);
        listener.setErr(ps);
    }

    @Test
    public void testSuccessfulGCResponse() {
        Request request = mock(Request.class);

        Response resp = new Response(Response.ResponseType.OK);

        Handler handler = mock(Handler.class);
        logger.addHandler(handler);

        final boolean[] complete = {false};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                LogRecord log = (LogRecord)invocation.getArguments()[0];
                if (log.getLevel().equals(Level.INFO)) {
                    complete[0] = true;
                }
                return null;
            }
        }).when(handler).publish(any(LogRecord.class));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);

        String expected = "GC Successful for VM with PID: null\n";
        assertEquals(expected, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testErrorResponse() {
        Request request = mock(Request.class);
        Response resp = new Response(Response.ResponseType.ERROR);

        Handler handler = mock(Handler.class);
        logger.addHandler(handler);

        final boolean[] complete = {false};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                LogRecord log = (LogRecord)invocation.getArguments()[0];
                if (log.getLevel().equals(Level.SEVERE)) {
                    complete[0] = true;
                }
                return null;
            }
        }).when(handler).publish(any(LogRecord.class));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);

        String expected = "GC Request Error for VM with PID: null\n";
        assertEquals(expected, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testDefaultResponse() {
        Request request = mock(Request.class);
        Response resp = new Response(Response.ResponseType.NOK);

        Handler handler = mock(Handler.class);
        logger.addHandler(handler);

        final boolean[] complete = {false};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                LogRecord log = (LogRecord)invocation.getArguments()[0];
                if (log.getLevel().equals(Level.WARNING)) {
                    complete[0] = true;
                }
                return null;
            }
        }).when(handler).publish(any(LogRecord.class));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);

        assertTrue(baos.size() > 0);

        String expected = "Unknown result for GC request\n";
        assertEquals(expected, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }
}
