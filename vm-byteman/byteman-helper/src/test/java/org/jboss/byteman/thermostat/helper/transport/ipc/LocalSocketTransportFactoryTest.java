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

package org.jboss.byteman.thermostat.helper.transport.ipc;

import static org.junit.Assert.assertEquals;

import org.jboss.byteman.thermostat.helper.Transport;
import org.jboss.byteman.thermostat.helper.transport.ipc.LocalSocketTransportFactory;
import org.jboss.byteman.thermostat.helper.transport.ipc.LocalSocketTransportFactory.CreatorHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalSocketTransportFactoryTest {
    
    private static final String IPC_SOCKET_NAME = "org.jboss.byteman.thermostat.socketName";
    private static final String IPC_SOCKET_NAME_VALUE = "someSocketName";
    private static final String IPC_CONFIG = "org.jboss.byteman.thermostat.ipcConfig";
    private static final String IPC_CONFIG_VALUE = "/path/to/ipcConfigFile";

    @Before
    public void setup() {
        System.setProperty(IPC_CONFIG, IPC_CONFIG_VALUE);
        System.setProperty(IPC_SOCKET_NAME, IPC_SOCKET_NAME_VALUE);
    }
    
    @After
    public void teardown() {
        System.clearProperty(IPC_CONFIG);
    }

    @Test
    public void createReadsProperties() {
        TestLocalTransportFactory factory = new TestLocalTransportFactory();
        LocalSocketTransportFactory.setInstance(factory);
        LocalSocketTransportFactory.create(); // sets holder
        CreatorHolder holder = factory.holder;
        assertEquals(IPC_SOCKET_NAME_VALUE, holder.getSocketName());
        assertEquals(IPC_CONFIG_VALUE, holder.getIpcConfigFile().getAbsolutePath());
    }
    
    static class TestLocalTransportFactory extends LocalSocketTransportFactory {
        
        CreatorHolder holder;
        
        @Override
        Transport create(CreatorHolder holder) {
            this.holder = holder;
            return null;
        }
    }
}
