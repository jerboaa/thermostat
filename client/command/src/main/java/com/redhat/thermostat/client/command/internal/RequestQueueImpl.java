/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.ssl.SslHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.ssl.SSLKeystoreConfiguration;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;

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
        authenticateRequest(request);
        queue.add(request);
    }

    private void authenticateRequest(Request request) {
        BundleContext bCtx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference storageRef = bCtx.getServiceReference(Storage.class.getName());
        Storage storage = (Storage) bCtx.getService(storageRef);
        if (storage instanceof SecureStorage) {
            authenticateRequest(request, (SecureStorage) storage);
        }
    }

    private void authenticateRequest(Request request, SecureStorage storage) {
        try {
            AuthToken token = storage.generateToken();
            request.setParameter(Request.CLIENT_TOKEN, Base64.encodeBase64String(token.getClientToken()));
            request.setParameter(Request.AUTH_TOKEN, Base64.encodeBase64String(token.getToken()));
        } catch (StorageException ex) {
            fireComplete(request, new Response(ResponseType.AUTH_FAILED));
        }
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
                ChannelFuture f = ((ClientBootstrap) ctx.getBootstrap()).connect(request.getTarget());
                f.awaitUninterruptibly();
                if (f.isSuccess()) {
                	Channel c = f.getChannel();
                	ChannelPipeline pipeline = c.getPipeline();
                	if (SSLKeystoreConfiguration.shouldSSLEnableCmdChannel()) {
                	    doSSLHandShake(pipeline, request);
                	}
                	pipeline.addLast("responseHandler", new ResponseHandler(request));
                	c.write(request);
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
        ChannelFuture future = sslHandler.handshake();
        
        // Register a future listener, since it gives us a way to
        // report an error on client side and to perform (optional) host name verification.
        boolean performHostnameCheck = !SSLKeystoreConfiguration.disableHostnameVerification();
        future.addListener(new SSLHandshakeFinishedListener(request, performHostnameCheck, sslHandler, this));
    }
}

