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

package com.redhat.thermostat.client.command.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

class RequestQueueImpl implements RequestQueue {

    private static final Logger logger = LoggingUtils.getLogger(RequestQueueImpl.class);
    private final BlockingQueue<Request> queue;
    private final ConfigurationRequestContext ctx;
    private volatile boolean processing;
    private Thread runningThread;

    RequestQueueImpl(ConfigurationRequestContext ctx) {
        processing = false;
        this.ctx = ctx;
        queue = new ArrayBlockingQueue<Request>(16, true);
    }

    @Override
    public void putRequest(Request request) {
        assertValidRequest(request);

        // Only enqueue request if we've successfully authenticated
        if (authenticateRequest(request)) {
            queue.add(request);
        }
    }

    private void assertValidRequest(Request request) {
        if (request.getReceiver() == null) {
            throw new AssertionError("The receiver for a Request must not be null");
        }
        if (request.getTarget() == null) {
            throw new AssertionError("The target for a Request must not be null");
        }
    }

    private boolean authenticateRequest(Request request) {
        boolean result = true; // Successful by default, unless storage is secure
        BundleContext bCtx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference storageRef = bCtx.getServiceReference(Storage.class.getName());
        Storage storage = (Storage) bCtx.getService(storageRef);
        if (storage instanceof SecureStorage) {
            result = authenticateRequest(request, (SecureStorage) storage);
        }
        return result;
    }

    private boolean authenticateRequest(Request request, SecureStorage storage) {
        boolean result = false; // Successful only if generateToken succeeds
        try {
            String actionName = request.getParameter(Request.ACTION);
            // actionName must not be null here.
            // This is checked in generateToken.
            AuthToken token = storage.generateToken(actionName);
            request.setParameter(Request.CLIENT_TOKEN, Base64.encodeBase64String(token.getClientToken()));
            request.setParameter(Request.AUTH_TOKEN, Base64.encodeBase64String(token.getToken()));
            result = true;
        } catch (StorageException ex) {
            logger.log(Level.WARNING, "Authentication failed", ex);
            fireComplete(request, new Response(ResponseType.AUTH_FAILED));
        }
        return result;
    }

    synchronized void startProcessingRequests() {
        if (!running()) {
            processing = true;
            new QueueRunner().start();
        }
    }

    synchronized void stopProcessingRequests() {
        if (running()) {
            processing = false;
            runningThread.interrupt();
            runningThread = null;
        }
    }

    private boolean running() {
        return runningThread != null && runningThread.isAlive();
    }

    private class QueueRunner extends Thread {

        @Override
        public void run() {
            runningThread = Thread.currentThread();
            while (processing) {
                Request request = null;
                try {
                    // This will block until available (or interrupted).
                    request = queue.take();
                } catch (InterruptedException e) {
                    if (Thread.interrupted()) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (request == null) {
                    break;
                }
                ChannelFuture f = ctx.getBootstrap().connect(request.getTarget()).syncUninterruptibly();
                if (f.isSuccess()) {
                	Channel c = f.channel();
                	ChannelPipeline pipeline = c.pipeline();
                	if (ctx.getSSLConfiguration().enableForCmdChannel()) {
                	    doSSLHandShake(pipeline, request);
                	}
                	pipeline.addLast("responseHandler", new ResponseHandler(request));
                	pipeline.writeAndFlush(request);
                } else {
                	Response response  = new Response(ResponseType.ERROR);
                	fireComplete(request, response);
                }
            }
        }

    }
    
    void fireComplete(Request request, Response response) {
        // TODO add more information once Response supports parameters.
        for (RequestResponseListener listener : request.getListeners()) {
            listener.fireComplete(request, response);
        }
    }

    private void doSSLHandShake(ChannelPipeline pipeline, Request request) {
        // Get the SslHandler from the pipeline
        // which was added in ConfigurationRequestContext$ClientPipelineFactory
        SslHandler sslHandler = pipeline.get(SslHandler.class);
        
        logger.log(Level.FINE, "Starting SSL handshake");
        // Begin handshake.
        Future<Channel> handshakeDoneFuture = sslHandler.handshakeFuture();
        
        // Register a future listener, since it gives us a way to
        // report an error on client side and to perform (optional) host name verification.
        boolean performHostnameCheck = !ctx.getSSLConfiguration().disableHostnameVerification();
        handshakeDoneFuture.addListener(new SSLHandshakeFinishedListener(request, performHostnameCheck, sslHandler, this));
    }
    
    /*
     * For testing purposes only.
     */
    BlockingQueue<Request> getQueue() {
        return queue;
    }
}

