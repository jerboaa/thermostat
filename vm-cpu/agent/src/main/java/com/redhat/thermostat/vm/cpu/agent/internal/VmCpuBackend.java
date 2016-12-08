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

package com.redhat.thermostat.vm.cpu.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.utils.linux.ProcDataSource;
import com.redhat.thermostat.agent.utils.SysConf;
import com.redhat.thermostat.agent.utils.windows.WindowsHelperImpl;
import com.redhat.thermostat.backend.VmPollingAction;
import com.redhat.thermostat.backend.VmPollingBackend;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.OS;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuBackend extends VmPollingBackend {

    private static final Logger LOGGER = LoggingUtils.getLogger(VmCpuBackend.class);

    private VmCpuBackendAction action;

    public VmCpuBackend(ScheduledExecutorService executor, VmCpuStatDAO vmCpuStatDao, Version version,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        super("VM CPU Backend",
                "Gathers CPU statistics about a JVM",
                "Red Hat, Inc.",
                version, executor, registrar);
        VmCpuBackendAction action = new VmCpuBackendAction(writerId, vmCpuStatDao);
        this.action = action;
        registerAction(action);
    }

    @Override
    public int getOrderValue() {
        return ORDER_CPU_GROUP + 50;
    }

    private static class VmCpuBackendAction implements VmPollingAction {

        private VmCpuStatBuilder builder;
        private VmCpuStatDAO dao;

        private VmCpuBackendAction(final WriterID id, VmCpuStatDAO dao) {
            Clock clock = new SystemClock();
            long ticksPerSecond = SysConf.getClockTicksPerSecond();
            ProcDataSource source = new ProcDataSource();
            int numCpus = getCpuCount(source);
            ProcessStatusInfoBuilder PSIBuilder = OS.IS_LINUX ? new LinuxProcessStatusInfoBuilderImpl(source) : new WindowsProcessStatusInfoBuilderImpl();
            builder = new VmCpuStatBuilder(clock, numCpus, ticksPerSecond, PSIBuilder, id);
            this.dao = dao;
            if (OS.IS_WINDOWS) {
                LOGGER.log(Level.WARNING, "VmCpu backend is not yet ported to Windows");
            }
        }

        @Override
        public void run(String vmId, int pid) {
            if (builder.knowsAbout(pid)) {
                VmCpuStat dataBuilt = builder.build(vmId, pid);
                if (dataBuilt != null) {
                    dao.putVmCpuStat(dataBuilt);
                }
            } else {
                builder.learnAbout(pid);
            }
        }

        private int getCpuCount(ProcDataSource dataSource) {
            return OS.IS_WINDOWS ? getWindowsCpuCount() : getLinuxCpuCount(dataSource);
        }

        private int getWindowsCpuCount() {
            return WindowsHelperImpl.INSTANCE.getCPUCount();
        }

        private int getLinuxCpuCount(ProcDataSource dataSource) {
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
    }

    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        super.vmStatusChanged(newStatus, vmId, pid);
        if (Status.VM_STOPPED.equals(newStatus)) {
            action.builder.forgetAbout(pid);
        }
    }

    /*
     * For testing purposes only.
     */
    void setVmCpuStatBuilder(VmCpuStatBuilder vmCpuStatBuilder) {
        action.builder = vmCpuStatBuilder;
    }

}

