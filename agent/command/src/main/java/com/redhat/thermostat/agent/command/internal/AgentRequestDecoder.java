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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.utils.LoggingUtils;

class AgentRequestDecoder {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentRequestDecoder.class);

    Request decodeRequest(byte[] jsonRequest) throws IOException {
        logger.log(Level.FINEST, "Agent: decoding request received from command channel");
        
        String jsonRequestString = new String(jsonRequest, Charset.forName("UTF-8"));
        Request request;
        try {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            JsonParser parser = new JsonParser();
            JsonElement parsed = parser.parse(jsonRequestString);
            
            // Get root of JsonObject tree
            JsonObject root = toJsonObject(parsed);
            JsonElement requestElement = root.get(CommandChannelConstants.REQUEST_JSON_TOP);
            requireNonJsonNull(requestElement, "Request data missing");
            JsonObject requestObj = toJsonObject(requestElement);
            
            // Create Request
            RequestType requestType = parseRequestType(gson, requestObj);
            InetSocketAddress target = parseTargetAddress(gson, requestObj);
            request = new Request(requestType, target);
            
            // Add parameters to Request
            JsonElement paramsElement = requestObj.get(CommandChannelConstants.REQUEST_JSON_PARAMS);
            requireNonJsonNull(paramsElement, "Request parameters missing");
            JsonObject paramsObj = toJsonObject(paramsElement);
            for (Map.Entry<String, JsonElement> param : paramsObj.entrySet()) {
                String paramName = param.getKey();
                
                JsonElement value = param.getValue();
                // Parameter value should be a string (JsonPrimitive) or null (JsonNull)
                if (!value.isJsonPrimitive() && !value.isJsonNull()) {
                    throw new IOException("Malformed parameter value");
                }
                String paramValue = gson.fromJson(value, String.class);
                
                request.setParameter(paramName, paramValue);
            }
        } catch (JsonParseException e) {
            throw new IOException("Failed to parse request", e);
        }
        
        return request;
    }

    private RequestType parseRequestType(Gson gson, JsonObject requestObj) throws IOException {
        JsonElement requestTypeObj = requestObj.get(CommandChannelConstants.REQUEST_JSON_TYPE);
        requireNonJsonNull(requestTypeObj, "Request type missing");
        String requestTypeString = gson.fromJson(requestTypeObj, String.class);
        try {
            return RequestType.valueOf(requestTypeString);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid request type: " + requestTypeString);
        }
    }

    private InetSocketAddress parseTargetAddress(Gson gson, JsonObject requestObj) throws IOException {
        JsonElement hostnameObj = requestObj.get(CommandChannelConstants.REQUEST_JSON_HOST);
        requireNonJsonNull(hostnameObj, "Target hostname missing");
        String hostname = gson.fromJson(hostnameObj, String.class);
        
        JsonElement portObj = requestObj.get(CommandChannelConstants.REQUEST_JSON_PORT);
        requireNonJsonNull(portObj, "Target port number missing");
        int port = gson.fromJson(portObj, int.class);
        
        // Ensure address is a valid one
        try {
            return new InetSocketAddress(hostname, port);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid target address");
        }
    }
    
    private JsonObject toJsonObject(JsonElement element) throws IOException {
        if (!element.isJsonObject()) {
            throw new IOException("Malformed data received");
        }
        return element.getAsJsonObject();
    }

    private void requireNonJsonNull(JsonElement element, String errorMessage) throws IOException {
        if (element == null || element.isJsonNull()) {
            throw new IOException(errorMessage);
        }
    }
    
}

