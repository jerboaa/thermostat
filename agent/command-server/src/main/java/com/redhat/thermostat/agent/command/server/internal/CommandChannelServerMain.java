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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.redhat.thermostat.agent.ipc.client.ClientIPCService;
import com.redhat.thermostat.agent.ipc.client.ClientIPCServiceFactory;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class CommandChannelServerMain {
    
    static final String IPC_SERVER_NAME = "command-channel";
    static final String CONFIG_FILE_PROP = "ipcConfigFile";
    
    private static SSLConfigurationParser sslConfParser = new SSLConfigurationParser();
    private static ServerCreator serverCreator = new ServerCreator();
    private static ShutdownHookHandler shutdownHandler = new ShutdownHookHandler();
    private static Sleeper sleeper = new Sleeper();
    private static CommandChannelServerImpl impl = null;
    private static ClientIPCService ipcService = null;

    // TODO Add some keep alive check
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IOException("usage: thermostat-command-channel <hostname> <port>");
        }
        String hostname = args[0];
        Integer port;
        try {
            port = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Port number must be a valid integer");
        }
        
        // Get IPC configuration file location from system property
        String configFileStr = System.getProperty(CONFIG_FILE_PROP);
        if (configFileStr == null) {
            throw new IOException("Unknown IPC configuration file location");
        }
        File configFile = new File(configFileStr);
        if (ipcService == null) { // Only non-null for testing
            ipcService = ClientIPCServiceFactory.getIPCService(configFile);
        }
        // Connect to IPC server
        IPCMessageChannel channel = ipcService.connectToServer(IPC_SERVER_NAME);
        
        try {
            // Notify server has started
            sendMessage(channel, CommandChannelConstants.SERVER_STARTED_TOKEN);
        
            SSLConfiguration config = sslConfParser.parseSSLConfiguration(channel);
            
            impl = serverCreator.createServer(config, channel);
            
            // Start listening on server
            impl.startListening(hostname, port);
            // Notify server is ready to accept requests
            sendMessage(channel, CommandChannelConstants.SERVER_READY_TOKEN);
            
            shutdownHandler.addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    impl.stopListening();
                }
            }));
            
            sleeper.sleepWait();
        } catch (IOException e) {
            // Shut down server
            if (impl != null) {
                impl.stopListening();
            }
            throw new IOException("Failed to start command channel server", e);
        } finally {
            channel.close();
        }
    }
    
    private static void sendMessage(IPCMessageChannel channel, byte[] message) throws IOException {
        // Don't interleave with other messages or requests
        synchronized (channel) {
            ByteBuffer buf = ByteBuffer.wrap(message);
            channel.writeMessage(buf);
        }
    }
    
    static class ServerCreator {
        CommandChannelServerImpl createServer(SSLConfiguration sslConf, IPCMessageChannel agentChannel) {
            CommandChannelServerContext ctx = new CommandChannelServerContext(sslConf, agentChannel);
            return new CommandChannelServerImpl(ctx);
        }
    }
    
    static class ShutdownHookHandler {
        void addShutdownHook(Thread hook) {
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }
    
    static class Sleeper {
        void sleepWait() {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /* For testing purposes only */
    static void setSSLConfigurationParser(SSLConfigurationParser parser) {
        CommandChannelServerMain.sslConfParser = parser;
    }
    
    /* For testing purposes only */
    static void setServerCreator(ServerCreator creator) {
        CommandChannelServerMain.serverCreator = creator;
    }
    
    /* For testing purposes only */
    static void setShutdownHookHandler(ShutdownHookHandler handler) {
        CommandChannelServerMain.shutdownHandler = handler;
    }
    
    /* For testing purposes only */
    static void setSleeper(Sleeper sleeper) {
        CommandChannelServerMain.sleeper = sleeper;
    }
    
    /* For testing purposes only */
    static void setIPCService(ClientIPCService ipcService) {
        CommandChannelServerMain.ipcService = ipcService;
    }
}
