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

package com.redhat.thermostat.backend.system.internal;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.backend.system.internal.ProcessUserInfoBuilder.ProcessUserInfo;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class JvmStatHostListener implements HostListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatHostListener.class);

    private final VmInfoDAO vmInfoDAO;
    private final VmStatusChangeNotifier notifier;
    private final ProcessUserInfoBuilder userInfoBuilder;
    private final WriterID writerId;
    private Map<Integer, Pair<String, MonitoredVm>> monitoredVms  = new HashMap<>();
    private final HostRef hostRef;
    private final VmBlacklist blacklist;

    JvmStatHostListener(VmInfoDAO vmInfoDAO, VmStatusChangeNotifier notifier, 
            ProcessUserInfoBuilder userInfoBuilder, WriterID writerId, HostRef hostRef,
            VmBlacklist blacklist) {
        this.vmInfoDAO = vmInfoDAO;
        this.notifier = notifier;
        this.userInfoBuilder = userInfoBuilder;
        this.writerId = writerId;
        this.hostRef = hostRef;
        this.blacklist = blacklist;
    }

    @Override
    public void disconnected(HostEvent event) {
        logger.warning("Disconnected from host");
    }

    @SuppressWarnings("unchecked") // Unchecked casts to (Set<Integer>).
    @Override
    public void vmStatusChanged(VmStatusChangeEvent event) {
        MonitoredHost host = event.getMonitoredHost();

        for (Integer newVm : (Set<Integer>) event.getStarted()) {
            try {
                logger.fine("New vm: " + newVm);
                sendNewVM(newVm, host);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "error getting info for new vm " + newVm, e);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for new vm " + newVm, e);
            }
        }

        for (Integer stoppedVm : (Set<Integer>) event.getTerminated()) {
            try {
                logger.fine("stopped vm: " + stoppedVm);
                sendStoppedVM(stoppedVm, host);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for stopped vm " + stoppedVm, e);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "error getting info for stopped vm " + stoppedVm, e);
            }
        }
    }

    private void sendNewVM(Integer vmPid, MonitoredHost host)
            throws MonitorException, URISyntaxException {
        // Propagate any MonitorException, and do not notify Backends or remember
        // VMs when we fail to extract the necessary information.
        // http://icedtea.classpath.org/pipermail/thermostat/2013-November/008702.html
        MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(
                new VmIdentifier(vmPid.toString())));
        if (vm != null) {
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            String vmId = UUID.randomUUID().toString();
            long startTime = System.currentTimeMillis();
            long stopTime = Long.MIN_VALUE;
            VmInfo info = createVmInfo(vmId, vmPid, startTime, stopTime, extractor);

            // Check blacklist
            VmRef vmRef = new VmRef(hostRef, vmId, vmPid, info.getMainClass());
            if (!blacklist.isBlacklisted(vmRef)) {
                vmInfoDAO.putVmInfo(info);

                notifier.notifyVmStatusChange(Status.VM_STARTED, vmId, vmPid);
                logger.finer("Sent VM_STARTED messsage");

                monitoredVms.put(vmPid, new Pair<>(vmId, vm));
            }
            else {
                logger.info("Skipping VM: " + vmPid);
            }
        }
    }

    VmInfo createVmInfo(String vmId, Integer vmPid, long startTime, long stopTime,
            JvmStatDataExtractor extractor) throws MonitorException {
        Map<String, String> properties = new HashMap<String, String>();
        ProcDataSource dataSource = new ProcDataSource();
        Map<String, String> environment = new ProcessEnvironmentBuilder(dataSource).build(vmPid);
        // TODO actually figure out the loaded libraries.
        String[] loadedNativeLibraries = new String[0];
        ProcessUserInfo userInfo = userInfoBuilder.build(vmPid);
        VmInfo info = new VmInfo(writerId.getWriterID(), vmId, vmPid, startTime, stopTime,
                extractor.getJavaVersion(), extractor.getJavaHome(),
                extractor.getMainClass(), extractor.getCommandLine(),
                extractor.getVmName(), extractor.getVmInfo(), extractor.getVmVersion(), extractor.getVmArguments(),
                properties, environment, loadedNativeLibraries, userInfo.getUid(), userInfo.getUsername());
        return info;
    }

    private void sendStoppedVM(Integer vmPid, MonitoredHost host) throws URISyntaxException, MonitorException {
        
        VmIdentifier resolvedVmID = host.getHostIdentifier().resolve(new VmIdentifier(vmPid.toString()));
        if (resolvedVmID != null && monitoredVms.containsKey(vmPid)) {
            Pair<String, MonitoredVm> vmData = monitoredVms.remove(vmPid);
            
            String vmId = vmData.getFirst();
            notifier.notifyVmStatusChange(Status.VM_STOPPED, vmId, vmPid);

            long stopTime = System.currentTimeMillis();
            vmInfoDAO.putVmStoppedTime(vmId, stopTime);

            MonitoredVm vm = vmData.getSecond();
            vm.detach();
        }
    }

    /*
     * For testing purposes only.
     */
    Map<Integer, Pair<String, MonitoredVm>> getMonitoredVms() {
        return monitoredVms;
    }
    
}

