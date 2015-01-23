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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.utils.management.internal.AgentProxyClient.ProcessCreator;

public class AgentProxyClientTest {
    
    private AgentProxyClient client;
    private ProcessCreator procCreator;
    private String user;
    private File binPath;
    
    @Before
    public void setup() throws Exception {
        procCreator = mock(ProcessCreator.class);
        binPath = new File("/path/to/thermostat/bin");
        user = "Hello";
        client = new AgentProxyClient(9000, user, binPath, procCreator);
    }
    
    @Test
    public void testCreateProxy() throws Exception {
        Process proxy = mock(Process.class);
        final String jmxUrl = "myJmxUrl";
        when(proxy.getInputStream()).thenReturn(new ByteArrayInputStream(jmxUrl.getBytes()));
        when(proxy.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(procCreator.createAndRunProcess(any(String[].class))).thenReturn(proxy);
        
        // Check returned URL
        String result = client.getJMXServiceURL();
        assertEquals(jmxUrl, result);
        
        // Check process arguments
        ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(procCreator).createAndRunProcess(argsCaptor.capture());
        String[] args = argsCaptor.getValue();
        assertEquals(3, args.length);
        assertEquals("/path/to/thermostat/bin/thermostat-agent-proxy", args[0]);
        assertEquals("9000", args[1]);
        assertEquals("Hello", args[2]);
        
        // Check cleanup
        verify(proxy).waitFor();
    }
    
    @Test
    public void testErrorHandler() throws Exception {
        Process proxy = mock(Process.class);
        final String errors = "This is an error\nThis is also an error\nOh no!\n";
        when(proxy.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(proxy.getErrorStream()).thenReturn(new ByteArrayInputStream(errors.getBytes()));
        when(procCreator.createAndRunProcess(any(String[].class))).thenReturn(proxy);
        
        List<LogRecord> logMessages = new ArrayList<>();
        TestLogHandler logHandler = new TestLogHandler(logMessages);
        LoggingUtils.getLogger(AgentProxyClient.class).addHandler(logHandler);
        
        try {
            try {
                client.getJMXServiceURL();
                fail("Expected exception");
            } catch (IOException e) {
                // Expected
            }
            assertEquals(3, logMessages.size());
            assertEquals("This is an error", logMessages.get(0).getMessage());
            assertEquals("This is also an error", logMessages.get(1).getMessage());
            assertEquals("Oh no!", logMessages.get(2).getMessage());
        } finally {
            LoggingUtils.getLogger(AgentProxyClient.class).removeHandler(logHandler);
        }
    }
    
    private static class TestLogHandler extends Handler {
        
        private List<LogRecord> logMessages;
        public TestLogHandler(List<LogRecord> logMessages) {
            this.logMessages = logMessages;
        }
        
        @Override
        public void publish(LogRecord record) {
            logMessages.add(record);
        }
        
        @Override
        public void flush() {
            // Do nothing
        }
        
        @Override
        public void close() throws SecurityException {
            // Do nothing
        }
    }

}

