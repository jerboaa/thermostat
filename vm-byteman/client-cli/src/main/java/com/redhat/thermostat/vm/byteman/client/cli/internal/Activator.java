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

package com.redhat.thermostat.vm.byteman.client.cli.internal;

import java.util.Hashtable;
import java.util.Map;

import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.FileNameTabCompleter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private ServiceRegistration<Command> commandReg;
    private ServiceRegistration completerRegistration;
    private MultipleServiceTracker commandDepsTracker;
    private ServiceTracker fileNameTabCompleterTracker;
    private BytemanCompleterService completerService;

    @Override
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put(Command.NAME, BytemanControlCommand.COMMAND_NAME);
        final BytemanControlCommand bytemanCommand = new BytemanControlCommand();
        commandReg = context.registerService(Command.class, bytemanCommand, properties);
        Class<?>[] deps = new Class[] {
                VmInfoDAO.class, 
                AgentInfoDAO.class,
                VmBytemanDAO.class,
                RequestQueue.class,
        };
        commandDepsTracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesUnavailable() {
                bytemanCommand.unsetAgentInfoDao();
                bytemanCommand.unsetRequestQueue();
                bytemanCommand.unsetVmBytemanDao();
                bytemanCommand.unsetVmInfoDao();
            }
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                VmInfoDAO vmInfo = (VmInfoDAO)services.get(VmInfoDAO.class.getName());
                AgentInfoDAO agentInfo = (AgentInfoDAO)services.get(AgentInfoDAO.class.getName());
                VmBytemanDAO vmBytemanDao = (VmBytemanDAO)services.get(VmBytemanDAO.class.getName());
                RequestQueue queue = (RequestQueue)services.get(RequestQueue.class.getName());
                bytemanCommand.setAgentInfoDao(agentInfo);
                bytemanCommand.setVmInfoDao(vmInfo);
                bytemanCommand.setVmBytemanDao(vmBytemanDao);
                bytemanCommand.setRequestQueue(queue);
            }
        });
        commandDepsTracker.open();

        completerService = new BytemanCompleterService();

        fileNameTabCompleterTracker = new ServiceTracker(context, FileNameTabCompleter.class, new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference serviceReference) {
                FileNameTabCompleter fileNameTabCompleter = (FileNameTabCompleter) context.getService(serviceReference);
                completerService.setFileNameTabCompleter(fileNameTabCompleter);
                completerRegistration = context.registerService(CompleterService.class.getName(), completerService, null);
                return context.getService(serviceReference);
            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                completerService.setFileNameTabCompleter(null);
            }
        });
        fileNameTabCompleterTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (commandDepsTracker != null) {
            commandDepsTracker.close();
        }
        if (commandReg != null) {
            commandReg.unregister();
        }
        if (fileNameTabCompleterTracker != null) {
            fileNameTabCompleterTracker.close();
        }
        if (completerRegistration != null) {
            completerRegistration.unregister();
        }
        if (completerService != null) {
            completerService.setFileNameTabCompleter(null);
        }
    }

}
