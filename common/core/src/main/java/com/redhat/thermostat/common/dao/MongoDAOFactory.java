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

package com.redhat.thermostat.common.dao;

import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.common.storage.Storage;

public class MongoDAOFactory implements DAOFactory {

    private final Storage storage;

    public MongoDAOFactory(StorageProvider prov) {
        storage = prov.createStorage();
    }

    @Override
    public Connection getConnection() {
        return storage.getConnection();
    }

    @Override
    public HostInfoDAO getHostInfoDAO() {
        ensureStorageConnected();
        return new HostInfoDAOImpl(storage);
    }

    @Override
    public CpuStatDAO getCpuStatDAO() {
        ensureStorageConnected();
        return new CpuStatDAOImpl(storage);
    }

    @Override
    public MemoryStatDAO getMemoryStatDAO() {
        ensureStorageConnected();
        return new MemoryStatDAOImpl(storage);
    }

    @Override
    public NetworkInterfaceInfoDAO getNetworkInterfaceInfoDAO() {
        ensureStorageConnected();
        return new NetworkInterfaceInfoDAOImpl(storage);
    }

    @Override
    public VmInfoDAO getVmInfoDAO() {
        ensureStorageConnected();
        return new VmInfoDAOImpl(storage);
    }

    @Override
    public VmCpuStatDAO getVmCpuStatDAO() {
        ensureStorageConnected();
        return new VmCpuStatDAOImpl(storage);
    }

    public VmMemoryStatDAO getVmMemoryStatDAO() {
        ensureStorageConnected();
        return new VmMemoryStatDAOImpl(storage);
    }

    @Override
    public VmClassStatDAO getVmClassStatsDAO() {
        ensureStorageConnected();
        return new VmClassStatDAOImpl(storage);
    }

    @Override
    public VmGcStatDAO getVmGcStatDAO() {
        ensureStorageConnected();
        return new VmGcStatDAOImpl(storage);
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    private void ensureStorageConnected() {
        if (!storage.getConnection().isConnected()) {
            throw new IllegalStateException("Set up connection before accessing DAO");
        }
    }
}
