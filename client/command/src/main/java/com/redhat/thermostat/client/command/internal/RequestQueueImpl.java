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

package com.redhat.thermostat.client.command.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;

class RequestQueueImpl implements RequestQueue {

    private final BlockingQueue<Request> queue;
    private final ConfigurationRequestContext ctx;
    private boolean processing;

    RequestQueueImpl(ConfigurationRequestContext ctx) {
        processing = false;
        this.ctx = ctx;
        queue = new ArrayBlockingQueue<Request>(16, true);
    }

    @Override
    public void putRequest(Request request) {
        queue.add(request);
    }

    void startProcessingRequests() {
        if (!processing) {
            processing = true;
            new QueueRunner().start();
        }
    }

    void stopProcessingRequests() {
        processing = false;
    }

    private class QueueRunner extends Thread {

        @Override
        public void run() {
            while (processing) {
                Request request = null;
                try {
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
                Channel c = f.getChannel();
                c.getPipeline().addLast("responseHandler", new ResponseHandler(request));
                c.write(request);
            }
        }
    }
}
