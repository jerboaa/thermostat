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

package com.redhat.thermostat.agent.ipc.server.internal;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import com.redhat.thermostat.shared.config.OS;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.internal.IPCConfigurationWriter.PropertiesHelper;

public class IPCConfigurationWriterTest {

    private IPCConfigurationWriter writer;
    private Properties props;
    private FileOutputStream fos;
    private PropertiesHelper helper;

    @Before
    public void setUp() throws Exception {
        helper = mock(PropertiesHelper.class);

        File configFile = mock(File.class);
        when(configFile.createNewFile()).thenReturn(true);
        props = mock(Properties.class);
        when(helper.createProperties()).thenReturn(props);
        fos = mock(FileOutputStream.class);
        when(helper.createStream(configFile)).thenReturn(fos);
        when(helper.getCurrentUid()).thenReturn(9876);
        
        writer = new IPCConfigurationWriter(configFile, helper);
    }
    
    @Test
    public void testWrite() throws Exception {
        writer.write();

        final IPCType expectedType = OS.IS_UNIX ? IPCType.UNIX_SOCKET : IPCType.WINDOWS_NAMED_PIPES;
        verify(props).setProperty(IPCConfigurationWriter.PROP_IPC_TYPE, expectedType.getConfigValue());
        verify(props).store(eq(fos), anyString());
        verify(fos).close();
    }
    
    @Test
    public void testCloseOnStoreException() throws Exception {
        doThrow(new IOException("TEST")).when(props).store(any(OutputStream.class), anyString());

        try {
            writer.write();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fos).close();
        }
    }
    
    @Test
    public void testNoCloseOnNullStream() throws Exception {
        when(helper.createStream(any(File.class))).thenThrow(new IOException("TEST"));

        try {
            writer.write();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fos, never()).close();
        }
    }

}
