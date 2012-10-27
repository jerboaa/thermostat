/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.utils.management;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.redhat.thermostat.common.dao.VmRef;
import com.sun.tools.attach.VirtualMachine;

public class MXBeanConnector implements Closeable {

    private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
    private String connectorAddress;
    
    private VirtualMachine vm;
    
    private boolean attached;
    
    private String reference;
    
    public MXBeanConnector(String reference) {
        this.reference = reference;
    }
    
    public MXBeanConnector(VmRef reference) {
        this.reference = reference.getStringID();
    }
    
    public synchronized void attach() throws Exception {
        if (attached)
            throw new IOException("Already attached");
        
        vm = VirtualMachine.attach(reference);
        attached = true;
        
        Properties props = vm.getAgentProperties();
        connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
        if (connectorAddress == null) {
           props = vm.getSystemProperties();
           String home = props.getProperty("java.home");
           String agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
           vm.loadAgent(agent);
           
           props = vm.getAgentProperties();
           connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
        }
    }
    
    public synchronized MXBeanConnection connect() throws Exception {
        
        if (!attached)
            throw new IOException("Agent not attached to target VM");
        
        JMXServiceURL url = new JMXServiceURL(connectorAddress);
        JMXConnector connection = JMXConnectorFactory.connect(url);
        MBeanServerConnection mbsc = null;
        try {
            mbsc = connection.getMBeanServerConnection();
            
        } catch (IOException e) {
            connection.close();
            throw e;
        }
        
        return new MXBeanConnection(connection, mbsc);
    }
    
    public boolean isAttached() {
        return attached;
    }
    
    @Override
    public synchronized void close() throws IOException {
        if (attached) {
            vm.detach();
            attached = false;
        }
    }
}
