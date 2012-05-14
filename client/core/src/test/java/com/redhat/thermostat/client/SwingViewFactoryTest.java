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

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.client.ui.AgentConfigurationView;
import com.redhat.thermostat.client.ui.ClientConfigurationView;
import com.redhat.thermostat.client.ui.HostCpuView;
import com.redhat.thermostat.client.ui.HostMemoryView;
import com.redhat.thermostat.client.ui.HostOverviewView;
import com.redhat.thermostat.client.ui.VmClassStatView;
import com.redhat.thermostat.client.ui.VmCpuView;
import com.redhat.thermostat.client.ui.VmGcView;
import com.redhat.thermostat.client.ui.VmMemoryView;
import com.redhat.thermostat.client.ui.VmOverviewView;
import com.redhat.thermostat.common.View;

public class SwingViewFactoryTest {

    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        SwingViewFactory factory = new SwingViewFactory();

        List<Class<? extends View>> knownViewClasses = new ArrayList<>();

        knownViewClasses.add(AgentConfigurationView.class);
        knownViewClasses.add(ClientConfigurationView.class);
        knownViewClasses.add(HostCpuView.class);
        knownViewClasses.add(HostMemoryView.class);
        knownViewClasses.add(HostOverviewView.class);
        knownViewClasses.add(VmClassStatView.class);
        knownViewClasses.add(VmCpuView.class);
        knownViewClasses.add(VmGcView.class);
        knownViewClasses.add(VmMemoryView.class);
        knownViewClasses.add(VmOverviewView.class);

        for (Class<? extends View> klass: knownViewClasses) {
            assertNotNull(factory.getViewClass(klass));
            assertNotNull(factory.getView(klass));
        }

    }
}
