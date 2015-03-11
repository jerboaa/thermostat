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

package com.redhat.thermostat.vm.io.agent.internal;

import java.util.concurrent.ScheduledExecutorService;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.backend.VmPollingAction;
import com.redhat.thermostat.backend.VmPollingBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.io.common.Constants;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoBackend extends VmPollingBackend {

    public VmIoBackend(Clock clock, ScheduledExecutorService executor, Version version,
            VmIoStatDAO vmIoStatDao,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        this(clock, executor, version,
                vmIoStatDao,
                new VmIoStatBuilder(clock, new ProcIoDataReader(new ProcDataSource()), writerId),
                registrar, writerId);
    }

    VmIoBackend(Clock clock, ScheduledExecutorService executor, Version version,
            VmIoStatDAO vmIoStatDao, VmIoStatBuilder vmIoStatBuilder,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        super("VM IO Backend",
              "Gathers IO statistics about a JVM",
              "Red Hat, Inc.",
              version, executor, registrar);

        VmIoBackendAction action = new VmIoBackendAction(vmIoStatDao, vmIoStatBuilder);
        registerAction(action);
    }

    private static class VmIoBackendAction implements VmPollingAction {

        private VmIoStatDAO dao;
        private VmIoStatBuilder builder;

        private VmIoBackendAction(VmIoStatDAO dao, VmIoStatBuilder builder) {
            this.dao = dao;
            this.builder = builder;
        }

        @Override
        public void run(String vmId, int pid) {
            VmIoStat dataBuilt = builder.build(vmId, pid);
            if (dataBuilt != null) {
                dao.putVmIoStat(dataBuilt);
            }
        }
        
    }

    @Override
    public int getOrderValue() {
        return Constants.ORDER_VALUE;
    }

}
