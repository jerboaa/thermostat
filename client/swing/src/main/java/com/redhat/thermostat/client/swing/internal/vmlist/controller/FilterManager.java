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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import com.redhat.thermostat.client.core.vmlist.HostFilter;
import com.redhat.thermostat.client.core.vmlist.VMFilter;
import com.redhat.thermostat.client.swing.internal.HostFilterRegistry;
import com.redhat.thermostat.client.swing.internal.VmFilterRegistry;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;

public class FilterManager {
    
    private ActionListener<ThermostatExtensionRegistry.Action> hostFilterListener;
    private ActionListener<ThermostatExtensionRegistry.Action> vmFilterListener;

    private VmFilterRegistry vmFilterRegistry;
    private HostFilterRegistry hostFilterRegistry;
    
    public FilterManager(VmFilterRegistry vmFilterRegistry,
                         HostFilterRegistry hostFilterRegistry,
                         final HostTreeController hostController)
    {
        
        hostFilterListener = new ActionListener<ThermostatExtensionRegistry.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                if (actionEvent.getActionId() == Action.SERVICE_ADDED){
                    hostController.addHostFilter((HostFilter) actionEvent.getPayload());
                } else {
                    hostController.removeHostFilter((HostFilter) actionEvent.getPayload());
                }
            }
        };
        vmFilterListener = new  ActionListener<ThermostatExtensionRegistry.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                if (actionEvent.getActionId() == Action.SERVICE_ADDED){
                    hostController.addVMFilter((VMFilter) actionEvent.getPayload());
                } else {
                    hostController.removeVMFilter((VMFilter) actionEvent.getPayload());
                }
            }
        };
        
        this.vmFilterRegistry = vmFilterRegistry;
        this.hostFilterRegistry = hostFilterRegistry;
    }
    
    public void start() {
        hostFilterRegistry.addActionListener(hostFilterListener);
        hostFilterRegistry.start();

        vmFilterRegistry.addActionListener(vmFilterListener);
        vmFilterRegistry.start();
    }
    
    public void stop() {
        hostFilterRegistry.removeActionListener(hostFilterListener);
        hostFilterListener = null;
        hostFilterRegistry.stop();

        vmFilterRegistry.removeActionListener(vmFilterListener);
        vmFilterListener = null;
        vmFilterRegistry.stop();
    }
}
