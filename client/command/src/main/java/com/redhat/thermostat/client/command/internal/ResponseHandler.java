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

package com.redhat.thermostat.client.command.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ResponseHandler extends SimpleChannelInboundHandler<Response> {

    private static final Logger logger = LoggingUtils
            .getLogger(ResponseHandler.class);

    private final Request request;

    ResponseHandler(Request request) {
        this.request = request;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO when response has support for parameters, provide the exception as well.
        logger.log(Level.WARNING, "exception caught: ", cause);
        Response response = new Response(ResponseType.ERROR);
        notifyListeners(response);
        // Close broken channel. This is important, please keep!
        Future<Void> future = ctx.channel().close();
        future.addListener(new GenericFutureListener<Future<? super Void>>() {

            @Override
            public void operationComplete(Future<? super Void> future)
                    throws Exception {
                logger.log(Level.FINEST, "channel closed");
            }
        });
        ctx.close();
    }

    private void notifyListeners(Response response) {
        for (RequestResponseListener listener : request.getListeners()) {
            listener.fireComplete(request, response);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response)
            throws Exception {
        ctx.pipeline().remove(this);
        logger.info((response).getType().toString());
        notifyListeners(response);
        ctx.close();
    }
}

