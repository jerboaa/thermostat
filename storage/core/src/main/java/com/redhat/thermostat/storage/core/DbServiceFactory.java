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

package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.internal.DbServiceImpl;

/*
 * Factory in order to be able to hide the DbService implementation. Note that
 * this package will be part of Export-Package whereas the package where *Impl
 * classes are won't be.
 *
 */
/**
 * Factory for creating new {@link DbService} instances.
 * 
 * @see {@link DbService}, {@link StorageProvider}.
 *
 */
public class DbServiceFactory {

    /**
     * Creates a <strong>new</strong> {@link DbService} instance which can be
     * used to establish a connection with {@link Storage}. Note that the actual
     * {@link StorageProvider} which will be used for the connection is looked
     * up based on registered StorageProvider OSGi services and URLs they are
     * able to handle. If a {@link DbService} instance is already registered as
     * a service, users are encouraged to use that instance for disconnecting
     * from storage.
     * 
     * @param dbUrl
     *            The URL to the storage endpoint. For example
     *            {@code mongodb://127.0.0.1:27518} or
     *            {@code https://storage.example.com/storage}.
     * @param creds
     *            The credentials to use for the connection.
     * @param sslConf
     *            The TLS configuration to use for the connection.
     * @return A new {@link DbService} instance which can be used for
     *         establishing new storage connections.
     * @throws StorageException
     *             If no matching {@link StorageProvider} could be found for the
     *             given dbUrl.
     */
    public DbService createDbService(String dbUrl, StorageCredentials creds, SSLConfiguration sslConf) throws StorageException {
        return DbServiceImpl.create(dbUrl, creds, sslConf);
    }
}

