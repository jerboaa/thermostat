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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.common.portability.PortableProcessImpl;
import com.redhat.thermostat.shared.config.OS;

class IPCConfigurationWriter {

    static final String PROP_IPC_TYPE = "type";
    private static final String PROP_UNIX_SOCKET_DIR = "unixsocket.dir";
    private static final String PROP_TCP_SOCKET_SUFFIX= ".tcpsocket.port";
    //private static final String TCP_SOCKET_JUMBO_FRAMES = "tcpsocket.jumboframes";

    // suggest some default vaules for TCP sockets - test this range for unused sockets
    private static int TEST_SOCKET_LOW = 51200;
    private static int TEST_SOCKET_HIGH = 55000;

    private static final String COMMENTS =
        "Configuration for Inter-process Communication (IPC) used in the Thermostat agent.\n"
        + "The agent is configured to use Unix sockets for IPC by default on Linux,\n"
        + "or TCP sockets on Windows.\n"
        + "The options below can be set to modify the defaults used by the agent:\n\n"

        + "Directory where Unix sockets are created, which may be deleted if it already exists.\n"
        + PROP_UNIX_SOCKET_DIR + "=/path/to/unix/sockets\n\n"

        + "TCP socket port numbers for various services.\n"
        + "command-channel" + PROP_TCP_SOCKET_SUFFIX + "=NNNN\n"
        + "agent-proxy" + PROP_TCP_SOCKET_SUFFIX + "=MMMM\n\n";

        //in the future, will allow jumbo frames for performance
        //+ "TCP parameters\n"
        //+ TCP_SOCKET_JUMBO_FRAMES + "=false\n\n";
    
    private final File configFile;
    private final PropertiesHelper helper;
    
    IPCConfigurationWriter(File configFile) {
        this(configFile, new PropertiesHelper());
    }
    
    IPCConfigurationWriter(File configFile, PropertiesHelper helper) {
        this.configFile = configFile;
        this.helper = helper;
    }
    
    void write() throws IOException {
        // Write defaults to config file
        configFile.createNewFile();
        
        Properties props = helper.createProperties();

        props.setProperty(PROP_IPC_TYPE, OS.IS_UNIX ? IPCType.UNIX_SOCKET.getConfigValue() : IPCType.TCP_SOCKET.getConfigValue());

        // unix socket will work without configuration (creates sockets in tmp directory
        // but tcpsocket always needs ports predefined (in the future, should support service discovery)

        // this implementation is flawed;
        //    the unused ports might be in used by a process that simply wasn't running at thermostat setup time.

        {
            int cmdPort = findUnusedTCPSocket(TEST_SOCKET_LOW, TEST_SOCKET_HIGH);
            int aport = cmdPort == 0 ? 0 : findUnusedTCPSocket(cmdPort + 1, TEST_SOCKET_HIGH);

            props.setProperty("command-channel" + PROP_TCP_SOCKET_SUFFIX, Integer.toString(cmdPort));
            props.setProperty("agent-proxy" + PROP_TCP_SOCKET_SUFFIX, Integer.toString(aport));

            // write a property for each user on the system - currently only the current user
            // note: this is required for UNIX too
            if (OS.IS_WINDOWS) {
                int uport = aport == 0 ? 0 : findUnusedTCPSocket( aport + 1, TEST_SOCKET_HIGH);
                int uid = PortableProcessImpl.INSTANCE.getUid(0); // if pid=0, gets uid of current process
                props.setProperty("agent-proxy-" + uid + PROP_TCP_SOCKET_SUFFIX, Integer.toString(uport));
            }
        }

        try (FileOutputStream fos = helper.createStream(configFile)) {
            props.store(fos, COMMENTS);
        }
    }

    private static int findUnusedTCPSocket(int lowPort, int highPort) {
        for (int port=lowPort; port<=highPort; port++) {
            if (isTCPPortAvailable(port)) {
                return port;
            }
        }
        return 0;
    }

    private static boolean isTCPPortAvailable(int tcpport) {
        try {
            final ServerSocket socket = new ServerSocket(tcpport);
            socket.close();
            return true;
        }
        catch (IOException e) {
            // socket already in use
        }
        return false;
    }
    
    // For testing purposes
    static class PropertiesHelper {
        FileOutputStream createStream(File configFile) throws IOException {
            return new FileOutputStream(configFile);
        }
        Properties createProperties() {
            return new Properties();
        }
    }

}
