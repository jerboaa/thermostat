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

package com.redhat.thermostat.agent.ipc.winpipes.client.internal;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.redhat.thermostat.agent.ipc.client.internal.ClientTransport;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCPropertiesProvider;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesChannelImpl;

public class WinPipesTransportImplTest {
    
    private static final String SERVER_NAME = "test";
    private static final String PIPE_NAME = "/path/to/pipe/test";
    private WinPipesTransportImpl.PipeHelper pipeHelper;
    private WinPipesMessageChannel messageChannel;
    private WinPipesIPCProperties props;

    @Before
    public void setUp() throws Exception {
        
        pipeHelper = mock(WinPipesTransportImpl.PipeHelper.class);
        WinPipesChannelImpl sockChannel = mock(WinPipesChannelImpl.class);
        when(pipeHelper.openChannel(eq(SERVER_NAME))).thenReturn(sockChannel);
        
        props = mock(WinPipesIPCProperties.class);
        when(props.getPipeName(SERVER_NAME)).thenReturn(PIPE_NAME);
    }

    @Test
    public void testConnectToServer() throws Exception {
        WinPipesTransportImpl service = new WinPipesTransportImpl(props, pipeHelper);
        IPCMessageChannel result = service.connect(SERVER_NAME);
        assertEquals(messageChannel, result);
    }

    @Test
    public void testConnectToServerBadSocket() throws Exception {
        when(pipeHelper.openChannel(anyString())).thenThrow(new IOException());
        WinPipesTransportImpl service = new WinPipesTransportImpl(props, pipeHelper);
        
        try {
            service.connect(SERVER_NAME);
            fail("Expected IOException");
        } catch (IOException ignored) {
            verify(pipeHelper).openChannel(props.getPipeName(SERVER_NAME));
        }
    }


    /**
     * small test routine to open a client connection, send a message and wait for a reply
     * @param args (ignored)
     */
    public static void main(String args[]) {

        try {
            WinPipesIPCProperties props = new WinPipesIPCPropertiesProvider().create(System.getProperties(), new File(System.getProperty("user.home") + "/.thermostat/etc/ipc.properties"));
            ClientTransport tr = new WinPipesTransportImpl(props);
            IPCMessageChannel ch = tr.connect("command-channel");
            if (!ch.isOpen()) {
                WinPipesMessageChannel wch = (WinPipesMessageChannel)(ch);
                final String name = wch.getByteChannel().getName();
                System.err.println("error opening channel '" + name + "'");
                return;
            }
            ByteBuffer msg = ByteBuffer.wrap("<SERVER READY>".getBytes(Charset.forName("UTF-8")));
            ch.writeMessage(msg);
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ByteBuffer msg2 = ch.readMessage();
            ch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
