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

package com.redhat.thermostat.storage.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.storage.config.ConnectionConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.StorageProvider;

public class DbServiceImpl implements DbService {
    
    @SuppressWarnings("rawtypes")
    private ServiceRegistration dbServiceReg;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration storageReg;
    
    private Storage storage;
    private BundleContext context;
    private String dbUrl;
    
    DbServiceImpl(String username, String password, String dbUrl) throws StorageException {
        BundleContext context = FrameworkUtil.getBundle(DbService.class).getBundleContext();
        init(context, username, password, dbUrl);
    }

    // for testing
    DbServiceImpl(BundleContext context, String username, String password, String dbUrl) {
        init(context, username, password, dbUrl);
    }
    
    private void init(BundleContext context, String username, String password, String dbUrl) {
        Storage storage = createStorage(context, username, password, dbUrl);

        this.storage = storage;
        this.context = context;
        this.dbUrl = dbUrl;
    }

    public void connect() throws ConnectionException {
        // Storage and DbService must not be registered
        // as service
        ensureConnectPreCondition();
        try {
            this.storage.getConnection().connect();
            dbServiceReg = context.registerService(DbService.class, this, null);
            storageReg = context.registerService(Storage.class.getName(), this.storage, null);
        } catch (Exception cause) {
            throw new ConnectionException(cause);
        }
    }
    
    public void disconnect() throws ConnectionException {
        // DbService and Storage must be registered as service at this point
        ensureDisconnectPrecondition();
        try {
            storage.getConnection().disconnect();
            storageReg.unregister();
            dbServiceReg.unregister();
        } catch (Exception cause) {
            throw new ConnectionException(cause);
        }
    }
    
    @Override
    public String getConnectionUrl() {
        return dbUrl;
    }

    /**
     * Factory method for creating a DbService instance.
     * 
     * @param username
     * @param password
     * @param dbUrl
     * @return a DbService instance
     * @throws StorageException if no storage provider exists for the given {@code dbUrl}.
     */
    public static DbService create(String username, String password, String dbUrl) throws StorageException {
        return new DbServiceImpl(username, password, dbUrl);
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

    private static Storage createStorage(BundleContext context, String username, String password, String dbUrl) throws StorageException {
        StartupConfiguration config = new ConnectionConfiguration(dbUrl, username, password);
        StorageProvider prov = getStorageProvider(context, config);
        if (prov == null) {
            // no suitable provider found
            throw new StorageException("No storage found for URL " + dbUrl);
        }
        return prov.createStorage();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StorageProvider getStorageProvider(BundleContext context, StartupConfiguration config) {
        try {
            ServiceReference[] refs = context.getServiceReferences(StorageProvider.class.getName(), null);
            if (refs == null) {
                throw new StorageException("No storage provider available");
            }
            for (int i = 0; i < refs.length; i++) {
                StorageProvider prov = (StorageProvider) context.getService(refs[i]);
                prov.setConfig(config);
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
    
}
