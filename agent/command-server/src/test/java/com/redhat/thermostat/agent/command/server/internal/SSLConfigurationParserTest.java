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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLConfigurationParserTest {
    
    private SSLConfigurationParser parser;
    private IPCMessageChannel agentChannel;
    
    @Before
    public void setUp() throws Exception {
        parser = new SSLConfigurationParser();
        agentChannel = mock(IPCMessageChannel.class);
    }
    
    @Test
    public void testSuccess() throws Exception {
        JsonObject root = createConfig();
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        
        SSLConfiguration config = parser.parseSSLConfiguration(agentChannel);
        assertEquals("/path/to/keystore", config.getKeystoreFile().getAbsolutePath());
        assertEquals("My Keystore Password", config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }

    @Test
    public void testSuccessNoKeystore() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE, (String) null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        
        SSLConfiguration config = parser.parseSSLConfiguration(agentChannel);
        assertNull(config.getKeystoreFile());
        assertEquals("My Keystore Password", config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }
    
    @Test
    public void testSuccessNoKeystorePass() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS, (String) null);

        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        
        SSLConfiguration config = parser.parseSSLConfiguration(agentChannel);
        assertEquals("/path/to/keystore", config.getKeystoreFile().getAbsolutePath());
        assertNull(config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }

    @Test(expected=IOException.class)
    public void testConfigMissing() throws IOException {
        mockByteChannel(new byte[0]);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBadConfigNotJson() throws IOException {
        mockByteChannel("Not JSON".getBytes(Charset.forName("UTF-8")));
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBadConfigRoot() throws IOException {
        byte[] encoded = toJson(new JsonArray());
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testParamsMissing() throws IOException {
        byte[] encoded = toJson(new JsonObject());
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBadParams() throws IOException {
        JsonObject root = createConfig();
        root.add(CommandChannelConstants.SSL_JSON_ROOT, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testNullParams() throws IOException {
        JsonObject root = createConfig();
        root.add(CommandChannelConstants.SSL_JSON_ROOT, null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testKeystoreFileMissing() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.remove(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testKeystoreFileBad() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.add(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testKeystorePassMissing() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.remove(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testKeystorePassBad() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.add(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testCmdChannelMissing() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.remove(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testCmdChannelBad() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.add(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testCmdChannelNull() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL, (String) null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBackingStorageMissing() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.remove(CommandChannelConstants.SSL_JSON_BACKING_STORAGE);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBackingStorageBad() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.add(CommandChannelConstants.SSL_JSON_BACKING_STORAGE, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testBackingStorageNull() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_BACKING_STORAGE, (String) null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testHostnameVerifyMissing() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.remove(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testHostnameVerifyBad() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.add(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION, new JsonArray());
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    @Test(expected=IOException.class)
    public void testHostnameVerifyNull() throws IOException {
        JsonObject root = createConfig();
        JsonObject params = root.get(CommandChannelConstants.SSL_JSON_ROOT).getAsJsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION, (String) null);
        
        byte[] encoded = toJson(root);
        mockByteChannel(encoded);
        parser.parseSSLConfiguration(agentChannel);
    }
    
    private JsonObject createConfig() {
        JsonObject params = new JsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE, "/path/to/keystore");
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS, "My Keystore Password");
        params.addProperty(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL, true);
        params.addProperty(CommandChannelConstants.SSL_JSON_BACKING_STORAGE, true);
        params.addProperty(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION, false);
        
        JsonObject root = new JsonObject();
        root.add(CommandChannelConstants.SSL_JSON_ROOT, params);
        return root;
    }

    private byte[] toJson(JsonElement element) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonString = gson.toJson(element);
        return jsonString.getBytes(Charset.forName("UTF-8"));
    }

    private void mockByteChannel(final byte[] encoded) throws IOException {
        when(agentChannel.readMessage()).thenReturn(ByteBuffer.wrap(encoded));
    }
}
