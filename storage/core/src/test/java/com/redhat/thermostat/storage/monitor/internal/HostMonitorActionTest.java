/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.HostMonitor.Action;

public class HostMonitorActionTest {

    private VmInfoDAO vmsDAO;
    
    private ActionNotifier<HostMonitor.Action> notifier;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        vmsDAO = mock(VmInfoDAO.class);
        notifier = mock(ActionNotifier.class);
    }
    
    @Test
    public void testAddRemoveVMS() {

        HostRef host = new HostRef("01", "01");
        
        Collection<VmRef> currentVMs = new ArrayList<>();
        VmRef a = new VmRef(host, "0", 0, "a");
        VmRef b = new VmRef(host, "1", 1, "b");
        VmRef c = new VmRef(host, "2", 2, "c");
        VmRef d = new VmRef(host, "3", 3, "d");
        VmRef e = new VmRef(host, "4", 3, "e");

        VmInfo info_a = mock(VmInfo.class);
        when(info_a.isAlive()).thenReturn(true);
        
        VmInfo info_b = mock(VmInfo.class);
        when(info_b.isAlive()).thenReturn(true);
        
        VmInfo info_c = mock(VmInfo.class);
        when(info_c.isAlive()).thenReturn(true);
        
        VmInfo info_d = mock(VmInfo.class);
        when(info_d.isAlive()).thenReturn(true);
        
        VmInfo info_e = mock(VmInfo.class);
        when(info_e.isAlive()).thenReturn(false);
        
        when(vmsDAO.getVmInfo(a)).thenReturn(info_a);
        when(vmsDAO.getVmInfo(b)).thenReturn(info_b);
        when(vmsDAO.getVmInfo(c)).thenReturn(info_c);
        when(vmsDAO.getVmInfo(d)).thenReturn(info_d);
        when(vmsDAO.getVmInfo(e)).thenReturn(info_e);
        
        currentVMs.add(a);
        currentVMs.add(b);
        currentVMs.add(c);
        currentVMs.add(d);
        currentVMs.add(e);

        when(vmsDAO.getVMs(host)).thenReturn(currentVMs);
        

        // the first result is to be notified of all those vms
        HostMonitorAction action = new HostMonitorAction(notifier, vmsDAO, host);
        action.run();

        verify(notifier).fireAction(Action.VM_ADDED, a);
        verify(notifier).fireAction(Action.VM_ADDED, b);
        verify(notifier).fireAction(Action.VM_ADDED, c);
        verify(notifier).fireAction(Action.VM_ADDED, d);

        verify(notifier,times(0)).fireAction(Action.VM_ADDED, e);
        verify(notifier, times(0)).fireAction(Action.VM_REMOVED, eq(any(VmRef.class)));

        // now remove one vm from each side
        currentVMs.remove(b);
        currentVMs.remove(c);

        action.run();

        verify(notifier).fireAction(Action.VM_REMOVED, b);
        verify(notifier).fireAction(Action.VM_REMOVED, c);
        
        verify(notifier, times(0)).fireAction(Action.VM_ADDED, eq(any(VmRef.class)));
        
        when(info_a.isAlive()).thenReturn(false);
        
        // not that a process can ever become alive again :)
        when(info_e.isAlive()).thenReturn(true);

        action.run();

        verify(notifier,times(1)).fireAction(Action.VM_ADDED, e);
        verify(notifier,times(1)).fireAction(Action.VM_REMOVED, a);
        
        verify(notifier, times(0)).fireAction(Action.VM_ADDED, eq(any(VmRef.class)));
        verify(notifier, times(0)).fireAction(Action.VM_REMOVED, eq(any(VmRef.class)));
    }
}

