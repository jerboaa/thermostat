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

package com.redhat.thermostat.vm.numa.agent.internal;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.VmPollingAction;
import com.redhat.thermostat.backend.VmPollingBackend;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaBackend extends VmPollingBackend {

    private final VmNumaBackendAction action;
    private static final Logger logger = LoggingUtils.getLogger(VmNumaBackend.class);

    public VmNumaBackend(ScheduledExecutorService executor, VmNumaDAO vmNumaDAO, Version version,
                         VmStatusListenerRegistrar registrar, WriterID id) {
        super("VM NUMA Backend",
                "Gathers NUMA statistics about a vm",
                "Red Hat, Inc.",
                version, executor, registrar);
        this.action = new VmNumaBackendAction(id, vmNumaDAO);
        registerAction(action);
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP;
    }

    private static class VmNumaBackendAction implements VmPollingAction {
        private VmNumaDAO dao;
        private WriterID writerID;
        private Map<Integer, VmNumaCollector> collectors;

        private VmNumaBackendAction(final WriterID writerID, VmNumaDAO dao) {
            this.writerID = writerID;
            this.dao = dao;
            this.collectors = new HashMap<>();
        }

        @Override
        public void run(String vmId, int pid) {
            if (!collectors.containsKey(pid)) {
                collectors.put(pid, new VmNumaCollector(pid));
            }

            try {
                VmNumaStat data = collectors.get(pid).collect();
                data.setAgentId(writerID.getWriterID());
                data.setVmId(vmId);
                dao.putVmNumaStat(data);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to add numastat data for vm: " + vmId);
            }
        }
    }

    /**
     * VmNumaBackend requires numastat process to function
     * @return true if numastat process exists, false otherwise
     */
    public boolean canRegister() {
        try {
            Runtime.getRuntime().exec("numastat");
            return true;
        } catch (IOException e) {
            //numastat does not exist, do nothing
        }
        return false;
    }

    // For testing purposes only
    void setVmNumaBackendCollector(int pid, VmNumaCollector collector) {
        action.collectors.put(pid, collector);
    }
}
