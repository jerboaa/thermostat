/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.agent.ipc.tcpsocket.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.TcpSocketIPCProperties.PathUtils;

public class TcpSocketIPCPropertiesTest {

    private static final String TEST_SERVER_NAME = "testserver";
    private final String UNKNOWN_SERVER_NAME = "unknownserver";
    
    private static final String BAD_SOCKET_PORT_STRING = "not an int";
    private static final int TEST_SOCKET_PORT = 9876;
    
    private Properties jProps;
    private File propFile;
    private PathUtils pathUtils;
    
    @Before
    public void setUp() throws Exception {
        jProps = mock(Properties.class);
        pathUtils = mock(PathUtils.class);
        propFile = mock(File.class);
        final String propName = TcpSocketIPCProperties.getPropertyNameFromServerName(TEST_SERVER_NAME);
        when(jProps.getProperty(propName)).thenReturn(Integer.toString(TEST_SOCKET_PORT));
    }
    
    @Test
    public void testType() throws Exception {
        TcpSocketIPCProperties props = new TcpSocketIPCProperties(jProps, propFile, pathUtils);
        assertEquals(IPCType.TCP_SOCKET, props.getType());
    }

    @Test(expected=IOException.class)
    public void testBadPort() throws Exception {
        final String propName = TcpSocketIPCProperties.getPropertyNameFromServerName(TEST_SERVER_NAME);
        when(jProps.getProperty(propName)).thenReturn(BAD_SOCKET_PORT_STRING);
        //when(pathUtils.getSystemProperty(propName)).thenReturn(BAD_SOCKET_PORT_STRING);
        new TcpSocketIPCProperties(jProps, propFile, pathUtils).getSocketAddr(TEST_SERVER_NAME);
    }
    
    @Test(expected=IOException.class)
    public void testBadServer() throws Exception {
      final String propName = TcpSocketIPCProperties.getPropertyNameFromServerName(UNKNOWN_SERVER_NAME);
      when(jProps.getProperty(propName)).thenReturn(null);
      //when(pathUtils.getSystemProperty(propName)).thenReturn(null);
      new TcpSocketIPCProperties(jProps, propFile, pathUtils).getSocketAddr(UNKNOWN_SERVER_NAME);
    }
    
}
