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

package com.redhat.thermostat.storage.monitor.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor.Action;

public class NetworkMonitorActionTest {

    private HostInfoDAO hostDAO;
    
    private ActionNotifier<NetworkMonitor.Action> notifier;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        hostDAO = mock(HostInfoDAO.class);
        notifier = mock(ActionNotifier.class);
    }
    
    @Test
    public void testAddRemoveHost() {
        
        Collection<HostRef> currentHosts = new ArrayList<>();
        HostRef a = new HostRef("0", "a");
        HostRef b = new HostRef("1", "b");
        HostRef c = new HostRef("2", "c");
        HostRef d = new HostRef("3", "d");
        
        currentHosts.add(a);
        currentHosts.add(b);
        currentHosts.add(c);
        currentHosts.add(d);
        
        when(hostDAO.getAliveHosts()).thenReturn(currentHosts);
        
        // the first result is to be notified of all those hosts
        NetworkMonitorAction action = new NetworkMonitorAction(notifier, hostDAO);
        action.run();
        
        verify(notifier).fireAction(Action.HOST_ADDED, a);
        verify(notifier).fireAction(Action.HOST_ADDED, b);
        verify(notifier).fireAction(Action.HOST_ADDED, c);
        verify(notifier).fireAction(Action.HOST_ADDED, d);
        
        verify(notifier, times(0)).fireAction(Action.HOST_REMOVED, eq(any(HostRef.class)));

        // now remove a from the series, add e
        HostRef e = new HostRef("4", "e");
        currentHosts.add(e);
        currentHosts.remove(a);
        
        action.run();
        
        verify(notifier).fireAction(Action.HOST_REMOVED, a);
        verify(notifier).fireAction(Action.HOST_ADDED, e);
        
        // now add f from the series, no host removal
        HostRef f = new HostRef("5", "f");
        currentHosts.add(f);

        action.run();
        
        verify(notifier).fireAction(Action.HOST_ADDED, f);
        verify(notifier, times(0)).fireAction(Action.HOST_REMOVED, eq(any(HostRef.class)));
        
        // now only remove f from the series, no other changes
        currentHosts.remove(f);
        
        action.run();

        verify(notifier).fireAction(Action.HOST_REMOVED, f);
        verify(notifier, times(0)).fireAction(Action.HOST_ADDED, eq(any(HostRef.class)));
    }
}

