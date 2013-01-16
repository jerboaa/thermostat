/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.backend.system;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.utils.ProcDataSource;

public class JvmStatHostListener implements HostListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatHostListener.class);

    private final VmInfoDAO vmInfoDAO;

    private Map<Integer, MonitoredVm> monitoredVms  = new HashMap<>();
    
    private VmStatusChangeNotifier notifier;

    JvmStatHostListener(VmInfoDAO vmInfoDAO, VmStatusChangeNotifier notifier) {
        this.vmInfoDAO = vmInfoDAO;
        this.notifier = notifier;
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
                logger.log(Level.WARNING, "error getting info for new vm" + newVm, e);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for new vm" + newVm, e);
            }
        }

        for (Integer stoppedVm : (Set<Integer>) event.getTerminated()) {
            try {
                logger.fine("stopped vm: " + stoppedVm);
                sendStoppedVM(stoppedVm, host);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for stopped vm" + stoppedVm, e);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "error getting info for stopped vm" + stoppedVm, e);
            }
        }
    }

    private void sendNewVM(Integer vmId, MonitoredHost host)
            throws MonitorException, URISyntaxException {
        MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(
                new VmIdentifier(vmId.toString())));
        if (vm != null) {
            JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
            try {
                long startTime = System.currentTimeMillis();
                long stopTime = Long.MIN_VALUE;
                recordVmInfo(vmId, startTime, stopTime, extractor);
                logger.finer("Sent VM_STARTED messsage");
            } catch (MonitorException me) {
                logger.log(Level.WARNING, "error getting vm info for " + vmId, me);
            }

            notifier.notifyVmStatusChange(Status.VM_STARTED, vmId);

            monitoredVms.put(vmId, vm);
        }
    }

    void recordVmInfo(Integer vmId, long startTime, long stopTime,
            JvmStatDataExtractor extractor) throws MonitorException {
        Map<String, String> properties = new HashMap<String, String>();
        ProcDataSource dataSource = new ProcDataSource();
        Map<String, String> environment = new ProcessEnvironmentBuilder(dataSource).build(vmId);
        // TODO actually figure out the loaded libraries.
        String[] loadedNativeLibraries = new String[0];
        VmInfo info = new VmInfo(vmId, startTime, stopTime,
                extractor.getJavaVersion(), extractor.getJavaHome(),
                extractor.getMainClass(), extractor.getCommandLine(),
                extractor.getVmName(), extractor.getVmInfo(), extractor.getVmVersion(), extractor.getVmArguments(),
                properties, environment, loadedNativeLibraries);
        vmInfoDAO.putVmInfo(info);
    }

    private void sendStoppedVM(Integer vmId, MonitoredHost host) throws URISyntaxException, MonitorException {
        
        VmIdentifier resolvedVmID = host.getHostIdentifier().resolve(new VmIdentifier(vmId.toString()));
        if (resolvedVmID != null) {
            long stopTime = System.currentTimeMillis();

            notifier.notifyVmStatusChange(Status.VM_STOPPED, vmId);

            vmInfoDAO.putVmStoppedTime(vmId, stopTime);

            MonitoredVm vm = monitoredVms.remove(vmId);
            vm.detach();
        }
    }

    /*
     * For testing purposes only.
     */
    Map<Integer, MonitoredVm> getMonitoredVms() {
        return monitoredVms;
    }
    
}

