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

package com.redhat.thermostat.client.internal.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.client.core.views.AgentInformationViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigViewProvider;
import com.redhat.thermostat.client.core.views.HostCpuViewProvider;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.HostMemoryViewProvider;
import com.redhat.thermostat.client.core.views.HostOverviewViewProvider;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmCpuViewProvider;
import com.redhat.thermostat.client.core.views.VmGcViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.core.views.VmOverviewViewProvider;
import com.redhat.thermostat.client.internal.HostIconDecorator;
import com.redhat.thermostat.client.osgi.service.HostDecorator;
import com.redhat.thermostat.client.swing.SwingAgentInformationViewProvider;
import com.redhat.thermostat.client.swing.SwingClientConfigurationViewProvider;
import com.redhat.thermostat.client.swing.SwingHostCpuViewProvider;
import com.redhat.thermostat.client.swing.SwingHostInformationViewProvider;
import com.redhat.thermostat.client.swing.SwingHostMemoryViewProvider;
import com.redhat.thermostat.client.swing.SwingHostOverviewViewProvider;
import com.redhat.thermostat.client.swing.SwingSummaryViewProvider;
import com.redhat.thermostat.client.swing.SwingVmCpuViewProvider;
import com.redhat.thermostat.client.swing.SwingVmGcViewProvider;
import com.redhat.thermostat.client.swing.SwingVmInformationViewProvider;
import com.redhat.thermostat.client.swing.SwingVmOverviewViewProvider;
import com.redhat.thermostat.test.StubBundleContext;

public class ThermostatActivatorTest {

    @Test
    public void verifyAllExpectedServicesAreRegistered() throws Exception {
        StubBundleContext ctx = new StubBundleContext();

        ThermostatActivator activator = new ThermostatActivator();

        activator.start(ctx);

        assertTrue(ctx.isServiceRegistered(HostDecorator.class.getName(), HostIconDecorator.class));
        assertTrue(ctx.isServiceRegistered(SummaryViewProvider.class.getName(), SwingSummaryViewProvider.class));
        assertTrue(ctx.isServiceRegistered(HostInformationViewProvider.class.getName(), SwingHostInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(HostMemoryViewProvider.class.getName(), SwingHostMemoryViewProvider.class));
        assertTrue(ctx.isServiceRegistered(HostCpuViewProvider.class.getName(), SwingHostCpuViewProvider.class));
        assertTrue(ctx.isServiceRegistered(HostOverviewViewProvider.class.getName(), SwingHostOverviewViewProvider.class));
        assertTrue(ctx.isServiceRegistered(VmInformationViewProvider.class.getName(), SwingVmInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(VmCpuViewProvider.class.getName(), SwingVmCpuViewProvider.class));
        assertTrue(ctx.isServiceRegistered(VmGcViewProvider.class.getName(), SwingVmGcViewProvider.class));
        assertTrue(ctx.isServiceRegistered(VmOverviewViewProvider.class.getName(), SwingVmOverviewViewProvider.class));
        assertTrue(ctx.isServiceRegistered(AgentInformationViewProvider.class.getName(), SwingAgentInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(ClientConfigViewProvider.class.getName(), SwingClientConfigurationViewProvider.class));
        
        assertEquals(12, ctx.getAllServices().size());
    }
}
