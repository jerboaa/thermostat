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

package com.redhat.thermostat.agent.ipc.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesBuilder.PropertiesHelper;

public class IPCPropertiesBuilderTest {
    
    private IPCProperties ipcProps;
    private Properties jProps;
    private PropertiesHelper helper;
    private File propFile;
    private FileInputStream propFileInputStream;
    
    @Before
    public void setUp() throws Exception {
        ipcProps = mock(IPCProperties.class);
        jProps = mock(Properties.class);
        helper = mock(PropertiesHelper.class);
        propFile = mock(File.class);
        when(helper.createProperties()).thenReturn(jProps);
        propFileInputStream = mock(FileInputStream.class);
        when(helper.getInputStream(propFile)).thenReturn(propFileInputStream);
    }
    
    @Test
    public void testGetProperties() throws Exception {
        IPCPropertiesBuilder builder = new TestIPCPropertiesBuilder();
        when(jProps.getProperty(IPCPropertiesBuilder.PROP_IPC_TYPE)).thenReturn(IPCType.UNIX_SOCKET.getConfigValue());
        
        IPCProperties result = builder.getProperties(propFile);
        
        verify(helper).createProperties();
        verify(helper).getInputStream(propFile);
        verify(jProps).load(propFileInputStream);
        
        assertEquals(ipcProps, result);
    }
    
    @Test(expected=IOException.class)
    public void testGetPropertiesNoType() throws Exception {
        IPCPropertiesBuilder builder = new TestIPCPropertiesBuilder();
        when(jProps.getProperty(IPCPropertiesBuilder.PROP_IPC_TYPE)).thenReturn(null);
        builder.getProperties(propFile);
    }
    
    @Test(expected=IOException.class)
    public void testGetPropertiesBadType() throws Exception {
        IPCPropertiesBuilder builder = new TestIPCPropertiesBuilder();
        when(jProps.getProperty(IPCPropertiesBuilder.PROP_IPC_TYPE)).thenReturn("Not A Real IPC Type");
        builder.getProperties(propFile);
    }
    
    private class TestIPCPropertiesBuilder extends IPCPropertiesBuilder {
        
        private TestIPCPropertiesBuilder() {
            super(helper);
        }
        
        @Override
        protected IPCProperties getPropertiesForType(IPCType type, Properties props) throws IOException {
            if (IPCType.UNIX_SOCKET.equals(type) && jProps.equals(props)) {
                return ipcProps;
            } else {
                throw new IOException("Wrong type or properties");
            }
        }
        
    }

}
