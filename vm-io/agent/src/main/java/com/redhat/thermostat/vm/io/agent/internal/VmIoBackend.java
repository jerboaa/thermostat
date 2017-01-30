/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.VmListenerBackend;
import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.io.common.Constants;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoBackend extends VmListenerBackend {

    private VmIoStatDAO vmIoStatDAO;
    private VmIoStatBuilder builder;

    public VmIoBackend(Clock clock, Version version,
            VmIoStatDAO vmIoStatDao,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        this(version,
                vmIoStatDao,
                new VmIoStatBuilderImpl(clock, writerId),
                registrar, writerId);
    }

    VmIoBackend(Version version,
            VmIoStatDAO vmIoStatDao, VmIoStatBuilder builder,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        super("VM IO Backend",
              "Gathers IO statistics about a JVM",
              "Red Hat, Inc.",
              version.getVersionNumber(), true , registrar, writerId);
        this.vmIoStatDAO = vmIoStatDao;
        this.builder = builder;
    }

    @Override
    protected VmUpdateListener createVmListener(String writerId, String vmId, int pid) {
        return new VmIoBackendListener(vmIoStatDAO, builder, vmId, pid);
    }

    private static class VmIoBackendListener implements VmUpdateListener {
        private VmIoStatDAO vmIoStatDAO;
        private VmIoStatBuilder builder;
        private String vmId;
        private int pid;


        public VmIoBackendListener (VmIoStatDAO vmIoStatDAO, VmIoStatBuilder builder, String vmId, int pid) {
            this.vmIoStatDAO = vmIoStatDAO;
            this.builder = builder;
            this.vmId = vmId;
            this.pid = pid;
        }

        @Override
        public void countersUpdated(VmUpdate update) {
            VmIoStat dataBuilt = builder.build(vmId, pid);
            if (dataBuilt != null) {
                vmIoStatDAO.putVmIoStat(dataBuilt);
            }
        }
    }

    @Override
    public int getOrderValue() {
        return Constants.ORDER_VALUE;
    }

}
