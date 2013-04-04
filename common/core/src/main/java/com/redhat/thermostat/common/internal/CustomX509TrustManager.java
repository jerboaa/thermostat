/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.internal;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.redhat.thermostat.common.ssl.SSLConfiguration;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Custom X509TrustManager which first attempts to verify peer certificates
 * using Java's default trust manager. If that fails it uses thermostat's
 * keystore file (if any) and uses it for another attempt to validate the
 * server certificate. If that fails as well, the connection is not allowed.
 * 
 */
class CustomX509TrustManager implements X509TrustManager {
    
    Logger logger = LoggingUtils.getLogger(CustomX509TrustManager.class);

    /*
     * The default X509TrustManager returned by SunX509. We'll delegate
     * decisions to it, and fall back to the logic in this class if the default
     * X509TrustManager doesn't trust it.
     */
    private X509TrustManager defaultX509TrustManager;
    private X509TrustManager ourX509TrustManager;

    // For testing
    CustomX509TrustManager(X509TrustManager defaultTrustManager,
            X509TrustManager ourTrustManager) {
        this.defaultX509TrustManager = defaultTrustManager;
        this.ourX509TrustManager = ourTrustManager;
    }
    
    // For testing
    CustomX509TrustManager(X509TrustManager defaultTrustManager,
            File keyStoreFile, String keyStorePassword) {
        this.defaultX509TrustManager = defaultTrustManager;
        this.ourX509TrustManager = getOurTrustManager(keyStoreFile, keyStorePassword);
    }

    // For testing
    CustomX509TrustManager(File keyStoreFile, String keyStorePassword) {
        this.defaultX509TrustManager = getDefaultTrustManager();
        this.ourX509TrustManager = getOurTrustManager(keyStoreFile, keyStorePassword);
    }

    /*
     * Main constructor, which uses ssl.properties as config if present.
     */
    CustomX509TrustManager() {
        this(SSLConfiguration.getKeystoreFile(), SSLConfiguration.getKeyStorePassword());
    }
 
    private X509TrustManager getDefaultTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    "SunX509", "SunJSSE");
            // Passing a null-KeyStore in order to get default Java behaviour.
            // See:
            // http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager
            tmf.init((KeyStore) null);

            TrustManager tms[] = tmf.getTrustManagers();

            /*
             * Iterate over the returned trustmanagers, look for an instance of
             * X509TrustManager. If found, use that as our "default" trust
             * manager.
             */
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof X509TrustManager) {
                    logger.log(Level.FINE, "Using system trust manager.");
                    return (X509TrustManager) tms[i];
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException e) {
            logger.log(Level.WARNING, "Could not retrieve system trust manager", e);
        }
        return null;
    }
    
    /*
     * If thermostat trust store file exists and a X509TrustManager can be
     * retrieved from said trust store it returns this X509TrustManager. Returns
     * null if an exception is thrown along the way, the keystore file does not
     * exist or no X509TrustManager was found in the backing trust store.
     */
    private X509TrustManager getOurTrustManager(File trustStoreFile,
            String keyStorePassword) {
        KeyStore trustStore  = KeyStoreProvider.getKeyStore(trustStoreFile, keyStorePassword);
        if (trustStore != null) {
            // backing keystore file existed and initialization was successful.
            try {
                TrustManagerFactory tmf = null;
                tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
                tmf.init(trustStore);

                for (TrustManager tm : tmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        logger.log(Level.FINE,
                                "Using Thermostat trust manager.");
                        return (X509TrustManager) tm;
                    }
                }
            } catch (NoSuchAlgorithmException | NoSuchProviderException
                    | KeyStoreException e) {
                logger.log(Level.WARNING,
                        "Could not load Thermostat trust manager");
                return null;
            }
        }
        logger.log(Level.FINE, "No Thermostat trust manager found");
        return null;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        // no-op: we don't support client authentication
        logger.log(Level.INFO, "Checking client authentication: Allowing all client certificates!");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        logger.log(Level.FINE, "Checking server certificate");
        if (defaultX509TrustManager != null) {
            try {
                defaultX509TrustManager.checkServerTrusted(chain, authType);
                logger.log(Level.FINE, "Server certificate check passed using system trust manager");
                return;
            } catch (CertificateException e) {
                if (ourX509TrustManager != null) {
                    // try our trust manager instead
                    ourX509TrustManager.checkServerTrusted(chain, authType);
                    logger.log(Level.FINE, "Server certificate check passed using Thermostat trust manager");
                    return;
                } else {
                    // just rethrow
                    logger.log(Level.WARNING, "Server certificate check FAILED!", e);
                    throw e;
                }
            }
        } else if (ourX509TrustManager != null) {
            ourX509TrustManager.checkServerTrusted(chain, authType);
            logger.log(Level.FINE, "Server certificate check passed using Thermostat trust manager");
            return;
        }
        logger.log(Level.SEVERE, "Server certificate could not be checked. No trust managers found. Stopping now.");
        // Default to not trusting this cert
        throw new CertificateException("Certificate verification failed!");
    }

    /*
     * Union of CA's trusted by default trust manager and the thermostat trust
     * manager (if any).
     * 
     * (non-Javadoc)
     * 
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Set<X509Certificate> trustedSet = new HashSet<>();
        if (defaultX509TrustManager != null) {
            trustedSet.addAll(Arrays.asList(defaultX509TrustManager
                    .getAcceptedIssuers()));
        }
        if (ourX509TrustManager != null) {
            trustedSet.addAll(Arrays.asList(ourX509TrustManager
                    .getAcceptedIssuers()));
        }
        X509Certificate[] certs = trustedSet.toArray(new X509Certificate[0]);
        return certs;
    }

}

