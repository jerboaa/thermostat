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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.File;
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
import com.redhat.thermostat.shared.config.SSLConfiguration;

class SSLConfigurationParser {
    
    SSLConfiguration parseSSLConfiguration(IPCMessageChannel channel) throws IOException {
        ByteBuffer buf = channel.readMessage();
        byte[] jsonSslConf = new byte[buf.remaining()];
        buf.get(jsonSslConf);
        
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonParser parser = new JsonParser();
        
        try {
            String jsonSslConfString = new String(jsonSslConf, Charset.forName("UTF-8"));
            JsonElement parsed = parser.parse(jsonSslConfString);
            requireNonNull(parsed, "Entire SSL configuration missing");
            JsonObject root = toJsonObject(parsed);
            
            JsonElement sslConfigElement = root.get(CommandChannelConstants.SSL_JSON_ROOT);
            requireNonNull(sslConfigElement, "SSL configuration parameters missing");
            JsonObject sslConfigObj = toJsonObject(sslConfigElement);
            
            // Construct SSLConfiguration from parameters
            String keystorePath = getStringOrNull(gson, sslConfigObj, CommandChannelConstants.SSL_JSON_KEYSTORE_FILE);
            File keystoreFile = null;
            if (keystorePath != null) {
                keystoreFile = new File(keystorePath);
            }
            String keystorePass = getStringOrNull(gson, sslConfigObj, CommandChannelConstants.SSL_JSON_KEYSTORE_PASS);
            boolean cmdChannel = getBoolean(gson, sslConfigObj, CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL);
            boolean backingStorage = getBoolean(gson, sslConfigObj, CommandChannelConstants.SSL_JSON_BACKING_STORAGE);
            boolean disableVerification = getBoolean(gson, sslConfigObj, CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION);
            
            return new CommandChannelSSLConfiguration(keystoreFile, keystorePass, cmdChannel, 
                    backingStorage, disableVerification);
        } catch (JsonParseException e) {
            throw new IOException("Invalid encoded SSL configuration", e);
        }
    }
    
    private String getStringOrNull(Gson gson, JsonObject sslConfigObj, String member) throws IOException {
        String value = null;
        // May be null value, but parameter must be specified
        if (!sslConfigObj.has(member)) {
            throw new IOException(member + " parameter missing");
        }
        JsonElement element = sslConfigObj.get(member);
        if (element != null) {
            value = gson.fromJson(element, String.class);
        }
        return value;
    }

    private boolean getBoolean(Gson gson, JsonObject sslConfigObj, String member) throws IOException {
        JsonElement element = sslConfigObj.get(member);
        requireNonNull(element, member + " parameter missing");
        boolean value = gson.fromJson(element, Boolean.class);
        return value;
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
