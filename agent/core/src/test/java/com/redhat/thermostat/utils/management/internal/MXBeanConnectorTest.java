/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.utils.management.internal.MXBeanConnector.JMXConnectionCreator;

public class MXBeanConnectorTest {
    
    private static final String JMX_URL = "service:jmx:rmi://myHost:1099/blah";
    
    private MXBeanConnector connector;
    private JMXConnectionCreator jmxCreator;
    private AgentProxyClient client;
    
    @Before
    public void setup() throws Exception {
        jmxCreator = mock(JMXConnectionCreator.class);
        client = mock(AgentProxyClient.class);
        when(client.getJMXServiceURL()).thenReturn(JMX_URL);
        connector = new MXBeanConnector(client, jmxCreator);
    }
    
    @Test
    public void testInit() throws Exception {
        // MXBeanConnector constructor calls getJMXServiceURL
        verify(client).getJMXServiceURL();
    }
    
    @Test
    public void testConnect() throws Exception {
        JMXConnector jmxConnector = mock(JMXConnector.class);
        when(jmxCreator.create(new JMXServiceURL(JMX_URL))).thenReturn(jmxConnector);
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        when(jmxConnector.getMBeanServerConnection()).thenReturn(connection);
        
        MXBeanConnectionImpl result = connector.connect();
        assertEquals(connection, result.get());
    }

}

