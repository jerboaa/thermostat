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

package com.redhat.thermostat.agent.command.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;


import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.IPCMessage;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.command.Message.MessageType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.config.OS;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;

class CommandChannelDelegate implements ConfigurationServer, ThermostatIPCCallbacks {
    
    private static final Logger logger = LoggingUtils.getLogger(CommandChannelDelegate.class);
    private static final String CMD_NAME = "thermostat-command-channel";
    private static final String IPC_SERVER_NAME = "command-channel";

    // States for 'state' field
    private static final int STATE_NOT_STARTED = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_READY = 2;
    private static final int STATE_ERROR = -1;
    
    private final ReceiverRegistry receivers;
    private final SSLConfiguration sslConf;
    private final StorageGetter storageGetter;
    private final File binPath;
    private final AgentIPCService ipcService;
    private final CountDownLatch readyLatch;
    private final SSLConfigurationEncoder sslEncoder;
    private final AgentRequestDecoder requestDecoder;
    private final AgentResponseEncoder responseEncoder;
    private final ProcessCreator procCreator;
    private final ProcessUserInfoBuilder userInfoBuilder;
    private final FileSystemUtils fsUtils;
    private Process process;
    private AtomicInteger state;
    
    CommandChannelDelegate(ReceiverRegistry receivers, SSLConfiguration sslConf, File binPath,
            AgentIPCService ipcService) {
        this(receivers, sslConf, binPath, ipcService, new CountDownLatch(1), new SSLConfigurationEncoder(), 
                new AgentRequestDecoder(), new AgentResponseEncoder(), new StorageGetter(), new ProcessUserInfoBuilder(), 
                new FileSystemUtils(), new ProcessCreator());
    }

    /** For testing only */
    CommandChannelDelegate(ReceiverRegistry receivers, SSLConfiguration sslConf, File binPath, 
            AgentIPCService ipcService, CountDownLatch readyLatch, SSLConfigurationEncoder sslEncoder, 
            AgentRequestDecoder requestDecoder, AgentResponseEncoder responseEncoder, StorageGetter getter, 
            ProcessUserInfoBuilder userInfoBuilder, FileSystemUtils fsUtils, ProcessCreator procCreator) {
        this.storageGetter = getter;
        this.receivers = receivers;
        this.sslConf = sslConf;
        this.binPath = binPath;
        this.ipcService = ipcService;
        this.readyLatch = readyLatch;
        this.sslEncoder = sslEncoder;
        this.requestDecoder = requestDecoder;
        this.responseEncoder = responseEncoder;
        this.procCreator = procCreator;
        this.userInfoBuilder = userInfoBuilder;
        this.fsUtils = fsUtils;
        this.state = new AtomicInteger();
    }

    @Override
    public void startListening(String hostname, int port) throws IOException {
        // Determine if this process is running as a privileged user
        // NOTE: on Windows, security is handled by permission bits, not userid.
        if (OS.IS_UNIX && userInfoBuilder.isPrivilegedUser()) {
            // Get owner of command channel script, which will also be the user running it
            Path cmdPath = fsUtils.getPath(binPath.getAbsolutePath(), CMD_NAME);
            UserPrincipal unprivilegedPrincipal = fsUtils.getOwner(cmdPath);
            // Create IPC server owned by user running command channel script
            ipcService.createServer(IPC_SERVER_NAME, this, unprivilegedPrincipal);
        } else {
            // Create IPC server owned by current user
            ipcService.createServer(IPC_SERVER_NAME, this);
        }
        
        startServer(hostname, port);
    }

    @Override
    public void stopListening() {
        try {
            killServer();
            // Clean up IPC server
            destroyIPCServerIfExists();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error occurred while stopping command channel server", e);
        }
    }

    private void destroyIPCServerIfExists() throws IOException {
        if (ipcService.serverExists(IPC_SERVER_NAME)) {
            ipcService.destroyServer(IPC_SERVER_NAME);
        }
    }
    
    @Override
    public void messageReceived(IPCMessage message) {
        byte[] result = null;
        ByteBuffer buf = message.get();
        byte[] data = new byte[buf.limit()];
        buf.get(data);
        
        switch (state.get()) {
        case STATE_NOT_STARTED:
            // First message from server just tells us it's started
            boolean started = checkStart(data);
            if (started) {
                // Return SSL Configuration
                try {
                    result = sslEncoder.encodeAsJson(sslConf);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to encode SSL configuration", e);
                    state.set(STATE_ERROR);
                }
            }
            break;
        case STATE_STARTED:
            // Second message from server indicates that it has processed
            // the SSL configuration and is ready to receive requests
            checkReady(data);
            readyLatch.countDown();
            // No response
            break;
        case STATE_READY:
            // Parse requests
            Request request;
            try {
                request = parseRequest(data);
                // Return response as bytes
                result = requestReceived(request);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to process request from command channel", e);
                // Send error response
                Response error = new Response(ResponseType.ERROR);
                result = encodeResponse(error);
            }
            break;
        default: // STATE_ERROR
            // Do nothing
        }
        
        if (state.get() == STATE_ERROR && readyLatch.getCount() > 0) {
            // Will never become ready, so throw exception
            readyLatch.countDown();
        }
        
        // Send reply using IPC service
        if (result != null) {
            ByteBuffer reply = ByteBuffer.wrap(result);
            try {
                message.reply(reply);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to send reply via command channel", e);
            }
        }
    }
    
