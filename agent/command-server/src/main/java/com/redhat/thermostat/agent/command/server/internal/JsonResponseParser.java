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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

// Parses a Response encoded as JSON received from the agent
class JsonResponseParser {
    
    Response parseResponse(IPCMessageChannel agentChannel) throws IOException {
        ByteBuffer buf = agentChannel.readMessage();
        byte[] jsonResponse = new byte[buf.remaining()];
        buf.get(jsonResponse);
                
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonParser parser = new JsonParser();
        
        try {
            // Get root of JsonObject tree
            String jsonResponseString = new String(jsonResponse, Charset.forName("UTF-8"));
            JsonElement rootElement = parser.parse(jsonResponseString);
            requireNonNull(rootElement, "Entire response missing");
            JsonObject rootObj = toJsonObject(rootElement);
            
            JsonElement responseElement = rootObj.get(CommandChannelConstants.RESPONSE_JSON_TOP);
            requireNonNull(responseElement, "Response data missing");
            JsonObject responseObj = toJsonObject(responseElement);
            
            // Get response type
            JsonElement typeElement = responseObj.get(CommandChannelConstants.RESPONSE_JSON_TYPE);
            requireNonNull(typeElement, "Response type missing");
            String typeString = gson.fromJson(typeElement, String.class);
            
            // Get enum value
            ResponseType type;
            try {
                type = ResponseType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid ResponseType: " + typeString);
            }
            
            return new Response(type);
        } catch (JsonParseException e) {
            throw new IOException("Failed to parse response from agent", e);
        }
    }

    private void requireNonNull(JsonElement element, String errorMessage) throws IOException {
        if (element == null || element.isJsonNull()) {
            throw new IOException(errorMessage);
        }
    }
    
    private JsonObject toJsonObject(JsonElement element) throws IOException {
        if (!element.isJsonObject()) {
            throw new IOException("Malformed data received");
        }
        return element.getAsJsonObject();
    }
}
