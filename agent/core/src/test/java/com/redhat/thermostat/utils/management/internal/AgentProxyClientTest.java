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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import com.redhat.thermostat.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.utils.management.internal.AgentProxyClient.ProcessCreator;

public class AgentProxyClientTest {
    
    private static final String SERVER_NAME = "agent-proxy-2000";
    
    private AgentProxyClient client;
    private String user;
    private File binPath;
    private File ipcConfigFile;
    private Process proxy;
    private ProcessCreator procCreator;
    
    @Before
    public void setup() throws Exception {
        binPath = new File("/path/to/thermostat/bin");
        user = "Hello";
        ipcConfigFile = new File("/path/to/ipc/config");
        procCreator = mock(ProcessCreator.class);
        proxy = mock(Process.class);
        when(procCreator.startProcess(any(ProcessBuilder.class))).thenReturn(proxy);
        client = new AgentProxyClient(9000, user, binPath, ipcConfigFile, SERVER_NAME, procCreator);
    }
    
    @Test
    public void testStart() throws Exception {
        when(proxy.exitValue()).thenReturn(0);
        client.runProcess();
        
        ArgumentCaptor<ProcessBuilder> builderCaptor = ArgumentCaptor.forClass(ProcessBuilder.class);
        verify(procCreator).startProcess(builderCaptor.capture());
        ProcessBuilder builder = builderCaptor.getValue();
        
        // Check I/O redirection
        assertEquals(Redirect.INHERIT, builder.redirectInput());
        assertEquals(Redirect.INHERIT, builder.redirectOutput());
        assertEquals(Redirect.INHERIT, builder.redirectError());
        
        // Check process arguments
        List<String> args = builder.command();
        assertEquals(5, args.size());

        final String arg0 = TestUtils.convertWinPathToUnixPath(args.get(0));
        final String arg3 = TestUtils.convertWinPathToUnixPath(args.get(3));

        assertEquals("/path/to/thermostat/bin/thermostat-agent-proxy", arg0);
        assertEquals("9000", args.get(1));
        assertEquals("Hello", args.get(2));
        assertEquals("/path/to/ipc/config", arg3);
        assertEquals(SERVER_NAME, args.get(4));
        
        // Check cleanup
        verify(proxy).waitFor();
        verify(proxy).destroy();
    }
    
    @Test
    public void testStartBadExit() throws Exception {
        when(proxy.exitValue()).thenReturn(-1);
        
        try {
            client.runProcess();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(proxy).destroy();
        }
    }
    
    @Test
    public void testStartInterrupted() throws Exception {
        when(proxy.waitFor()).thenThrow(new InterruptedException());
        
        try {
            client.runProcess();
            fail("Expected InterruptedException");
        } catch (InterruptedException e) {
            verify(proxy).destroy();
        }
    }

}

