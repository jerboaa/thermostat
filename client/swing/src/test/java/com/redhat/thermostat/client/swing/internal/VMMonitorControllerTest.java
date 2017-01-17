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

package com.redhat.thermostat.client.swing.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.swing.internal.VMMonitorController.NetworkChangeListener;
import com.redhat.thermostat.client.swing.internal.search.ReferenceFieldSearchFilter;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor.Action;

public class VMMonitorControllerTest {

    private NetworkMonitor networkMonitor;
    private HostMonitor hostMonitor;
    private MainView view;
    private HostTreeController treeController;
    private ReferenceFieldSearchFilter searchFilter;

    @Before
    public void setUp() {
        networkMonitor = mock(NetworkMonitor.class);
        hostMonitor = mock(HostMonitor.class);
        view = mock(MainView.class);
        treeController = mock(HostTreeController.class);
        when(view.getHostTreeController()).thenReturn(treeController);
        
        searchFilter = mock(ReferenceFieldSearchFilter.class);
        when(view.getSearchFilter()).thenReturn(searchFilter);

    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void test() {
        
        HostRef host1 = mock(HostRef.class);
        
        ArgumentCaptor<ActionListener> captor =
                ArgumentCaptor.forClass(ActionListener.class);
        
        VMMonitorController controller =
                new VMMonitorController(networkMonitor, hostMonitor, view);
        controller.start();
        
        verify(networkMonitor).addNetworkChangeListener(captor.capture());
        NetworkChangeListener networkListener = (NetworkChangeListener) captor.getValue();
        
        ActionEvent<NetworkMonitor.Action> event =
                new ActionEvent<NetworkMonitor.Action>(networkMonitor,
                                                       Action.HOST_ADDED);
        event.setPayload(host1);
        
        networkListener.actionPerformed(event);
        
        verify(treeController).registerHost(host1);
        verify(searchFilter).addHost(host1);

        event = new ActionEvent<NetworkMonitor.Action>(networkMonitor,
                                                       Action.HOST_REMOVED);
        event.setPayload(host1);
        networkListener.actionPerformed(event);

        verify(treeController).updateHostStatus(host1);
    }
}

