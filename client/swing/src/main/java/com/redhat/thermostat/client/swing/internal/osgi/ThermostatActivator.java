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

package com.redhat.thermostat.client.swing.internal.osgi;

import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.core.views.AgentInformationViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigViewProvider;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.osgi.service.HostDecorator;
import com.redhat.thermostat.client.swing.internal.GUIClientCommand;
import com.redhat.thermostat.client.swing.internal.HostIconDecorator;
import com.redhat.thermostat.client.swing.internal.Main;
import com.redhat.thermostat.client.swing.internal.UiFacadeFactoryImpl;
import com.redhat.thermostat.client.swing.views.SwingAgentInformationViewProvider;
import com.redhat.thermostat.client.swing.views.SwingClientConfigurationViewProvider;
import com.redhat.thermostat.client.swing.views.SwingHostInformationViewProvider;
import com.redhat.thermostat.client.swing.views.SwingSummaryViewProvider;
import com.redhat.thermostat.client.swing.views.SwingVmInformationViewProvider;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ThermostatActivator implements BundleActivator {

    private HostInformationServiceTracker hostInfoServiceTracker;
    private VmInformationServiceTracker vmInfoServiceTracker;
    private VMContextActionServiceTracker contextActionTracker;

    private CommandRegistry cmdReg;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void start(final BundleContext context) throws Exception {
        
        HostDecorator hostDecorator = new HostIconDecorator();
        context.registerService(HostDecorator.class.getName(), hostDecorator, null);
        
        // Host views
        HostInformationViewProvider infoProvider = new SwingHostInformationViewProvider();
        context.registerService(HostInformationViewProvider.class.getName(), infoProvider, null);
        
        // Vm views
        VmInformationViewProvider vmInfoProvider = new SwingVmInformationViewProvider();
        context.registerService(VmInformationViewProvider.class.getName(), vmInfoProvider, null);
        
        // Summary view
        SummaryViewProvider summaryViewProvider = new SwingSummaryViewProvider();
        context.registerService(SummaryViewProvider.class.getName(), summaryViewProvider, null);

        // AgentInformation and ClientConfiguraiton view
        AgentInformationViewProvider agentViewProvider = new SwingAgentInformationViewProvider();
        context.registerService(AgentInformationViewProvider.class.getName(), agentViewProvider, null);
        ClientConfigViewProvider clientConfigViewProvider = new SwingClientConfigurationViewProvider();
        context.registerService(ClientConfigViewProvider.class, clientConfigViewProvider, null);
        
        ServiceTracker tracker = new ServiceTracker(context, Keyring.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
              
                Keyring keyring = (Keyring) context.getService(reference);
                
                UiFacadeFactory uiFacadeFactory = new UiFacadeFactoryImpl(context);

                hostInfoServiceTracker = new HostInformationServiceTracker(context, uiFacadeFactory);
                hostInfoServiceTracker.open();
                vmInfoServiceTracker = new VmInformationServiceTracker(context, uiFacadeFactory);
                vmInfoServiceTracker.open();
                contextActionTracker = new VMContextActionServiceTracker(context, uiFacadeFactory);
                contextActionTracker.open();

                cmdReg = new CommandRegistryImpl(context);
                Main main = new Main(keyring, uiFacadeFactory, new String[0]);
                
                GUIClientCommand cmd = new GUIClientCommand(main);
                cmdReg.registerCommands(Arrays.asList(cmd));
                
                return super.addingService(reference);
            }
        };
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        hostInfoServiceTracker.close();
        vmInfoServiceTracker.close(); //context.removeServiceListener(vmInfoServiceTracker);
        contextActionTracker.close();
        cmdReg.unregisterCommands();
    }
}
