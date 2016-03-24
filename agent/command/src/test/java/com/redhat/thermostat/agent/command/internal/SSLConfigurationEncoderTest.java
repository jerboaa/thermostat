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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLConfigurationEncoderTest {
    
    private static final String KEYSTORE_PASS = "My Keystore Password";
    private static final String KEYSTORE_FILE = "/path/to/keystore";
    private SSLConfiguration sslConf;
    
    @Before
    public void setUp() {
        sslConf = mock(SSLConfiguration.class);
        File keystore = new File(KEYSTORE_FILE);
        when(sslConf.getKeystoreFile()).thenReturn(keystore);
        when(sslConf.getKeyStorePassword()).thenReturn(KEYSTORE_PASS);
        when(sslConf.enableForBackingStorage()).thenReturn(true);
        when(sslConf.enableForCmdChannel()).thenReturn(true);
        when(sslConf.disableHostnameVerification()).thenReturn(false);
    }
    
    @Test
    public void testSSLConfig() throws IOException {
        String expected = getJsonString(KEYSTORE_FILE, KEYSTORE_PASS);
        String result = getEncodedSSLConfiguration();
        assertEquals(expected, result);
    }
    
    @Test
    public void testSSLConfigNoKeystoreFile() throws IOException {
        when(sslConf.getKeystoreFile()).thenReturn(null);
        
        String expected = getJsonString(null, KEYSTORE_PASS);
        String result = getEncodedSSLConfiguration();
        assertEquals(expected, result);
    }
    
    @Test
    public void testSSLConfigNoKeystorePass() throws IOException {
        when(sslConf.getKeyStorePassword()).thenReturn(null);
        
        String expected = getJsonString(KEYSTORE_FILE, null);
        String result = getEncodedSSLConfiguration();
        assertEquals(expected, result);
    }

    private String getEncodedSSLConfiguration() throws IOException {
        SSLConfigurationEncoder encoder = new SSLConfigurationEncoder();
        byte[] encoded = encoder.encodeAsJson(sslConf);
        return new String(encoded, Charset.forName("UTF-8"));
    }
    
    private String getJsonString(String keystoreFile, String keystorePass) {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        Gson gson = builder.create();
        
        JsonObject params = new JsonObject();
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE, keystoreFile);
        params.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS, keystorePass);
        params.addProperty(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL, true);
        params.addProperty(CommandChannelConstants.SSL_JSON_BACKING_STORAGE, true);
        params.addProperty(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION, false);
        
        JsonObject sslConfigJson = new JsonObject();
        sslConfigJson.add(CommandChannelConstants.SSL_JSON_ROOT, params);
        return gson.toJson(sslConfigJson);
    }

}