    private void startServer(String hostname, int port) throws IOException {
        File ipcConfig = ipcService.getConfigurationFile();
        String[] processArgs = OS.IS_UNIX
                ? new String[]{ binPath.getAbsolutePath() + File.separator + CMD_NAME, hostname,
                                String.valueOf(port), ipcConfig.getAbsolutePath() }
                : new String[] { "cmd", "/c", binPath.getAbsolutePath() + File.separator + CMD_NAME + ".cmd", hostname,
                                String.valueOf(port), ipcConfig.getAbsolutePath() };

        ProcessBuilder builder = new ProcessBuilder(processArgs);
        // This has the problem of some messages/Exceptions not
        // showing up in the parent's stderr stream if used together
        // with JUL-logging. One such example is CNFE in
        // the child process. In that case your best bet is
        // Redirect.to(File).
        builder.inheritIO();
        logger.info("Starting command channel server process");
        process = procCreator.startProcess(builder);
        
        // Wait for started notification
        try {
            waitForStarted();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void waitForStarted() throws IOException, InterruptedException {
        // Wait for started token
        readyLatch.await();
        // If not started at this point, there was a token mismatch
        if (state.get() != STATE_READY) {
            throw new IOException("Command channel server failed to start");
        }
        logger.info("Command channel server ready to accept requests");
    }

    private void killServer() {
        if (process != null) {
            logger.info("Stopping command channel server process");
            process.destroy();
        }
    }
    
    private byte[] requestReceived(Request request) {
        String receiverName = request.getReceiver();
        MessageType requestType = request.getType();
        logger.info("Request received: '" + requestType + "' for '" + receiverName + "'");
        boolean authSucceeded = authenticateRequestIfNecessary(request);
        Response response = null;
        if (! authSucceeded) {
            logger.info("Authentication for request failed");
            response = new Response(ResponseType.AUTH_FAILED);
        } else {
            if (receiverName != null && requestType != null) {
                RequestReceiver receiver = receivers.getReceiver(receiverName);
                if (receiver != null) {
                    response = receiver.receive(request);
                }
            }

            if (response == null) {
                logger.info("Receiver with name '" + receiverName + "' not found ");
                response = new Response(ResponseType.ERROR);
            }
        }
        
        return encodeResponse(response);
    }

    private Request parseRequest(byte[] data) throws IOException {
        return requestDecoder.decodeRequest(data);
    }

    private byte[] encodeResponse(Response response) {
        return responseEncoder.encodeResponse(response);
    }

    private boolean authenticateRequestIfNecessary(Request request) {
        Storage storage = storageGetter.get();
        boolean result = false;
        if (storage instanceof SecureStorage) {
            result = authenticateRequest(request, (SecureStorage) storage);
            if (result) {
                logger.finest("Authentication and authorization for request " + request + " succeeded!");
            } else {
                logger.finest("Request " + request + " failed to authenticate or authorize");
            }
        } else {
            result = true;
        }
        storageGetter.unget();
        return result;
    }

    private boolean authenticateRequest(Request request, SecureStorage storage) {
        String clientTokenStr = request.getParameter(Request.CLIENT_TOKEN);
        byte[] clientToken = Base64.decodeBase64(clientTokenStr);
        String authTokenStr = request.getParameter(Request.AUTH_TOKEN);
        byte[] authToken = Base64.decodeBase64(authTokenStr);
        AuthToken token = new AuthToken(authToken, clientToken);
        String actionName = request.getParameter(Request.ACTION);
        try {
            // actionName must not be null here. If we somehow get a bogus request
            // at this point where this does not exist, verifyToken will throw a
            // NPE.
            return storage.verifyToken(token, actionName);
        } catch (NullPointerException e) {
            return false; 
        }
    }

    private boolean checkStart(byte[] data) {
        boolean tokenMatch = Arrays.equals(CommandChannelConstants.SERVER_STARTED_TOKEN, data);
        if (!tokenMatch) {
            String message = new String(data, Charset.forName("UTF-8"));
            logger.severe("Unexpected start message from command channel: " + message);
            state.set(STATE_ERROR);
        } else {
            // Set state to indicate started
            state.set(STATE_STARTED);
        }
        return tokenMatch;
    }
    
    private void checkReady(byte[] data) {
        boolean tokenMatch = Arrays.equals(CommandChannelConstants.SERVER_READY_TOKEN, data);
        if (!tokenMatch) {
            String message = new String(data, Charset.forName("UTF-8"));
            logger.severe("Unexpected ready message from command channel: " + message);
            state.set(STATE_ERROR);
        } else {
            // Set state to indicate ready
            state.set(STATE_READY);
        }
    }

    /** for testing only */
    static class StorageGetter {
        Storage get() {
            BundleContext bCtx = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<Storage> storageRef = bCtx.getServiceReference(Storage.class);
            Storage storage = (Storage) bCtx.getService(storageRef);
            return storage;
        }
        
        void unget() {
            BundleContext bCtx = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<Storage> storageRef = bCtx.getServiceReference(Storage.class);
            bCtx.ungetService(storageRef);
        }
    }
    
    /** for testing only */
    static class ProcessCreator {
        Process startProcess(ProcessBuilder builder) throws IOException {
            return builder.start();
        }
    }
    
    /** for testing only */
    static class FileSystemUtils {
        Path getPath(String first, String... more) {
            return FileSystems.getDefault().getPath(first, more);
        }
        
        UserPrincipal getOwner(Path path) throws IOException {
            return Files.getOwner(path);
        }
    }
    
}


