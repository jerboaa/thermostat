/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.client.core.views.AgentInformationViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigViewProvider;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.VersionAndInfoViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.views.SwingAgentInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.views.SwingClientConfigurationViewProvider;
import com.redhat.thermostat.client.swing.internal.views.SwingHostInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.views.SwingSummaryViewProvider;
import com.redhat.thermostat.client.swing.internal.views.SwingVmInformationViewProvider;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ThermostatActivatorTest {

    @Test
    public void verifyAllExpectedServicesAreRegistered() throws Exception {
        StubBundleContext ctx = new StubBundleContext();

        ThermostatActivator activator = new ThermostatActivator();

        activator.start(ctx);

        assertTrue(ctx.isServiceRegistered(VersionAndInfoViewProvider.class.getName(), SwingSummaryViewProvider.class));
        assertTrue(ctx.isServiceRegistered(HostInformationViewProvider.class.getName(), SwingHostInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(VmInformationViewProvider.class.getName(), SwingVmInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(AgentInformationViewProvider.class.getName(), SwingAgentInformationViewProvider.class));
        assertTrue(ctx.isServiceRegistered(ClientConfigViewProvider.class.getName(), SwingClientConfigurationViewProvider.class));
        
        assertEquals(5, ctx.getAllServices().size());

        // FIXME add more tests for the service tracker used by ThermostatActivator
    }
}

