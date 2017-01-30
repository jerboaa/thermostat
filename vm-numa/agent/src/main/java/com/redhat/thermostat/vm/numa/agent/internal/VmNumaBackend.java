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

package com.redhat.thermostat.vm.numa.agent.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.VmPollingAction;
import com.redhat.thermostat.backend.VmPollingBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaBackend extends VmPollingBackend {

    private static final Logger logger = LoggingUtils.getLogger(VmNumaBackend.class);
    private final VmNumaBackendAction action;

    public VmNumaBackend(ScheduledExecutorService executor, Clock clock, NumaMapsReaderProvider readerProvider, PageSizeProvider pageSizeProvider,
                         VmNumaDAO vmNumaDAO, Version version, VmStatusListenerRegistrar registrar, WriterID id) {
        super("VM NUMA Backend",
                "Gathers NUMA statistics about a vm",
                "Red Hat, Inc.",
                version, executor, registrar);
        this.action = new VmNumaBackendAction(id, clock, readerProvider, pageSizeProvider, vmNumaDAO);
        registerAction(action);
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP;
    }

    private static class VmNumaBackendAction implements VmPollingAction {
        private WriterID writerID;
        private Clock clock;
        private NumaMapsReaderProvider readerProvider;
        private PageSizeProvider pageSizeProvider;
        private VmNumaDAO dao;
        private Map<Integer, VmNumaCollector> collectors;

        private VmNumaBackendAction(final WriterID writerID, Clock clock, NumaMapsReaderProvider readerProvider,
                                    PageSizeProvider pageSizeProvider, VmNumaDAO dao) {
            this.writerID = writerID;
            this.clock = clock;
            this.readerProvider = readerProvider;
            this.pageSizeProvider = pageSizeProvider;
            this.dao = dao;
            this.collectors = new HashMap<>();
        }

        @Override
        public void run(String vmId, int pid) {
            if (!collectors.containsKey(pid)) {
                VmNumaCollector collector = new VmNumaCollector(pid, clock, readerProvider, pageSizeProvider);
                collectors.put(pid, collector);
            }

            try {
                VmNumaStat data = collectors.get(pid).collect();
                data.setAgentId(writerID.getWriterID());
                data.setVmId(vmId);
                dao.putVmNumaStat(data);
            } catch (IOException e) {
                logger.log(Level.FINE, "Unable to read numa info for: " + pid, e);
            }
        }
    }

    // For testing purposes only
    void setVmNumaBackendCollector(int pid, VmNumaCollector collector) {
        action.collectors.put(pid, collector);
    }
}
