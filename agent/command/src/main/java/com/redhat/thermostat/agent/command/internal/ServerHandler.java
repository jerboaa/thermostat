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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Message.MessageType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;

class ServerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggingUtils.getLogger(ServerHandler.class);
    private ReceiverRegistry receivers;
    private SSLConfiguration sslConf;
    private StorageGetter storageGetter;

    public ServerHandler(ReceiverRegistry receivers, SSLConfiguration sslConf) {
        this(receivers, sslConf, new StorageGetter());
    }

    /** For testing only */
    ServerHandler(ReceiverRegistry receivers, SSLConfiguration sslConf, StorageGetter getter) {
        this.storageGetter = getter;
        this.receivers = receivers;
        this.sslConf = sslConf;
    }

    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            logger.log(Level.FINEST, e.toString());
        }
        super.handleUpstream(ctx, e);
    }
    
    @Override
    public void channelConnected(
            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (sslConf.enableForCmdChannel()) {
            // Get the SslHandler in the current pipeline.
            // We added it in ConfigurationServerContext$ServerPipelineFactory.
            final SslHandler sslHandler = ctx.getPipeline().get(
                    SslHandler.class);

            // Get notified when SSL handshake is done.
            ChannelFuture handshakeFuture = sslHandler.handshake();
            handshakeFuture.addListener(new SSLHandshakeDoneListener());
        }
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Request request = (Request) e.getMessage();
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
        Channel channel = ctx.getChannel();
        if (channel.isConnected()) {
            logger.info("Sending response: " + response.getType().toString());
            ChannelFuture f = channel.write(response);
            f.addListener(ChannelFutureListener.CLOSE);
        } else {
            logger.warning("Channel not connected.");
        }
    }

    private boolean authenticateRequestIfNecessary(Request request) {
        Storage storage = storageGetter.get();
        if (storage instanceof SecureStorage) {
            boolean authenticatedRequest = authenticateRequest(request, (SecureStorage) storage);
            if (authenticatedRequest) {
                logger.finest("Authentication and authorization for request " + request + " succeeded!");
            } else {
                logger.finest("Request " + request + " failed to authenticate or authorize");
            }
            return authenticatedRequest;
        } else {
            return true;
        }
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }
    
    /*
     * Only registered if SSL is enabled
     */
    static final class SSLHandshakeDoneListener implements ChannelFutureListener {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                logger.log(Level.FINE, "Finished SSL handshake.");
            } else {
                logger.log(Level.WARNING, "SSL handshake failed!");
                future.getChannel().close();
            }
        }
    }

    /** for testing only */
    static class StorageGetter {
        public Storage get() {
            BundleContext bCtx = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<Storage> storageRef = bCtx.getServiceReference(Storage.class);
            // FIXME there should be a matching unget() somewhere to release the reference
            Storage storage = (Storage) bCtx.getService(storageRef);
            return storage;
        }
    }
}

