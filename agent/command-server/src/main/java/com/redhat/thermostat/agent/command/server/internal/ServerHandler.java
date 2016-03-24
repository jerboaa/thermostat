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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Message.MessageType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

class ServerHandler extends SimpleChannelInboundHandler<Request> {

    private static final Logger logger = LoggingUtils.getLogger(ServerHandler.class);
    
    private final SSLConfiguration sslConf;
    private final ByteChannel agentChannel;
    private final JsonRequestEncoder requestEncoder;
    private final JsonResponseParser responseParser;
    
    ServerHandler(SSLConfiguration sslConf, ByteChannel agentChannel) {
        this(sslConf, agentChannel, new JsonRequestEncoder(), new JsonResponseParser());
    }
    
    ServerHandler(SSLConfiguration sslConf, ByteChannel agentChannel, JsonRequestEncoder requestEncoder, JsonResponseParser responseParser) {
        this.sslConf = sslConf;
        this.agentChannel = agentChannel;
        this.requestEncoder = requestEncoder;
        this.responseParser = responseParser;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINEST, "Channel active!");
        if (sslConf.enableForCmdChannel()) {
            // Get the SslHandler in the current pipeline.
            // We added it in ConfigurationServerContext$ServerPipelineFactory.
            final SslHandler sslHandler = ctx.pipeline().get(
                    SslHandler.class);

            // Get notified when SSL handshake is done.
            Future<Channel> handshakeFuture = sslHandler.handshakeFuture();
            handshakeFuture.addListener(new SSLHandshakeDoneListener(ctx));
        }
    }
    
    /*
     * 1. Read request from command channel
     * 2. Write request to agent encoded as JSON
     * 3. Wait on agent for response (maybe add timeout here)
     * 4. Read JSON-encoded response from agent
     * 5. Write response to command channel
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request)
            throws Exception {
        Response response;
        String receiverName = request.getReceiver();
        MessageType requestType = request.getType();
        if (requestType == null || receiverName == null) {
            logger.warning("Invalid Request");
            response = new Response(ResponseType.ERROR);
        } else {
            // Reading/writing to agent should be synchronized
            synchronized (agentChannel) {
                logger.info("Request received: '" + requestType + "' for '" + receiverName + "'");
                try {
                    // Ensure channel is still open
                    if (!agentChannel.isOpen()) {
                        throw new IOException("Communication channel with agent is closed");
                    }
                    requestEncoder.encodeRequestAndSend(agentChannel, request);
                    response = responseParser.parseResponse(agentChannel);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Failed to communicate with agent", ex);
                    response = new Response(ResponseType.ERROR);
                }
            }
        }
        
        Channel channel = ctx.channel();
        logger.info("Sending response: " + response.getType().toString());
        channel.pipeline().writeAndFlush(response);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
        ctx.close();
    }
    
    /*
     * Only registered if SSL is enabled
     */
    static final class SSLHandshakeDoneListener implements GenericFutureListener<Future<Channel>> {

        private final ChannelHandlerContext ctx;
        
        SSLHandshakeDoneListener(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public void operationComplete(Future<Channel> future) throws Exception {
            if (future.isSuccess()) {
                logger.log(Level.FINE, "Finished SSL handshake.");
            } else {
                logger.log(Level.WARNING, "SSL handshake failed!");
                ctx.close();
            }
        }
    }

}

