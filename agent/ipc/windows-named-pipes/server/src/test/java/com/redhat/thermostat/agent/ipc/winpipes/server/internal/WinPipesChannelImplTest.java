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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipe;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.server.internal.WinPipesServerChannelImpl.WinPipesServerChannelHelper;

public class WinPipesChannelImplTest {
    
    private static final String SERVER_NAME = "test";
    
    private WinPipesServerChannelHelper channelHelper;
    private final String PIPE_NAME = "//test/pipe/path";
    private WinPipe pipe;
    private ThermostatIPCCallbacks callbacks;
    private WinPipesIPCProperties props;
    
    @Before
    public void setUp() throws IOException {

        channelHelper = mock(WinPipesServerChannelHelper.class);

        pipe = mock(WinPipe.class);
        when(pipe.getPipeName()).thenReturn(SERVER_NAME);
        when(pipe.isOpen()).thenReturn(true);
        doNothing().when(pipe).close();

        WinPipesServerChannelImpl channel = mock(WinPipesServerChannelImpl.class);
        when(channel.getPipe()).thenReturn(pipe);
        doCallRealMethod().when(channel).isOpen();
        doCallRealMethod().when(channel).close();
        when(channel.getChannelHelper()).thenReturn(channelHelper);

        callbacks = mock(ThermostatIPCCallbacks.class);

        when(channelHelper.open(PIPE_NAME)).thenReturn(pipe);
        doCallRealMethod().when(channelHelper).isOpen(any(WinPipe.class));
        doCallRealMethod().when(channelHelper).close(any(WinPipe.class));
        //when(channelHelper.createServerChannel(PIPE_NAME, pipe, callbacks)).thenReturn(channel);
        when(channelHelper.createServerChannel(anyString(), any(WinPipe.class), any(ThermostatIPCCallbacks.class))).thenReturn(channel);

        props = mock(WinPipesIPCProperties.class);
        File propFile = mock(File.class);
        when(props.getPipeName(SERVER_NAME)).thenReturn(PIPE_NAME);
        when(props.getPropertiesFile()).thenReturn(propFile);
    }

    @Test
    public void testOpen() throws Exception {
        WinPipesServerChannelImpl channel = createChannel();
    }

    @Test
    public void testIsOpen() throws IOException {
        WinPipesServerChannelImpl channel = createChannel();
        channel.isOpen();
        verify(pipe).isOpen();
    }
    
    @Test
    public void testClose() throws IOException {
        WinPipesServerChannelImpl channel = createChannel();
        channel.close();
        verify(pipe).close();
    }

    private WinPipesServerChannelImpl createChannel() throws IOException {
        return WinPipesServerChannelImpl.createChannel(SERVER_NAME, callbacks, props, channelHelper);
    }
}
