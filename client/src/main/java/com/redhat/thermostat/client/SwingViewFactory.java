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

package com.redhat.thermostat.client;


import com.redhat.thermostat.client.ui.AgentConfigurationFrame;
import com.redhat.thermostat.client.ui.AgentConfigurationView;
import com.redhat.thermostat.client.ui.ClientConfigurationFrame;
import com.redhat.thermostat.client.ui.ClientConfigurationView;
import com.redhat.thermostat.client.ui.HostCpuPanel;
import com.redhat.thermostat.client.ui.HostCpuView;
import com.redhat.thermostat.client.ui.HostMemoryPanel;
import com.redhat.thermostat.client.ui.HostMemoryView;
import com.redhat.thermostat.client.ui.HostOverviewPanel;
import com.redhat.thermostat.client.ui.HostOverviewView;
import com.redhat.thermostat.client.ui.VmClassStatPanel;
import com.redhat.thermostat.client.ui.VmClassStatView;
import com.redhat.thermostat.client.ui.VmCpuPanel;
import com.redhat.thermostat.client.ui.VmCpuView;
import com.redhat.thermostat.client.ui.VmGcPanel;
import com.redhat.thermostat.client.ui.VmGcView;
import com.redhat.thermostat.client.ui.VmMemoryPanel;
import com.redhat.thermostat.client.ui.VmMemoryView;
import com.redhat.thermostat.client.ui.VmOverviewPanel;
import com.redhat.thermostat.client.ui.VmOverviewView;
import com.redhat.thermostat.common.ViewFactory;

public class SwingViewFactory extends DefaultViewFactory implements ViewFactory {

    public SwingViewFactory() {
        setViewClass(AgentConfigurationView.class, AgentConfigurationFrame.class);
        setViewClass(ClientConfigurationView.class, ClientConfigurationFrame.class);

        setViewClass(HostCpuView.class, HostCpuPanel.class);
        setViewClass(HostMemoryView.class, HostMemoryPanel.class);
        setViewClass(HostOverviewView.class, HostOverviewPanel.class);

        setViewClass(VmClassStatView.class, VmClassStatPanel.class);
        setViewClass(VmCpuView.class, VmCpuPanel.class);
        setViewClass(VmGcView.class, VmGcPanel.class);
        setViewClass(VmMemoryView.class, VmMemoryPanel.class);
        setViewClass(VmOverviewView.class, VmOverviewPanel.class);
    }

}
