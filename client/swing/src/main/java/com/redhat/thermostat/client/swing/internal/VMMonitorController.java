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

package com.redhat.thermostat.client.swing.internal;

import com.redhat.thermostat.client.swing.internal.search.ReferenceFieldSearchFilter;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor.Action;

class VMMonitorController {

    private NetworkMonitor networkMonitor;
    private HostMonitor hostMonitor;
    private MainView view;
    
    private HostChangeListener hostListener;
    
    public VMMonitorController(NetworkMonitor networkMonitor,
                               HostMonitor hostMonitor, MainView view)
    {
        this.hostMonitor = hostMonitor;
        this.networkMonitor = networkMonitor;
        this.view = view;
        this.hostListener = new HostChangeListener();
    }
    
    void start() {
        networkMonitor.addNetworkChangeListener(new NetworkChangeListener());
    }
    
    class NetworkChangeListener implements ActionListener<NetworkMonitor.Action>
    {
        @Override
        public void actionPerformed(ActionEvent<Action> actionEvent) {
            HostRef host = (HostRef) actionEvent.getPayload();
            switch (actionEvent.getActionId()) {
            case HOST_ADDED:
                view.getHostTreeController().registerHost(host);
                view.getSearchFilter().addHost(host);
                hostMonitor.addHostChangeListener(host, hostListener);
                break;

            case HOST_REMOVED:
                view.getHostTreeController().updateHostStatus(host);
                view.getSearchFilter().removeHost(host);
                hostMonitor.removeHostChangeListener(host, hostListener);
                break;
                
            default:
                break;
            }
        }
    }
    
    class HostChangeListener implements ActionListener<HostMonitor.Action>
    {
        @Override
        public void actionPerformed(ActionEvent<HostMonitor.Action> actionEvent)
        {
            VmRef vm = (VmRef) actionEvent.getPayload();
            switch (actionEvent.getActionId()) {
            case VM_ADDED:
                view.getHostTreeController().registerVM(vm);
                view.getSearchFilter().addVM(vm);
                break;
            
            case VM_REMOVED:
                view.getHostTreeController().updateVMStatus(vm);
                view.getSearchFilter().removeVM(vm);
            default:
                break;
            }
        }
    }
}
