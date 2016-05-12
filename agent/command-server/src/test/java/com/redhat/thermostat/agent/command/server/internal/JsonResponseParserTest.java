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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class JsonResponseParserTest {
    
    private ByteChannel agentChannel;
    private JsonResponseParser parser;

    @Before
    public void setUp() {
        agentChannel = mock(ByteChannel.class);
        parser = new JsonResponseParser();
    }

    @Test
    public void testSuccess() throws IOException {
        JsonObject jsonResponse = createResponse();
        byte[] encoded = toJson(jsonResponse);
        mockByteChannel(encoded);
        
        Response response = parser.parseResponse(agentChannel);
        assertEquals(ResponseType.OK, response.getType());
    }
    
    @Test(expected=IOException.class)
    public void testEmpty() throws IOException {
        mockByteChannel(new byte[0]);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testNotJson() throws IOException {
        mockByteChannel("Not JSON".getBytes(Charset.forName("UTF-8")));
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBadResponseRoot() throws IOException {
        byte[] encoded = toJson(new JsonArray());
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testParamsMissing() throws IOException {
        byte[] encoded = toJson(new JsonObject());
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBadParams() throws IOException {
        JsonObject jsonResponse = createResponse();
        jsonResponse.add(CommandChannelConstants.RESPONSE_JSON_TOP, new JsonArray());
        
        byte[] encoded = toJson(new JsonObject());
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testNullParams() throws IOException {
        JsonObject jsonResponse = createResponse();
        jsonResponse.add(CommandChannelConstants.RESPONSE_JSON_TOP, null);
        
        byte[] encoded = toJson(new JsonObject());
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testResponseTypeMissing() throws IOException {
        JsonObject root = createResponse();
        JsonObject jsonResponse = root.get(CommandChannelConstants.RESPONSE_JSON_TOP).getAsJsonObject();
        jsonResponse.remove(CommandChannelConstants.RESPONSE_JSON_TYPE);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testResponseTypeNotString() throws IOException {
        JsonObject root = createResponse();
        JsonObject jsonResponse = root.get(CommandChannelConstants.RESPONSE_JSON_TOP).getAsJsonObject();
        jsonResponse.add(CommandChannelConstants.RESPONSE_JSON_TYPE, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testResponseTypeNull() throws IOException {
        JsonObject root = createResponse();
        JsonObject jsonResponse = root.get(CommandChannelConstants.RESPONSE_JSON_TOP).getAsJsonObject();
        jsonResponse.addProperty(CommandChannelConstants.RESPONSE_JSON_TYPE, (String) null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testResponseTypeNotEnum() throws IOException {
        JsonObject root = createResponse();
        JsonObject jsonResponse = root.get(CommandChannelConstants.RESPONSE_JSON_TOP).getAsJsonObject();
        jsonResponse.addProperty(CommandChannelConstants.RESPONSE_JSON_TYPE, "Not a ResponseType");
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseResponse(agentChannel);
    }

    private JsonObject createResponse() {
        JsonObject params = new JsonObject();
        params.addProperty(CommandChannelConstants.RESPONSE_JSON_TYPE, ResponseType.OK.name());
        JsonObject root = new JsonObject();
        root.add(CommandChannelConstants.RESPONSE_JSON_TOP, params);
        return root;
    }
    
    private byte[] toJson(JsonElement element) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonString = gson.toJson(element);
        return jsonString.getBytes(Charset.forName("UTF-8"));
    }

    private void mockByteChannel(final byte[] encoded) throws IOException {
        when(agentChannel.read(any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
                buf.put(encoded);
                buf.flip();
                return encoded.length;
            }
        });
    }

}
