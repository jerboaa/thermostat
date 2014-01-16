/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.agent.utils.SysConf;
import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuBackend extends BaseBackend implements VmStatusListener {

    private static final Logger LOGGER = LoggingUtils.getLogger(VmCpuBackend.class);
    static final long PROC_CHECK_INTERVAL = 1000; // TODO make this configurable.

    private final VmCpuStatDAO vmCpuStats;
    private final ScheduledExecutorService executor;
    private final VmStatusListenerRegistrar registrar;
    private VmCpuStatBuilder vmCpuStatBuilder;
    private boolean started;

    private final Map<Integer, String> pidsToMonitor = new ConcurrentHashMap<>();

    public VmCpuBackend(ScheduledExecutorService executor, VmCpuStatDAO vmCpuStatDao, Version version,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        super("VM CPU Backend",
                "Gathers CPU statistics about a JVM",
                "Red Hat, Inc.",
                version.getVersionNumber(), true);
        this.executor = executor;
        this.vmCpuStats = vmCpuStatDao;
        this.registrar = registrar;

        Clock clock = new SystemClock();
        long ticksPerSecond = SysConf.getClockTicksPerSecond();
        ProcDataSource source = new ProcDataSource();
        ProcessStatusInfoBuilder builder = new ProcessStatusInfoBuilder(new ProcDataSource());
        int numCpus = getCpuCount(source);
        vmCpuStatBuilder = new VmCpuStatBuilder(clock, numCpus, ticksPerSecond,
                                                builder, writerId);
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
                        if (vmCpuStatBuilder.knowsAbout(pid)) {
                            VmCpuStat dataBuilt = vmCpuStatBuilder.build(vmId, pid);
                            if (dataBuilt != null) {
                                vmCpuStats.putVmCpuStat(dataBuilt);
                            }
                        } else {
                            vmCpuStatBuilder.learnAbout(pid);
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
        return ORDER_CPU_GROUP + 50;
    }

    private int getCpuCount(ProcDataSource dataSource) {
        final String KEY_PROCESSOR_ID = "processor";
        int cpuCount = 0;
        try (BufferedReader bufferedReader = new BufferedReader(dataSource.getCpuInfoReader())) {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(KEY_PROCESSOR_ID)) {
                    cpuCount++;
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Unable to read cpu info");
        }
        
        return cpuCount;
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
            vmCpuStatBuilder.forgetAbout(pid);
            break;
        }

    }
    
    /*
     * For testing purposes only.
     */
    void setVmCpuStatBuilder(VmCpuStatBuilder vmCpuStatBuilder) {
        this.vmCpuStatBuilder = vmCpuStatBuilder;
    }

}

