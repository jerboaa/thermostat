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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.internal.dao.AgentInfoDAOImpl;
import com.redhat.thermostat.storage.internal.dao.BackendInfoDAOImpl;
import com.redhat.thermostat.storage.internal.dao.HostInfoDAOImpl;
import com.redhat.thermostat.storage.internal.dao.NetworkInterfaceInfoDAOImpl;
import com.redhat.thermostat.storage.internal.dao.VmInfoDAOImpl;

public class Activator implements BundleActivator {
    
    private static final String WRITER_UUID = UUID.randomUUID().toString();
    
    ServiceTracker<Storage, Storage> tracker;
    List<ServiceRegistration<?>> regs;
    
    public Activator() {
        regs = new ArrayList<>();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        // WriterID has to be registered unconditionally (at least not as part
        // of the Storage.class tracker, since that is only registered once
        // storage is connected).
        final WriterID writerID = new WriterIDImpl(WRITER_UUID);
        ServiceRegistration<?> reg = context.registerService(WriterID.class, writerID, null);
        regs.add(reg);
        
        tracker = new ServiceTracker<Storage, Storage>(context, Storage.class, null) {
            @Override
            public Storage addingService(ServiceReference<Storage> reference) {
                Storage storage = (Storage) super.addingService(reference);
                AgentInfoDAO agentInfoDao = new AgentInfoDAOImpl(storage);
                ServiceRegistration<?> reg = context.registerService(AgentInfoDAO.class.getName(), agentInfoDao, null);
                regs.add(reg);
                BackendInfoDAO backendInfoDao = new BackendInfoDAOImpl(storage);
                reg = context.registerService(BackendInfoDAO.class.getName(), backendInfoDao, null);
                regs.add(reg);
                HostInfoDAO hostInfoDao = new HostInfoDAOImpl(storage, agentInfoDao);
                reg = context.registerService(HostInfoDAO.class.getName(), hostInfoDao, null);
                regs.add(reg);
                NetworkInterfaceInfoDAO networkInfoDao = new NetworkInterfaceInfoDAOImpl(storage);
                reg = context.registerService(NetworkInterfaceInfoDAO.class.getName(), networkInfoDao, null);
                regs.add(reg);
                VmInfoDAO vmInfoDao = new VmInfoDAOImpl(storage);
                reg = context.registerService(VmInfoDAO.class.getName(), vmInfoDao, null);
                regs.add(reg);
                return storage;
            }
            
            @Override
            public void removedService(ServiceReference<Storage> reference,
                    Storage service) {
                unregisterServices();
                super.removedService(reference, service);
            }
        };
        
        tracker.open();
    }

    private void unregisterServices() {
        for (ServiceRegistration<?> reg : regs) {
            reg.unregister();
        }
        regs.clear();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        unregisterServices();
        tracker.close();
    }
}

