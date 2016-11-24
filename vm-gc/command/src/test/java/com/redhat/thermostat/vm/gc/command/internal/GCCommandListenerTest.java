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

package com.redhat.thermostat.vm.gc.command.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;

public class GCCommandListenerTest {

    private static Locale defaultLocale;

    private GCCommandListener listener;

    private Logger logger;
    private PrintStream out;
    private PrintStream err;

    @BeforeClass
    public static void setupClass() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void teardownClass() {
        Locale.setDefault(defaultLocale);
    }

    @Before
    public void setup() {
        logger = mock(Logger.class);
        out = mock(PrintStream.class);
        err = mock(PrintStream.class);

        listener = new GCCommandListener(logger, out, err);
    }

    @Test
    public void testSuccessfulGCResponse() {
        listener.fireComplete(mock(Request.class), new Response(Response.ResponseType.OK));

        verify(logger).log(Level.INFO, "Garbage Collection performed on VM with PID null");
        verify(out).println("GC Successful for VM with PID: null");
    }

    @Test
    public void testErrorResponse() {
        listener.fireComplete(mock(Request.class), new Response(Response.ResponseType.ERROR));

        verify(logger).log(Level.SEVERE, "GC Request error for VM PID null");
        verify(err).println("GC Request Error for VM with PID: null");
    }

    @Test
    public void testDefaultResponse() {
        listener.fireComplete(mock(Request.class), new Response(Response.ResponseType.NOK));

        verify(logger).log(Level.WARNING, "Unknown result from GC command");
        verify(out).println("Unknown result for GC request");
    }

}
