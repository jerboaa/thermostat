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

package com.redhat.thermostat.agent.ipc.winpipes.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties.PathUtils;

public class WinPipesIPCPropertiesTest {

    private static final String PIPE_PREFIX = "some-prefix";
    private static final String PIPE_ID = "ABC123";
    private static final String PIPE_NAME = "pipe-name";
    private Properties jProps;
    private File propFile;
    private PathUtils pathUtils;

    @Before
    public void setUp() throws Exception {
        jProps = mock(Properties.class);
        propFile = mock(File.class);
        pathUtils = mock(PathUtils.class);
        when(pathUtils.getProperty(jProps, WinPipesIPCProperties.PIPE_PREFIX_PROPERTY, WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX)).thenReturn(PIPE_PREFIX);
        when(jProps.getProperty(WinPipesIPCProperties.PIPE_PREFIX_PROPERTY)).thenReturn(PIPE_PREFIX);
    }

    @Test
    public void testType() throws Exception {
        WinPipesIPCProperties props = new WinPipesIPCProperties(jProps, propFile, pathUtils);
        assertEquals(IPCType.WINDOWS_NAMED_PIPES, props.getType());
    }

    @Test
    public void testPrefixFromProps() throws Exception {
        when(pathUtils.getProperty(jProps, WinPipesIPCProperties.PIPE_PREFIX_PROPERTY, WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX)).thenReturn(PIPE_PREFIX);

        WinPipesIPCProperties props = new WinPipesIPCProperties(jProps, propFile, pathUtils);
        String path = props.getPipePrefix();
        assertEquals(PIPE_PREFIX, path);
    }

    @Test
    public void testDefaultPrefix() throws Exception {
        when(pathUtils.getProperty(jProps, WinPipesIPCProperties.PIPE_PREFIX_PROPERTY, WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX)).thenReturn(WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX);
        WinPipesIPCProperties props = new WinPipesIPCProperties(jProps, propFile, pathUtils);
        assertEquals(WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX, props.getPipePrefix());
    }

    @Test
    public void testFullPath() throws Exception {
        when(pathUtils.getProperty(jProps, WinPipesIPCProperties.PIPE_PREFIX_PROPERTY, WinPipesIPCProperties.DEFAULT_WINPIPE_PREFIX)).thenReturn(PIPE_PREFIX);
        when(pathUtils.getProperty(jProps, WinPipesIPCProperties.WINPIPE_ID_PROPERTY, "")).thenReturn(PIPE_ID);

        WinPipesIPCProperties props = new WinPipesIPCProperties(jProps, propFile, pathUtils);
        String path = props.getPipeName(PIPE_NAME);
        assertTrue("must start with global prefix", path.startsWith(WinPipesIPCProperties.GLOBAL_WINPIPE_PREFIX));
        assertTrue("must contain pipename", path.contains(PIPE_NAME));
        assertTrue("must contain pipe prefix", path.contains(PIPE_PREFIX));
        assertTrue("must contain pipe id", path.contains(PIPE_ID));
    }
}
