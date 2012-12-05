/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.web.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.junit.Test;

import com.redhat.thermostat.web.client.internal.CustomX509TrustManager;

/**
 * This trust manager test uses files in src/test/resources. Files are as
 * follows:
 * 
 * empty.keystore => emtpy file (not a real keystore file)
 * 
 * ca.crt => a openssl generated X509 certificate (representing a custom CA)
 * 
 * test_ca.keystore => a Java keystore file with ca.crt imported. Uses keystore
 * password "testpassword". Used command sequence to generate this file:
 * 
 * $ keytool -genkey -alias com.redhat -keyalg RSA -keystore test_ca.keystore -keysize 2048
 * $ keytool -import -trustcacerts -alias root -file ca.crt -keystore test_ca.keystore
 * 
 * 
 */
public class CustomX509TrustManagerTest {

    @Test
    public void testEmptyDefaultOur() {
        X509TrustManager tm = new CustomX509TrustManager(
                (X509TrustManager) null, (X509TrustManager) null);
        assertEquals(0, tm.getAcceptedIssuers().length);
        try {
            tm.checkClientTrusted(null, null);
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
        try {
            tm.checkServerTrusted(null, null);
            fail("Expected exception since there aren't any trust managers available");
        } catch (CertificateException e) {
            // pass
        }
    }

    @Test
    public void testLoadEmptyTrustStoreForOur() {
        File emptyKeyStore = new File(this.getClass()
                .getResource("/empty.keystore").getFile());
        X509TrustManager tm = new CustomX509TrustManager(null, emptyKeyStore,
                "");
        assertEquals(0, tm.getAcceptedIssuers().length);
        try {
            tm.checkClientTrusted(null, null);
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
        try {
            X509Certificate dummyCert = mock(X509Certificate.class);
            tm.checkServerTrusted(new X509Certificate[] { dummyCert }, "RSA");
            fail("Expected exception since there aren't any trust managers available");
        } catch (CertificateException e) {
            // pass
        }
    }

    @Test
    public void testLoadEmptyTrustStoreForOurDefaultAsUsual() throws Exception {
        File emptyKeyStore = new File(this.getClass()
                .getResource("/empty.keystore").getFile());
        X509TrustManager tm = new CustomX509TrustManager(emptyKeyStore, "");
        // Default list should not be empty
        assertTrue(tm.getAcceptedIssuers().length > 0);
        try {
            tm.checkClientTrusted(null, null);
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }
    
    @Test
    public void canGetCustomCaCertFromOurTrustManager() {
        File ourKeyStore = new File(this.getClass()
                .getResource("/test_ca.keystore").getFile());
        X509TrustManager tm = new CustomX509TrustManager((X509TrustManager)null, ourKeyStore, "testpassword");
        // keystore contains private key of itself + imported CA cert
        assertEquals(2, tm.getAcceptedIssuers().length);
        String issuerNameCustomCA = "1.2.840.113549.1.9.1=#16126a6572626f6161407265646861742e636f6d,CN=test.example.com,O=Red Hat Inc.,L=Saalfelden,ST=Salzburg,C=AT";
        String issuerNameKeystoreCA = "CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown";
        assertEquals(issuerNameCustomCA, tm.getAcceptedIssuers()[0]
                .getIssuerX500Principal().getName());
        assertEquals(issuerNameKeystoreCA, tm.getAcceptedIssuers()[1]
                .getIssuerX500Principal().getName());
    }

}
