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

package com.redhat.thermostat.backend.system.osgi;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.backend.system.SystemBackend;
import com.redhat.thermostat.backend.system.VmStatusChangeNotifier;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.utils.username.UserNameUtil;

@SuppressWarnings("rawtypes")
public class SystemBackendActivator implements BundleActivator {

    private MultipleServiceTracker tracker;
    private SystemBackend backend;
    private ServiceRegistration reg;
    private VmStatusChangeNotifier notifier;
    
    @Override
    public void start(final BundleContext context) throws Exception {
        
        notifier = new VmStatusChangeNotifier(context);
        notifier.start();
        
        Class<?>[] deps = new Class<?>[] {
                BackendService.class,
                HostInfoDAO.class,
                NetworkInterfaceInfoDAO.class,
                VmInfoDAO.class,
                UserNameUtil.class,
                WriterID.class, // system backend uses it
                VmBlacklist.class,
        };
        tracker = new MultipleServiceTracker(context, deps, new Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                HostInfoDAO hostInfoDAO = (HostInfoDAO) services.get(HostInfoDAO.class.getName());
                NetworkInterfaceInfoDAO netInfoDAO = (NetworkInterfaceInfoDAO) services
                        .get(NetworkInterfaceInfoDAO.class.getName());
                VmInfoDAO vmInfoDAO = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                UserNameUtil userNameUtil = (UserNameUtil) services.get(UserNameUtil.class.getName());
                Version version = new Version(context.getBundle());
                WriterID id = (WriterID) services.get(WriterID.class.getName());
                VmBlacklist blacklist = (VmBlacklist) services.get(VmBlacklist.class.getName());
                backend = new SystemBackend(hostInfoDAO, netInfoDAO, vmInfoDAO, version, notifier, 
                        userNameUtil, id, blacklist);
                reg = context.registerService(Backend.class, backend, null);
            }
            
            @Override
            public void dependenciesUnavailable() {
                if (backend.isActive()) {
                    backend.deactivate();
                }
                reg.unregister();
            }
            
        });
                
        tracker.open();
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        if (backend != null && backend.isActive()) {
            backend.deactivate();
        }
        tracker.close();
        notifier.stop();
    }
}

