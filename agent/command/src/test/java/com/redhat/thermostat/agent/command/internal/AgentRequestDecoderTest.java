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

package com.redhat.thermostat.agent.command.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

public class AgentRequestDecoderTest {

    private AgentRequestDecoder decoder;

    @Before
    public void setUp() throws Exception {
        decoder = new AgentRequestDecoder();
    }
    
    @Test(expected=IOException.class)
    public void testEmptyString() throws IOException {
        decoder.decodeRequest(new byte[0]);
    }
    
    @Test(expected=IOException.class)
    public void testNotJson() throws IOException {
        decoder.decodeRequest("Not JSON".getBytes(Charset.forName("UTF-8")));
    }
    
    @Test(expected=IOException.class)
    public void testNotJsonObject() throws IOException {
        byte[] json = toJson(new JsonArray());
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testRequestMissing() throws IOException {
        byte[] json = toJson(new JsonObject());
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testBadRequestObject() throws IOException {
        JsonObject root = createRequest();
        root.add(CommandChannelConstants.REQUEST_JSON_TOP, new JsonArray());
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testNullRequestObject() throws IOException {
        JsonObject root = createRequest();
        root.add(CommandChannelConstants.REQUEST_JSON_TOP, null);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testRequestTypeMissing() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.remove(CommandChannelConstants.REQUEST_JSON_TYPE);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testRequestTypeNotString() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.add(CommandChannelConstants.REQUEST_JSON_TYPE, new JsonArray());
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testRequestTypeNull() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_TYPE, (String) null);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testRequestTypeNotEnum() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_TYPE, "Not a RequestType");
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }

    @Test(expected=IOException.class)
    public void testHostnameMissing() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.remove(CommandChannelConstants.REQUEST_JSON_HOST);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testHostnameNotString() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.add(CommandChannelConstants.REQUEST_JSON_HOST, new JsonArray());
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testHostnameNull() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_HOST, (String) null);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testPortMissing() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.remove(CommandChannelConstants.REQUEST_JSON_PORT);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testPortNotInt() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_PORT, "Not a Port");
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testPortNull() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_PORT, (String) null);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testParamsMissing() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.remove(CommandChannelConstants.REQUEST_JSON_PARAMS);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testBadParams() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.add(CommandChannelConstants.REQUEST_JSON_PARAMS, new JsonArray());
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testParamsNull() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        jsonRequest.add(CommandChannelConstants.REQUEST_JSON_PARAMS, null);
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test(expected=IOException.class)
    public void testParamsValueNotString() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        JsonObject params = jsonRequest.get(CommandChannelConstants.REQUEST_JSON_PARAMS).getAsJsonObject();
        params.add("name", new JsonArray());
        
        byte[] json = toJson(root);
        decoder.decodeRequest(json);
    }
    
    @Test
    public void testSuccess() throws IOException {
        JsonObject root = createRequest();
        byte[] json = toJson(root);
        Request request = decoder.decodeRequest(json);
        
        assertEquals(RequestType.NO_RESPONSE_EXPECTED, request.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12000), request.getTarget());
        
        Collection<String> parameterNames = request.getParameterNames();
        assertEquals(2, parameterNames.size());
        assertTrue(parameterNames.contains("name1"));
        assertTrue(parameterNames.contains("name2"));
        assertEquals("value1", request.getParameter("name1"));
        assertEquals("value2", request.getParameter("name2"));
    }
    
    @Test
    public void testSuccessNullValue() throws IOException {
        JsonObject root = createRequest();
        JsonObject jsonRequest = root.get(CommandChannelConstants.REQUEST_JSON_TOP).getAsJsonObject();
        JsonObject params = jsonRequest.get(CommandChannelConstants.REQUEST_JSON_PARAMS).getAsJsonObject();
        params.addProperty("name1", (String) null);

        byte[] json = toJson(root);
        Request request = decoder.decodeRequest(json);
        
        assertEquals(RequestType.NO_RESPONSE_EXPECTED, request.getType());
        assertEquals(new InetSocketAddress("127.0.0.1", 12000), request.getTarget());
        
        Collection<String> parameterNames = request.getParameterNames();
        assertEquals(2, parameterNames.size());
        assertTrue(parameterNames.contains("name1"));
        assertTrue(parameterNames.contains("name2"));
        assertNull(request.getParameter("name1"));
        assertEquals("value2", request.getParameter("name2"));
    }
    
    private JsonObject createRequest() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_TYPE, RequestType.NO_RESPONSE_EXPECTED.name());
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_HOST, "127.0.0.1");
        jsonRequest.addProperty(CommandChannelConstants.REQUEST_JSON_PORT, "12000");
        
        JsonObject params = new JsonObject();
        params.addProperty("name1", "value1");
        params.addProperty("name2", "value2");
        jsonRequest.add(CommandChannelConstants.REQUEST_JSON_PARAMS, params);
        
        JsonObject root = new JsonObject();
        root.add(CommandChannelConstants.REQUEST_JSON_TOP, jsonRequest);
        return root;
    }
    
    private byte[] toJson(JsonElement element) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonString = gson.toJson(element);
        return jsonString.getBytes(Charset.forName("UTF-8"));
    }

}
