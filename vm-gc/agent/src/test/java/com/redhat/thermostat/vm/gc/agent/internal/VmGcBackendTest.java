/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.event.HostListener;

import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;

public class VmGcBackendTest {
    
    private VmGcBackend backend;
    private MonitoredHost host;

    @Before
    public void setup() throws MonitorException, URISyntaxException {
        VmGcStatDAO vmGcStatDao = mock(VmGcStatDAO.class);
        
        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");
        
        backend = new VmGcBackend(vmGcStatDao, version);
        
        host = mock(MonitoredHost.class);
        backend.setHost(host);
    }

    @Test
    public void testStart() throws MonitorException {
        backend.activate();
        verify(host).addHostListener(any(HostListener.class));
        assertTrue(backend.isActive());
    }

    @Test
    public void testStop() throws MonitorException {
        backend.activate();
        backend.deactivate();
        verify(host).removeHostListener(any(HostListener.class));
        assertFalse(backend.isActive());
    }
    
}
