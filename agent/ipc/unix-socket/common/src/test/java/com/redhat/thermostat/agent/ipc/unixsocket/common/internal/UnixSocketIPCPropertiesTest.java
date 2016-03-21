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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties.PathUtils;

public class UnixSocketIPCPropertiesTest {
    
    private static final String SOCKET_DIR_PATH = "/path/to/sockets";
    
    private Properties jProps;
    private PathUtils pathUtils;
    
    @Before
    public void setUp() throws Exception {
        jProps = mock(Properties.class);
        pathUtils = mock(PathUtils.class);
        when(jProps.getProperty(UnixSocketIPCProperties.PROP_UNIX_SOCKET_DIR)).thenReturn(SOCKET_DIR_PATH);
    }
    
    @Test
    public void testType() throws Exception {
        UnixSocketIPCProperties props = new UnixSocketIPCProperties(jProps, pathUtils);
        assertEquals(IPCType.UNIX_SOCKET, props.getType());
    }
    
    @Test
    public void testSocketDirectory() throws Exception {
        UnixSocketIPCProperties props = new UnixSocketIPCProperties(jProps, pathUtils);
        String path = props.getSocketDirectory().getAbsolutePath();
        assertEquals(SOCKET_DIR_PATH, path);
    }
    
    @Test
    public void testDefaultSocketDirectoryXDG() throws Exception {
        when(jProps.getProperty(UnixSocketIPCProperties.PROP_UNIX_SOCKET_DIR)).thenReturn(null);
        when(pathUtils.getEnvironmentVariable("XDG_RUNTIME_DIR")).thenReturn("/path/to/xdg/runtime");
        UnixSocketIPCProperties props = new UnixSocketIPCProperties(jProps, pathUtils);
        assertEquals("/path/to/xdg/runtime/thermostat-socks", props.getSocketDirectory().getAbsolutePath());
    }
    
    @Test
    public void testDefaultSocketDirectoryTmp() throws Exception {
        when(jProps.getProperty(UnixSocketIPCProperties.PROP_UNIX_SOCKET_DIR)).thenReturn(null);
        when(pathUtils.getSystemProperty("java.io.tmpdir")).thenReturn("/path/to/tmp");
        when(pathUtils.getSystemProperty("user.name")).thenReturn("myUserName");
        UnixSocketIPCProperties props = new UnixSocketIPCProperties(jProps, pathUtils);
        assertEquals("/path/to/tmp/myUserName/thermostat-socks", props.getSocketDirectory().getAbsolutePath());
    }
    
    @Test(expected=IOException.class)
    public void testNoXDGNoTmp() throws Exception {
        when(jProps.getProperty(UnixSocketIPCProperties.PROP_UNIX_SOCKET_DIR)).thenReturn(null);
        new UnixSocketIPCProperties(jProps, pathUtils);
    }
    
    @Test(expected=IOException.class)
    public void testNoUsername() throws Exception {
        when(jProps.getProperty(UnixSocketIPCProperties.PROP_UNIX_SOCKET_DIR)).thenReturn(null);
        when(pathUtils.getSystemProperty("java.io.tmpdir")).thenReturn("/path/to/tmp");
        new UnixSocketIPCProperties(jProps, pathUtils);
    }
    
}
