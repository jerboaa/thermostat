/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.redhat.thermostat.agent.command.server.internal.SSLConfigurationParser;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLConfigurationParserTest {
    
    public void testSuccess() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        SSLConfiguration config = parseTestData(testData);
        assertEquals("/path/to/keystore", config.getKeystoreFile().getAbsolutePath());
        assertEquals("My Keystore Password", config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }
    
    public void testSuccessNoKeystore() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_NULL + "\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        SSLConfiguration config = parseTestData(testData);
        assertEquals(null, config.getKeystoreFile());
        assertEquals("My Keystore Password", config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }
    
    public void testSuccessNoKeystorePass() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_NULL + "\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        SSLConfiguration config = parseTestData(testData);
        assertEquals("/path/to/keystore", config.getKeystoreFile().getAbsolutePath());
        assertEquals(null, config.getKeyStorePassword());
        assertEquals(true, config.enableForCmdChannel());
        assertEquals(true, config.enableForBackingStorage());
        assertEquals(false, config.disableHostnameVerification());
    }

    @Test(expected=IOException.class)
    public void testEOFStartToken() throws IOException {
        SSLConfigurationParser parser = new SSLConfigurationParser();
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        parser.parse(bais);
    }

    @Test(expected=IOException.class)
    public void testBadStartToken() throws IOException {
        final String testData = "<BEGIN TLS CONFIG>\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFKeystoreFileToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadKeystoreFileToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + "KSFOOL:/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFKeystorePassToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadKeystorePassToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + "KSPSWD:My Keystore Password\n"
                + "true\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFEnableForCommandChannel() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadEnableForCommandChannel() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "maybe\ntrue\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFEnableForBackingStorage() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadEnableForBackingStorage() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\nmaybe\nfalse\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFDisableHostnameVerification() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadDisableHostnameVerification() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\nmaybe\n"
                + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testEOFEndToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\ntrue\n";
        parseTestData(testData);
    }
    
    @Test(expected=IOException.class)
    public void testBadEndToken() throws IOException {
        final String testData = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
                + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
                + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
                + "true\ntrue\ntrue\n"
                + "<END TLS CONFIG>\n";
        parseTestData(testData);
    }

    private SSLConfiguration parseTestData(String testData) throws IOException {
        SSLConfigurationParser parser = new SSLConfigurationParser();
        ByteArrayInputStream bais = new ByteArrayInputStream(testData.getBytes());
        return parser.parse(bais);
    }
}
