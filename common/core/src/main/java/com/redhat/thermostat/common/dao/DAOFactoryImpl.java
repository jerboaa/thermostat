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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageProvider;

public class DAOFactoryImpl implements DAOFactory {

    private final Storage storage;
    private final BundleContext bundleContext;
    @SuppressWarnings("rawtypes")
    private final List<ServiceRegistration> registeredServices = new ArrayList<>();
    private AgentInfoDAO agentDAO;
    private BackendInfoDAO backendInfoDAO;
    private HostInfoDAO hostInfoDAO;
    private CpuStatDAO cpuStatDAO;
    private MemoryStatDAO memoryStatDAO;
    private NetworkInterfaceInfoDAO networkInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private VmCpuStatDAO vmCpuStatDAO;
    private VmClassStatDAO vmClassStatDAO;
    private VmMemoryStatDAO vmMemStatDAO;
    private VmGcStatDAO vmGcStatDAO;

    public DAOFactoryImpl(StorageProvider prov) {
        this(FrameworkUtil.getBundle(DAOFactoryImpl.class).getBundleContext(), prov);
    }

    public DAOFactoryImpl(BundleContext bundleContext, StorageProvider prov) {
        this.bundleContext = bundleContext;
        storage = prov.createStorage();
    }

    @Override
    public Connection getConnection() {
        return storage.getConnection();
    }

    @Override
    public AgentInfoDAO getAgentInfoDAO() {
        ensureStorageConnected();
        return agentDAO;
    }

    @Override
    public BackendInfoDAO getBackendInfoDAO() {
        ensureStorageConnected();
        return backendInfoDAO;
    }

    @Override
    public HostInfoDAO getHostInfoDAO() {
        ensureStorageConnected();
        return hostInfoDAO;
    }

    @Override
    public CpuStatDAO getCpuStatDAO() {
        ensureStorageConnected();
        return cpuStatDAO;
    }

    @Override
    public MemoryStatDAO getMemoryStatDAO() {
        ensureStorageConnected();
        return memoryStatDAO;
    }

    @Override
    public NetworkInterfaceInfoDAO getNetworkInterfaceInfoDAO() {
        ensureStorageConnected();
        return networkInfoDAO;
    }

    @Override
    public VmInfoDAO getVmInfoDAO() {
        ensureStorageConnected();
        return vmInfoDAO;
    }

    @Override
    public VmCpuStatDAO getVmCpuStatDAO() {
        ensureStorageConnected();
        return vmCpuStatDAO;
    }

    @Override
    public VmMemoryStatDAO getVmMemoryStatDAO() {
        ensureStorageConnected();
        return vmMemStatDAO;
    }

    @Override
    public VmClassStatDAO getVmClassStatsDAO() {
        ensureStorageConnected();
        return vmClassStatDAO;
    }

    @Override
    public VmGcStatDAO getVmGcStatDAO() {
        ensureStorageConnected();
        return vmGcStatDAO;
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

    @Override
    public void registerDAOsAndStorageAsOSGiServices() {
        ensureStorageConnected();
        createDAOs();
        registerAndRecordService(Storage.class, getStorage());

        registerAndRecordService(AgentInfoDAO.class, getAgentInfoDAO());
        registerAndRecordService(BackendInfoDAO.class, getBackendInfoDAO());

        registerAndRecordService(HostInfoDAO.class, getHostInfoDAO());
        registerAndRecordService(NetworkInterfaceInfoDAO.class, getNetworkInterfaceInfoDAO());
        registerAndRecordService(CpuStatDAO.class, getCpuStatDAO());
        registerAndRecordService(MemoryStatDAO.class, getMemoryStatDAO());

        registerAndRecordService(VmInfoDAO.class, getVmInfoDAO());
        registerAndRecordService(VmClassStatDAO.class, getVmClassStatsDAO());
        registerAndRecordService(VmCpuStatDAO.class, getVmCpuStatDAO());
        registerAndRecordService(VmGcStatDAO.class, getVmGcStatDAO());
        registerAndRecordService(VmMemoryStatDAO.class, getVmMemoryStatDAO());
    }

    /*
     * Pre: Db connected
     */
    void createDAOs() {
        agentDAO = new AgentInfoDAOImpl(storage);
        backendInfoDAO = new BackendInfoDAOImpl(storage);
        hostInfoDAO = new HostInfoDAOImpl(storage, agentDAO);
        cpuStatDAO = new CpuStatDAOImpl(storage);
        memoryStatDAO = new MemoryStatDAOImpl(storage);
        networkInfoDAO = new NetworkInterfaceInfoDAOImpl(storage);
        vmInfoDAO = new VmInfoDAOImpl(storage);
        vmCpuStatDAO = new VmCpuStatDAOImpl(storage);
        vmClassStatDAO = new VmClassStatDAOImpl(storage);
        vmMemStatDAO = new VmMemoryStatDAOImpl(storage);
        vmGcStatDAO = new VmGcStatDAOImpl(storage);
    }

    private <K> void registerAndRecordService(Class<K> serviceType, K serviceImplementation) {
        registeredServices.add(bundleContext.registerService(serviceType, serviceImplementation, null));
    }

    public void unregisterDAOsAndStorageAsOSGiServices() {
        Iterator<ServiceRegistration> iter = registeredServices.iterator();
        while (iter.hasNext()) {
            ServiceRegistration registration = iter.next();
            registration.unregister();
            iter.remove();
        }
    }

}
