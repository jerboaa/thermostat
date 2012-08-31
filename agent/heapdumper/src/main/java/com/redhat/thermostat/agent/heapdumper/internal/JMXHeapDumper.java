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


package com.redhat.thermostat.agent.heapdumper.internal;

import java.io.File;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;

class JMXHeapDumper {

    private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";

    void dumpHeap(String vmId, String filename) throws HeapDumpException {
        try {
            doHeapDump(vmId, filename);
        } catch (Exception ex) {
            throw new HeapDumpException(ex);
        }
    }

    private void doHeapDump(String vmId, String filename) throws Exception {

        VirtualMachine vm = VirtualMachine.attach(vmId);
        String connectorAddress = getConnectorAddress(vm);
        JMXServiceURL url = new JMXServiceURL(connectorAddress);

        try (JMXConnector conn = JMXConnectorFactory.connect(url)) {
            MBeanServerConnection mbsc = conn.getMBeanServerConnection();
            mbsc.invoke(new ObjectName("com.sun.management:type=HotSpotDiagnostic"),
                        "dumpHeap",
                        new Object[] { filename, Boolean.TRUE },
                        new String[] { String.class.getName(), boolean.class.getName() });
        }
    }

    private String getConnectorAddress(VirtualMachine vm) throws Exception {

        Properties props = vm.getAgentProperties();
        String connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
        if (connectorAddress == null) {
            props = vm.getSystemProperties();
            String home = props.getProperty("java.home");
            String agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
            vm.loadAgent(agent);
            props = vm.getAgentProperties();
            connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
        }
        return connectorAddress;
    }
}
