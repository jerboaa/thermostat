/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.numa.client.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.numa.client.core.internal.VmNumaController;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;

public class VmNumaServiceImplTest {

    private VmNumaServiceImpl service;

    @Before
    public void setup() {
        ApplicationService appSvc = mock(ApplicationService.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        Timer timer = mock(Timer.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        NumaDAO numaDAO = mock(NumaDAO.class);
        VmNumaDAO vmNumaDAO = mock(VmNumaDAO.class);

        VmNumaViewProvider numaViewProvider = mock(VmNumaViewProvider.class);
        VmNumaView view = mock(VmNumaView.class);
        when(numaViewProvider.createView()).thenReturn(view);

        service = new VmNumaServiceImpl(appSvc, numaDAO, vmNumaDAO, numaViewProvider);
    }

    @Test
    public void verifyMemoryGroup() {
        assertEquals(200, service.getOrderValue());
    }

    @Test
    public void verifyFilter() {
        assertNotNull(service.getFilter());
    }

    @Test
    public void verifyInformationServiceController() {
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("agentId");
        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getVmId()).thenReturn("vmId");
        when(vmRef.getHostRef()).thenReturn(hostRef);

        assertTrue(service.getInformationServiceController(vmRef) instanceof VmNumaController);
    }
}
