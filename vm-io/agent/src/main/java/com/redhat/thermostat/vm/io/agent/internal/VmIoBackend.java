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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.io.common.Constants;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoBackend extends BaseBackend implements VmStatusListener {

    static final long PROC_CHECK_INTERVAL = 1000; // TODO make this configurable.

    private final VmIoStatDAO vmIoStats;
    private final ScheduledExecutorService executor;
    private final VmStatusListenerRegistrar registrar;
    private VmIoStatBuilder vmIoStatBuilder;
    private boolean started;

    private final Map<Integer, String> pidsToMonitor = new ConcurrentHashMap<>();

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
              version.getVersionNumber(), true);
        this.executor = executor;
        this.vmIoStats = vmIoStatDao;
        this.registrar = registrar;
        this.vmIoStatBuilder = vmIoStatBuilder;
    }

    @Override
    public boolean activate() {
        if (!started) {
            registrar.register(this);

            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (Entry<Integer, String> entry : pidsToMonitor.entrySet()) {
                        String vmId = entry.getValue();
                        Integer pid = entry.getKey();
                        VmIoStat dataBuilt = vmIoStatBuilder.build(vmId, pid);
                        if (dataBuilt != null) {
                            vmIoStats.putVmIoStat(dataBuilt);
                        }
                    }
                }
            }, 0, PROC_CHECK_INTERVAL, TimeUnit.MILLISECONDS);

            started = true;
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        if (started) {
            executor.shutdown();
            registrar.unregister(this);

            started = false;
        }
        return !started;
    }

    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public int getOrderValue() {
        return Constants.ORDER_VALUE;
    }

    /*
     * Methods implementing VmStatusListener
     */
    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            pidsToMonitor.put(pid, vmId);
            break;
        case VM_STOPPED:
            pidsToMonitor.remove(pid);
            break;
        }

    }

}
