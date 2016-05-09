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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

public class Activator implements BundleActivator {

    private ServiceRegistration<Command> commandReg;
    private MultipleServiceTracker depsTracker;
    
    @Override
    public void start(BundleContext context) throws Exception {
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
        depsTracker = new MultipleServiceTracker(context, deps, new Action() {
            
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
        depsTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        depsTracker.close();
        commandReg.unregister();
    }

}
