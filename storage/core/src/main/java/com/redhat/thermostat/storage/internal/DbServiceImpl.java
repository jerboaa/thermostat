/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.internal;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.StorageProvider;

public class DbServiceImpl implements DbService {
    
    private static Logger logger = LoggingUtils.getLogger(DbServiceImpl.class);
    @SuppressWarnings("rawtypes")
    private ServiceRegistration dbServiceReg;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration storageReg;
    
    private Storage storage;
    private BundleContext context;
    private String dbUrl;
    private String userName;
    
    DbServiceImpl(String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) throws StorageException {
        BundleContext context = FrameworkUtil.getBundle(DbService.class).getBundleContext();
        init(context, dbUrl, creds, sslConf);
    }

    // for testing
    DbServiceImpl(BundleContext context, String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) {
        init(context, dbUrl, creds, sslConf);
    }
    
    // For testing. Injects custom storage.
    DbServiceImpl(BundleContext context, Storage storage, StorageCredentials creds, SSLConfiguration sslConf) {
        this.context = context;
        this.storage = storage;
    }
    
    private void init(BundleContext context, String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) {
        Storage storage = createStorage(context, dbUrl, creds, sslConf);
        this.storage = storage;
        this.context = context;
        this.dbUrl = dbUrl;
    }

    public void connect() throws ConnectionException {
        // Storage and DbService must not be registered
        // as service
        ensureConnectPreCondition();
        try {
            // connection needs to be synchronous, otherwise there is no
            // way to guarantee the postcondition if there's a delayed exception
            // during connection handling.
            doSynchronousConnect();
        } catch (Exception cause) {
            throw new ConnectionException(cause);
        }
        // Connection didn't throw an exception. Now it is safe to register
        // services for general consumption.
        dbServiceReg = context.registerService(DbService.class, this, null);
        storageReg = context.registerService(Storage.class.getName(), this.storage, null);
    }
    
    private void doSynchronousConnect() throws ConnectionException {
        CountDownLatch latch = new CountDownLatch(1);
        SynchronousConnectionListener listener = new SynchronousConnectionListener(
                latch, ConnectionStatus.CONNECTED);
        // Install listener in order to ensure connection is synchronous.
        addConnectionListener(listener);
        Connection connection = this.storage.getConnection();
        connection.connect();
        try {
            // Wait for connection to finish.
            // The synchronous connection listener gets removed once connection
            // has finished.
            latch.await();
            // Grab username after connection completes
            this.userName = connection.getUsername();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        if (!listener.successful) {
            throw new ConnectionException();
        }
    }

    public void disconnect() throws ConnectionException {
        // DbService and Storage must be registered as service at this point
        ensureDisconnectPrecondition();
        try {
            doSyncronousDisconnect();
        } catch (Exception cause) {
            throw new ConnectionException(cause);
        }
        storageReg.unregister();
        dbServiceReg.unregister();
    }
    
    private void doSyncronousDisconnect() {
        CountDownLatch latch = new CountDownLatch(1);
        SynchronousConnectionListener listener = new SynchronousConnectionListener(
                latch, ConnectionStatus.DISCONNECTED);
        // Install listener in order to ensure connection is synchronous.
        addConnectionListener(listener);
        this.storage.getConnection().disconnect();
        try {
            // Wait for disconnect to finish.
            // The synchronous connection listener gets removed once connection
            // has finished.
            latch.await();
            this.userName = Connection.UNSET_USERNAME;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        if (!listener.successful) {
            throw new ConnectionException();
        }
    }

    @Override
    public String getConnectionUrl() {
        return dbUrl;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    /**
     * Factory method for creating a DbService instance.
     *
     * @param dbUrl The storage url to use
     * @param creds The storage credentials to use
     * @param sslConf The SSL configuration to use
     * @return a DbService instance
     * @throws StorageException if no storage provider exists for the given {@code dbUrl}.
     */
    public static DbService create(String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) throws StorageException {
        return new DbServiceImpl(dbUrl, creds, sslConf);
    }

    @SuppressWarnings("rawtypes")
    private void ensureDisconnectPrecondition() {
        ServiceReference dbServiceReference = context
                .getServiceReference(DbService.class);
        ServiceReference storageReference = context
                .getServiceReference(Storage.class);
        if (dbServiceReference == null || storageReference == null) {
            throw new IllegalStateException(
                    "DbService or Storage not registered as service when "
                            + "trying to disconnect");
        }
    }

    @SuppressWarnings("rawtypes")
    private void ensureConnectPreCondition() {
        ServiceReference dbServiceReference = context
                .getServiceReference(DbService.class);
        ServiceReference storageReference = context
                .getServiceReference(Storage.class);
        if (dbServiceReference != null || storageReference != null) {
            throw new IllegalStateException(
                    "DbService or Storage already registered as service when "
                            + "trying to connect");
        }
    }

    private static Storage createStorage(BundleContext context, String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) throws StorageException {
        StorageProvider prov = getStorageProvider(context, dbUrl, creds, sslConf);
        if (prov == null) {
            // no suitable provider found
            throw new StorageException("No storage found for URL " + dbUrl);
        }
        return prov.createStorage();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StorageProvider getStorageProvider(BundleContext context, String url, StorageCredentials creds, SSLConfiguration sslConf) {
        try {
            ServiceReference[] refs = context.getServiceReferences(StorageProvider.class.getName(), null);
            if (refs == null) {
                throw new StorageException("No storage provider available");
            }
            for (int i = 0; i < refs.length; i++) {
                StorageProvider prov = (StorageProvider) context.getService(refs[i]);
                prov.setConfig(url, creds, sslConf);
                if (prov.canHandleProtocol()) {
                    return prov;
                }
                else {
                    context.ungetService(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new AssertionError("Bad filter used to get StorageProviders", e);
        }
        return null;
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        storage.getConnection().addListener(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        storage.getConnection().removeListener(listener);
    }
    
    class SynchronousConnectionListener implements ConnectionListener {

        CountDownLatch latch;
        boolean successful = false;
        ConnectionStatus expectedType;
        
        public SynchronousConnectionListener(CountDownLatch latch, ConnectionStatus expectedType) {
            this.latch = latch;
            this.expectedType = expectedType;
        }
        
        @Override
        public void changed(ConnectionStatus newStatus) {
            switch (newStatus) {
            case CONNECTED: {
                successful = (expectedType == ConnectionStatus.CONNECTED);
                latch.countDown();
                removeConnectionListener(this);
                break;
            }
            case FAILED_TO_CONNECT: {
                latch.countDown();
                removeConnectionListener(this);
                break;
            }
            case DISCONNECTED: {
                successful = (expectedType == ConnectionStatus.DISCONNECTED);
                latch.countDown();
                removeConnectionListener(this);
            }
            default: {
                // nothing
            }
            }
        }
        
    }
}

