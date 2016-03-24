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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

// Encodes a Request as JSON and sends the result to the agent
class JsonRequestEncoder {

    void encodeRequestAndSend(ByteChannel agentChannel, Request request) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls(); // In case null parameter values are permitted
        Gson gson = builder.create();
        
        JsonObject requestObj = new JsonObject();
        InetSocketAddress addr = request.getTarget();
        requestObj.addProperty(CommandChannelConstants.REQUEST_JSON_HOST, addr.getHostString());
        requestObj.addProperty(CommandChannelConstants.REQUEST_JSON_PORT, addr.getPort());
        
        // CommandChannelRequestDecoder ensures this is a safe cast
        RequestType type = (RequestType) request.getType();
        requestObj.addProperty(CommandChannelConstants.REQUEST_JSON_TYPE, type.name());
        
        JsonObject paramsObj = new JsonObject();
        for (String param : request.getParameterNames()) {
            // Don't allow null parameter names
            if (param == null) {
                throw new IOException("Null parameter names are not allowed");
            }
            String value = request.getParameter(param);
            paramsObj.addProperty(param, value);
        }
        requestObj.add(CommandChannelConstants.REQUEST_JSON_PARAMS, paramsObj);
        JsonObject requestRoot = new JsonObject();
        requestRoot.add(CommandChannelConstants.REQUEST_JSON_TOP, requestObj);
        
        String jsonRequest = gson.toJson(requestRoot);
        byte[] jsonRequestBytes = jsonRequest.getBytes(Charset.forName("UTF-8"));
        
        // Write request
        ByteBuffer buf = ByteBuffer.wrap(jsonRequestBytes);
        int written = agentChannel.write(buf);
        if (written != jsonRequestBytes.length) {
            throw new IOException("Failed to write complete message, wrote " + written + " of " + jsonRequestBytes.length);
        }
    }
}

