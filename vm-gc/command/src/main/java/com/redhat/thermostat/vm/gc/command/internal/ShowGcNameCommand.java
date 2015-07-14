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

package com.redhat.thermostat.vm.gc.command.internal;

import java.io.PrintStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.gc.command.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;

public class ShowGcNameCommand extends AbstractCommand {

    // The name as which this command is registered.
    static final String REGISTER_NAME = "show-gc-name";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final GcCommonNameMapper mapper = new GcCommonNameMapper();
    private CountDownLatch servicesAvailableLatch = new CountDownLatch(2); // vmInfo dao and vmGcStat dao
    private VmInfoDAO vmInfoDao;
    private VmGcStatDAO gcDao;
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        try {
            servicesAvailableLatch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_INTERRUPTED));
        }
        
        requireNonNull(vmInfoDao, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        requireNonNull(gcDao, translator.localize(LocaleResources.GC_STAT_DAO_SERVICE_UNAVAILABLE));

        String vmIdArg = ctx.getArguments().getArgument(Arguments.VM_ID_ARGUMENT);
        if (vmIdArg == null)
            throw new CommandException(translator.localize(LocaleResources.VMID_REQUIRED));

        VmId vmId = new VmId(vmIdArg);
        VmInfo vmInfo = checkVmExists(vmId, translator.localize(LocaleResources.VM_NOT_FOUND, vmId.get()));
        
        PrintStream out = ctx.getConsole().getOutput();
        CollectorCommonName commonName = getCommonName(vmId);
        String msg = translator.localize(LocaleResources.GC_COMMON_NAME_SUCCESS_MSG,
                                      vmInfo.getVmId(),
                                      vmInfo.getMainClass(),
                                      commonName.getHumanReadableString())
                                      .getContents();
        out.println(msg);
    }
    
    void setVmInfo(VmInfoDAO vmInfoDAO) {
        this.vmInfoDao = vmInfoDAO;
        servicesAvailableLatch.countDown();
    }
    
    void setVmGcStat(VmGcStatDAO vmGcStat) {
        this.gcDao = vmGcStat;
        servicesAvailableLatch.countDown();
    }
    
    void servicesUnavailable() {
        this.gcDao = null;
        this.vmInfoDao = null;
        servicesAvailableLatch = new CountDownLatch(2);
    }
    
    /**
     * Checks that a VM record exists in storage and if not throws a
     * command exception with an appropriate message.
     * 
     * @param vmId The VM ID to look up in storage.
     * @param errorMsg The error message to use for when VM is not found.
     * @return A non-null VmInfo.
     * @throws CommandException If the VM could not be found in storage.
     */
    private VmInfo checkVmExists(VmId vmId, LocalizedString errorMsg) throws CommandException {
        VmInfo vmInfo = vmInfoDao.getVmInfo(vmId);
        if (vmInfo == null) {
            throw new CommandException(errorMsg);
        }
        return vmInfo;
    }
    
    /**
     * Finds the common name of the GC algorithm used for a given VM.
     * @param vmId The id for the VM in question.
     * @return A common name of the GC algorithm or {@code CollectorCommonName#UNKNOWN_COLLECTOR}.
     */
    private CollectorCommonName getCommonName(VmId vmId) {
        Set<String> distinctCollectors = gcDao.getDistinctCollectorNames(vmId);
        CollectorCommonName commonName = CollectorCommonName.UNKNOWN_COLLECTOR;
        if (distinctCollectors.size() > 0) {
            commonName = mapper.mapToCommonName(distinctCollectors);
        }
        return commonName;
    }

}
