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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;
import org.junit.Before;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.server.internal.WinPipesServerTransport.ChannelUtils;

public class WinPipesServerTransportTest {
    
    private static final String SERVER_NAME = "test";
    
    private WinPipesServerTransport transport;
    private SelectorProvider provider;
    private AbstractSelector selector;
    private ExecutorService execService;
    private PipenameValidator validator;
    private Path socketDirPath;
    private FileAttribute<Set<PosixFilePermission>> fileAttr;
    private Path socketPath;
    private ThermostatIPCCallbacks callbacks;
    private ChannelUtils channelUtils;
    private WinPipesServerChannelImpl channel;
    private WinPipesIPCProperties props;
    private UserPrincipalLookupService lookup;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        provider = mock(SelectorProvider.class);
        selector = mock(AbstractSelector.class);
        when(provider.openSelector()).thenReturn(selector);
        
        props = mock(WinPipesIPCProperties.class);
        File sockDirFile = mock(File.class);
      //  when(props.getPipePrefix()).thenReturn(sockDirFile);
        socketDirPath = mock(Path.class);
        when(socketDirPath.toAbsolutePath()).thenReturn(socketDirPath);
        when(socketDirPath.normalize()).thenReturn(socketDirPath);
        when(sockDirFile.toPath()).thenReturn(socketDirPath);
        socketPath = mock(Path.class);
        //when(socketDirPath.resolve(WinPipesServerTransport. + SERVER_NAME)).thenReturn(socketPath);
        

        
        execService = mock(ExecutorService.class);
        validator = mock(PipenameValidator.class);
        when(validator.validate(any(String.class))).thenReturn(true);

        
        channelUtils = mock(ChannelUtils.class);
        channel = mock(WinPipesServerChannelImpl.class);
        File socketFile = mock(File.class);
        when(socketFile.toPath()).thenReturn(socketPath);

        
        callbacks = mock(ThermostatIPCCallbacks.class);

    }

}
