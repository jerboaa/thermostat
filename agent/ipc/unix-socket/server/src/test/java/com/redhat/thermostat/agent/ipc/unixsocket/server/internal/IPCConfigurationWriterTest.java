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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.IPCConfigurationWriter.PropertiesHelper;
import com.redhat.thermostat.shared.config.CommonPaths;

public class IPCConfigurationWriterTest {
    
    @Test
    public void testWrite() throws Exception {
        PropertiesHelper helper = mock(PropertiesHelper.class);
        
        CommonPaths paths = mock(CommonPaths.class);
        File configFile = mock(File.class);
        when(paths.getUserIPCConfigurationFile()).thenReturn(configFile);
        
        Properties props = mock(Properties.class);
        when(helper.createProperties()).thenReturn(props);
        FileOutputStream fos = mock(FileOutputStream.class);
        when(helper.createStream(configFile)).thenReturn(fos);
        
        IPCConfigurationWriter writer = new IPCConfigurationWriter(paths, helper);
        writer.write();
        
        verify(props).setProperty(IPCConfigurationWriter.PROP_IPC_TYPE, IPCType.UNIX_SOCKET.getConfigValue());
        verify(props).store(eq(fos), anyString());
        verify(fos).close();
    }

}
