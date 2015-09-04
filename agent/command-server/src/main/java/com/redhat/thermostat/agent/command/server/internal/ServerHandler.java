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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
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

import com.redhat.thermostat.common.command.Message.MessageType;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;

class ServerHandler extends SimpleChannelUpstreamHandler {

    private static final Object STDIO_LOCK = new Object();
    private static final Logger logger = LoggingUtils.getLogger(ServerHandler.class);
    
    private SSLConfiguration sslConf;
    private ResponseParser responseParser;
    private OutputStream outStream;
    
    ServerHandler(SSLConfiguration sslConf) {
        this(sslConf, new ResponseParser(), System.out);
    }
    
    ServerHandler(SSLConfiguration sslConf, ResponseParser responseParser, OutputStream outStream) {
        this.sslConf = sslConf;
        this.responseParser = responseParser;
        this.outStream = outStream;
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
    
    /*
     * 1. Read request from command channel
     * 2. Write request to agent in described form
     * 3. Wait on agent for response (maybe add timeout here)
     * 4. Read response from agent
     * 5. Write response to command channel
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Response response;
        Request request = (Request) e.getMessage();
        String receiverName = request.getReceiver();
        MessageType requestType = request.getType();
        if (requestType == null || receiverName == null) {
            logger.warning("Invalid Request");
            response = new Response(ResponseType.ERROR);
        } else {
            // Reading/writing to agent should be synchronized
            synchronized (STDIO_LOCK) {
                logger.info("Request received: '" + requestType + "' for '" + receiverName + "'");
                try {
                    writeRequest(request);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Failed to write request to agent", ex);
                    response = new Response(ResponseType.ERROR);
                }

                try {
                    response = responseParser.parseResponse(System.in);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Failed to read response from agent", ex);
                    response = new Response(ResponseType.ERROR);
                }
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

    /**
     * Write request using the following protocol:
     * <pre>
     * '&lt;BEGIN REQUEST&gt;'
     * Target Address Host
     * Target Address Port
     * Length of encoded request in bytes
     * Encoded Request (see format: {@link RequestEncoder})
     * '&lt;END REQUEST&gt;'
     * </pre>
     */
    private void writeRequest(Request request) throws IOException {
        DataOutputStream dos = new DataOutputStream(outStream);
        RequestEncoder encoder = new RequestEncoder();
        ChannelBuffer buf = encoder.encode(request);
        ByteBuffer encodedRequest = buf.toByteBuffer();

        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        InetSocketAddress addr = request.getTarget();
        dos.writeUTF(addr.getHostString());
        dos.writeInt(addr.getPort());
        byte[] requestBytes = encodedRequest.array();
        dos.writeInt(requestBytes.length);
        dos.write(requestBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
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

}

