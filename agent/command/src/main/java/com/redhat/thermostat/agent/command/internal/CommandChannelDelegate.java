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

package com.redhat.thermostat.agent.command.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.command.internal.ProcessOutputStreamReader.ProcessOutputStreamRequestListener;
import com.redhat.thermostat.agent.command.internal.ProcessStreamReader.ExceptionListener;
import com.redhat.thermostat.common.command.Message.MessageType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;

class CommandChannelDelegate implements ConfigurationServer, ProcessOutputStreamRequestListener {
    
    private static final String CMD_NAME = "thermostat-command-channel";
    private static final Logger logger = LoggingUtils.getLogger(CommandChannelDelegate.class);
    
    private final ReceiverRegistry receivers;
    private final SSLConfiguration sslConf;
    private final StorageGetter storageGetter;
    private final File binPath;
    private final ProcessCreator procCreator;
    private Process process;
    private ProcessStreamReader stdoutReader;
    private PrintWriter printer;
    
    CommandChannelDelegate(ReceiverRegistry receivers, SSLConfiguration sslConf, File binPath) {
        this(receivers, sslConf, binPath, new StorageGetter(), new ProcessCreator());
    }

    /** For testing only */
    CommandChannelDelegate(ReceiverRegistry receivers, SSLConfiguration sslConf, File binPath, 
            StorageGetter getter, ProcessCreator procCreator) {
        this.storageGetter = getter;
        this.receivers = receivers;
        this.sslConf = sslConf;
        this.binPath = binPath;
        this.procCreator = procCreator;
    }

    @Override
    public void startListening(String hostname, int port) throws IOException {
        startServer(hostname, port);
    }

    @Override
    public void stopListening() {
        try {
            killServer();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error occurred while stopping command channel server", e);
        }
    }
    
    void startServer(String hostname, int port) throws IOException {
        String[] processArgs = { binPath.getAbsolutePath() + File.separator + CMD_NAME, hostname, String.valueOf(port) };
        logger.info("Starting command channel server process");
        process = procCreator.startProcess(processArgs);
        
        ExceptionListener exceptionListener = new ExceptionListener() {
            
            @Override
            public void notifyException(IOException e) {
                // Log exception, send ERROR Response
                logger.log(Level.WARNING, "Unexpected input received from command channel server", e);
                writeResponse(new Response(ResponseType.ERROR));
            }
        };
        stdoutReader = new ProcessOutputStreamReader(process.getInputStream(), this, exceptionListener);
        // Must be instantiated before starting output reader
        printer = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
        stdoutReader.start();
        
        SSLConfigurationWriter sslWriter = new SSLConfigurationWriter(printer);
        sslWriter.writeSSLConfiguration(sslConf);
    }

    private void shutdownProcess() throws IOException {
        // Interrupt the reader thread to stop processing
        stdoutReader.interrupt();
        
        process.destroy();
        
        try {
            stdoutReader.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void killServer() throws IOException {
        if (process != null) {
            logger.info("Stopping command channel server process");
            shutdownProcess();
        }
    }
    
    @Override
    public void requestReceived(Request request) {
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
        
        writeResponse(response);
    }

    private void writeResponse(Response response) {
        /*
         * Write out response to command channel server using the
         * following protocol:
         * '<BEGIN RESPONSE>'
         * ResponseType
         * '<END RESPONSE>'
         */
        printer.println(CommandChannelConstants.BEGIN_RESPONSE_TOKEN);
        printer.println(response.getType().name());
        printer.println(CommandChannelConstants.END_RESPONSE_TOKEN);
        printer.flush();
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
        Process startProcess(String[] args) throws IOException {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectError(Redirect.INHERIT);
            return builder.start();
        }
    }
}

