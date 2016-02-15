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

package com.redhat.thermostat.common.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.internal.TrustManagerFactory;
import com.redhat.thermostat.shared.config.SSLConfiguration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SSLContext.class, KeyManagerFactory.class, javax.net.ssl.TrustManagerFactory.class })
public class SSLContextFactoryTest {

    /*
     * cmdChanServer.keystore is a keystore converted from openssl. It contains
     * key material which was signed by ca.crt. More information as to how to
     * create such a file here (first create server.crt => convert it to java
     * keystore format):
     * http://icedtea.classpath.org/wiki/Thermostat/DevDeployWarInTomcatNotes
     * 
     * Unfortunately, powermock messes up the KeyManagerFactory. We can only
     * verify that proper methods are called.
     */
    @Test
    public void verifySetsUpServerContextWithProperKeyMaterial()
            throws Exception {
        File keystoreFile = new File(this.getClass()
                .getResource("/cmdChanServer.keystore").getFile());

        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        when(sslConf.getKeystoreFile()).thenReturn(
                keystoreFile);
        when(sslConf.getKeyStorePassword()).thenReturn(
                "testpassword");

        PowerMockito.mockStatic(SSLContext.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2", "SunJSSE")).thenReturn(context);
        ArgumentCaptor<KeyManager[]> keymanagersCaptor = ArgumentCaptor
                .forClass(KeyManager[].class);
        ArgumentCaptor<TrustManager[]> tmsCaptor = ArgumentCaptor
                .forClass(TrustManager[].class);
        PowerMockito.mockStatic(KeyManagerFactory.class);
        KeyManagerFactory mockFactory = PowerMockito.mock(KeyManagerFactory.class);
        when(KeyManagerFactory.getInstance("SunX509", "SunJSSE")).thenReturn(mockFactory);
        KeyManager[] mockKms = new KeyManager[] { mock(X509KeyManager.class) };
        when(mockFactory.getKeyManagers()).thenReturn(mockKms);
        PowerMockito.mockStatic(javax.net.ssl.TrustManagerFactory.class);
        javax.net.ssl.TrustManagerFactory mockTrustFactory = PowerMockito.mock(javax.net.ssl.TrustManagerFactory.class);
        when(mockTrustFactory.getTrustManagers()).thenReturn(new TrustManager[0]);
        when(javax.net.ssl.TrustManagerFactory.getInstance("SunX509", "SunJSSE")).thenReturn(mockTrustFactory);
        
        SSLContextFactory.getServerContext(sslConf);
        verify(context).init(keymanagersCaptor.capture(),
                tmsCaptor.capture(), any(SecureRandom.class));
        KeyManager[] kms = keymanagersCaptor.getValue();
        assertEquals(1, kms.length);
        // Keymanagers should be wrapped by JSSEKeyManager
        assertEquals(
                "com.redhat.thermostat.common.internal.JSSEKeyManager",
                kms[0].getClass().getName());
        TrustManager[] tms = tmsCaptor.getValue();
        assertEquals(1, tms.length);
        assertEquals(
                "com.redhat.thermostat.common.internal.CustomX509TrustManager",
                tms[0].getClass().getName());
    }

    @Test
    public void verifySetsUpClientContextWithProperTrustManager()
            throws Exception {
        File keystoreFile = new File(this.getClass()
                .getResource("/cmdChanServer.keystore").getFile());

        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        when(sslConf.getKeystoreFile()).thenReturn(
                keystoreFile);
        when(sslConf.getKeyStorePassword()).thenReturn(
                "testpassword");

        PowerMockito.mockStatic(SSLContext.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2", "SunJSSE")).thenReturn(context);
        PowerMockito.mockStatic(javax.net.ssl.TrustManagerFactory.class);
        javax.net.ssl.TrustManagerFactory mockTrustFactory = PowerMockito.mock(javax.net.ssl.TrustManagerFactory.class);
        when(mockTrustFactory.getTrustManagers()).thenReturn(new TrustManager[0]);
        when(javax.net.ssl.TrustManagerFactory.getInstance("SunX509", "SunJSSE")).thenReturn(mockTrustFactory);

        ArgumentCaptor<TrustManager[]> tmsCaptor = ArgumentCaptor
                .forClass(TrustManager[].class);
        SSLContextFactory.getClientContext(sslConf);
        verify(context).init(any(KeyManager[].class), tmsCaptor.capture(),
                any(SecureRandom.class));
        TrustManager[] tms = tmsCaptor.getValue();
        assertEquals(1, tms.length);
        assertEquals(tms[0].getClass().getName(),
                "com.redhat.thermostat.common.internal.CustomX509TrustManager");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @PrepareForTest({TrustManagerFactory.class, SSLContext.class})
    public void verifyTLSVersionFallsBackProperlyToTLS11() throws Exception {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        PowerMockito.mockStatic(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.1", "SunJSSE")).thenReturn(context);
        PowerMockito.mockStatic(TrustManagerFactory.class);
        X509TrustManager tm = PowerMockito.mock(X509TrustManager.class);
        when(TrustManagerFactory.getTrustManager(isA(SSLConfiguration.class))).thenReturn(tm);
        SSLContextFactory.getClientContext(sslConf);
        verify(context).init(any(KeyManager[].class),
                any(TrustManager[].class), any(SecureRandom.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @PrepareForTest({TrustManagerFactory.class, SSLContext.class})
    public void verifyTLSVersionFallsBackProperlyToTLS10() throws Exception {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        PowerMockito.mockStatic(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.1", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        when(SSLContext.getInstance("TLSv1", "SunJSSE")).thenReturn(context);
        PowerMockito.mockStatic(TrustManagerFactory.class);
        X509TrustManager tm = PowerMockito.mock(X509TrustManager.class);
        when(TrustManagerFactory.getTrustManager(isA(SSLConfiguration.class))).thenReturn(tm);
        SSLContextFactory.getClientContext(sslConf);
        verify(context).init(any(KeyManager[].class),
                any(TrustManager[].class), any(SecureRandom.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @PrepareForTest({TrustManagerFactory.class, SSLContext.class})
    public void throwAssertionErrorIfNoReasonableTlsAvailable()
            throws Exception {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        PowerMockito.mockStatic(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        when(SSLContext.getInstance("TLSv1.1", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        when(SSLContext.getInstance("TLSv1", "SunJSSE")).thenThrow(
                NoSuchAlgorithmException.class);
        try {
            SSLContextFactory.getClientContext(sslConf);
            fail("No suitable algos available, which should trigger AssertionError");
        } catch (AssertionError e) {
            // pass
        }
    }
    
}

