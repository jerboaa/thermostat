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

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

import com.redhat.thermostat.common.internal.JSSEKeyManager;
import com.redhat.thermostat.common.internal.KeyStoreProvider;
import com.redhat.thermostat.common.internal.TrustManagerFactory;
import com.redhat.thermostat.common.internal.DelegateSSLSocketFactory;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLContextFactory {

    private static final Logger logger = LoggingUtils.getLogger(SSLContextFactory.class);
    private static final String PROTOCOL_TLSv12 = "TLSv1.2";
    private static final String PROTOCOL_TLSv11 = "TLSv1.1";
    private static final String PROTOCOL_TLSv10 = "TLSv1";
    private static final String TLS_PROVIDER = "SunJSSE";
    private static final String ALGORITHM = "SunX509";
    private static SSLContext serverContext;
    private static SSLContext clientContext;
    
    /**
     * 
     * @return An initialized SSLContext 
     * @throws SslInitException
     * @throws InvalidConfigurationException
     */
    public static SSLContext getServerContext(SSLConfiguration sslConf) throws SslInitException,
            InvalidConfigurationException {
        if (serverContext != null) {
            return serverContext;
        }
        initServerContext(sslConf);
        return serverContext;
    }

    /**
     * 
     * @return An initialized SSLContext with Thermostat's only X509TrustManager
     *         registered.
     * @throws SslInitException if SSL initialization failed.
     */
    public static SSLContext getClientContext(SSLConfiguration sslConf) throws SslInitException {
        if (clientContext != null) {
            return clientContext;
        }
        initClientContext(sslConf);
        return clientContext;
    }
    
    public static SSLSocketFactory wrapSSLFactory(SSLSocketFactory socketFactory, SSLParameters params) {
        return new DelegateSSLSocketFactory(socketFactory, params);
    }

    public static SSLParameters getSSLParameters(SSLContext ctxt) {
        SSLParameters params = ctxt.getDefaultSSLParameters();
        ArrayList<String> protocols = new ArrayList<String>(
                Arrays.asList(params.getProtocols()));
        // Do not send an SSL-2.0-compatible Client Hello.
        protocols.remove("SSLv2Hello");
        params.setProtocols(protocols.toArray(new String[protocols.size()]));
        ArrayList<String> ciphers = new ArrayList<String>(Arrays.asList(params
                .getCipherSuites()));
        ciphers.retainAll(Arrays
                .asList("TLS_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_RSA_WITH_AES_256_CBC_SHA256",
                        "TLS_RSA_WITH_AES_256_CBC_SHA",
                        "TLS_RSA_WITH_AES_128_CBC_SHA",
                        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"));
        params.setCipherSuites(ciphers.toArray(new String[ciphers.size()]));
        return params;
    }

    private static void initClientContext(SSLConfiguration sslConf) throws SslInitException {
        SSLContext clientCtxt = null;
        try {
            clientCtxt = getContextInstance();
            // Don't need key managers for client mode
            clientCtxt.init(null, getTrustManagers(sslConf), new SecureRandom());
        } catch (KeyManagementException e) {
            throw new SslInitException(e);
        }
        clientContext = clientCtxt;
    }

    private static void initServerContext(SSLConfiguration sslConf) throws SslInitException,
            InvalidConfigurationException {
        SSLContext serverCtxt = null;
        File trustStoreFile = sslConf.getKeystoreFile();
        String keyStorePassword = sslConf.getKeyStorePassword();
        KeyStore ks = KeyStoreProvider.getKeyStore(trustStoreFile,
                keyStorePassword);
        if (ks == null) {
            // This is bad news. We need a proper key store for retrieving the
            // server certificate.
            logReason(trustStoreFile);
            throw new SslInitException(
                    "Failed to initialize server side SSL context");
        }
        try {
            serverCtxt = getContextInstance();
            // Initialize the SSLContext to work with our key and trust managers.
            serverCtxt.init(getKeyManagers(ks, keyStorePassword),
                    getTrustManagers(sslConf), new SecureRandom());
        } catch (GeneralSecurityException e) {
            throw new SslInitException(e);
        }
        serverContext = serverCtxt;
    }
    
    private static TrustManager[] getTrustManagers(SSLConfiguration sslConf) throws SslInitException {
        TrustManager tm = TrustManagerFactory.getTrustManager(sslConf);
        return new TrustManager[] { tm }; 
    }
    
    private static KeyManager[] getKeyManagers(KeyStore ks, String keystorePassword)
            throws NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException, NoSuchProviderException {
        // Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM, TLS_PROVIDER);
        kmf.init(ks, keystorePassword.toCharArray());
        KeyManager[] rawKeyManagers = kmf.getKeyManagers();
        KeyManager kms[] = new KeyManager[rawKeyManagers.length];
        for (int i = 0; i < rawKeyManagers.length; i++) {
            // Wrap with our keymanager, so that propperly aliased key is
            // used in keystore.
            kms[i] = new JSSEKeyManager((X509KeyManager)rawKeyManagers[i]);
        }
        return kms;
    }

    private static void logReason(File trustStoreFile) {
        String detail = "Reason: no keystore file specified!";
        if (trustStoreFile != null) {
            if (!trustStoreFile.exists()) {
                detail = "Reason: keystore file '" + trustStoreFile.toString() + "' does not exist!";
            } else {
                detail = "Reason: illegal keystore password!";
            }
        }
        logger.log(Level.SEVERE, "Failed to load keystore. " + detail);
    }
    
    private static SSLContext getContextInstance() {
        // Create the context. Specify the SunJSSE provider to avoid
        // picking up third-party providers. Try the TLS 1.2 provider
        // first, TLS 1.1 second and then fall back to TLS 1.0.
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance(PROTOCOL_TLSv12, TLS_PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            try {
                ctx = SSLContext.getInstance(PROTOCOL_TLSv11, TLS_PROVIDER);
            } catch (NoSuchAlgorithmException ex) {
                try {
                    ctx = SSLContext.getInstance(PROTOCOL_TLSv10, TLS_PROVIDER);
                } catch (NoSuchAlgorithmException exptn) {
                    // The TLS 1.0 provider should always be available.
                    throw new AssertionError(exptn);
                } catch (NoSuchProviderException exptn) {
                    // The SunJSSE provider should always be available.
                    throw new AssertionError(exptn);
                }
            } catch (NoSuchProviderException ex) {
                // The SunJSSE provider should always be available.
                throw new AssertionError(e);
            }
        } catch (NoSuchProviderException e) {
            // The SunJSSE provider should always be available.
            throw new AssertionError(e);
        }
        return ctx;
    }
}

