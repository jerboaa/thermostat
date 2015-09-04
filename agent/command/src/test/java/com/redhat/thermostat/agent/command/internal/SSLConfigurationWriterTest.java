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

package com.redhat.thermostat.agent.command.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLConfigurationWriterTest {
    
    private static final String SSL_OUT = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
            + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
            + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
            + "true\ntrue\nfalse\n"
            + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
    
    private static final String SSL_OUT_NO_KS_FILE = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
            + CommandChannelConstants.KEYSTORE_NULL + "\n"
            + CommandChannelConstants.KEYSTORE_PASS_PREFIX + "My Keystore Password\n"
            + "true\ntrue\nfalse\n"
            + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";
    
    private static final String SSL_OUT_NO_KS_PASS = CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + "\n"
            + CommandChannelConstants.KEYSTORE_FILE_PREFIX + "/path/to/keystore\n"
            + CommandChannelConstants.KEYSTORE_NULL + "\n"
            + "true\ntrue\nfalse\n"
            + CommandChannelConstants.END_SSL_CONFIG_TOKEN + "\n";

    private SSLConfiguration sslConf;
    
    @Before
    public void setUp() {
        sslConf = mock(SSLConfiguration.class);
        File keystore = new File("/path/to/keystore");
        when(sslConf.getKeystoreFile()).thenReturn(keystore);
        when(sslConf.getKeyStorePassword()).thenReturn("My Keystore Password");
        when(sslConf.enableForBackingStorage()).thenReturn(true);
        when(sslConf.enableForCmdChannel()).thenReturn(true);
        when(sslConf.disableHostnameVerification()).thenReturn(false);
    }
    
    @Test
    public void testSSLConfig() throws IOException {
        String result = getSSLConfiguration();
        assertEquals(SSL_OUT, result);
    }
    
    @Test
    public void testSSLConfigNoKeystoreFile() throws IOException {
        when(sslConf.getKeystoreFile()).thenReturn(null);
        
        String result = getSSLConfiguration();
        assertEquals(SSL_OUT_NO_KS_FILE, result);
    }
    
    @Test
    public void testSSLConfigNoKeystorePass() throws IOException {
        when(sslConf.getKeyStorePassword()).thenReturn(null);
        
        String result = getSSLConfiguration();
        assertEquals(SSL_OUT_NO_KS_PASS, result);
    }

    private String getSSLConfiguration() {
        StringWriter buf = new StringWriter();
        final PrintWriter printer = new PrintWriter(buf);
        
        SSLConfigurationWriter writer = new SSLConfigurationWriter(printer);
        writer.writeSSLConfiguration(sslConf);
        printer.close();
        return buf.getBuffer().toString();
    }

}
