package com.redhat.thermostat.backend.system;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.redhat.thermostat.agent.storage.Storage;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatHostListener implements HostListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatHostListener.class);

    private Storage storage;

    private Map<Integer, JvmStatVmListener> listenerMap = new HashMap<Integer, JvmStatVmListener>();

    public void setStorage(Storage storage) {
        if (storage == null) {
            throw new NullPointerException();
        }
        this.storage = storage;
    }

    @Override
    public void disconnected(HostEvent event) {
        logger.warning("Disconnected from host");
    }

    @Override
    public void vmStatusChanged(VmStatusChangeEvent event) {
        if (storage == null) {
            throw new NullPointerException("null");
        }

        MonitoredHost host = event.getMonitoredHost();

        Iterator<Integer> newActive = event.getStarted().iterator();
        while (newActive.hasNext()) {
            Integer newVm = newActive.next();
            try {
                logger.fine("New vm: " + newVm);
                sendNewVM(newVm, host);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "error getting info for new vm" + newVm, e);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for new vm" + newVm, e);
            }
        }

        Iterator<Integer> newStopped = event.getTerminated().iterator();
        while (newStopped.hasNext()) {
            Integer stoppedVm = newStopped.next();
            try {
                logger.fine("stopped vm: " + stoppedVm);
                sendStoppedVM(stoppedVm, host);
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "error getting info for stopped vm" + stoppedVm, e);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "error getting info for new vm" + stoppedVm, e);
            }
        }
    }

    private void sendNewVM(Integer vmId, MonitoredHost host)
            throws MonitorException, URISyntaxException {
        MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(
                new VmIdentifier(vmId.toString())));
        if (vm != null) {
            VmInfo info = null;
            try {
                long startTime = System.currentTimeMillis();
                long stopTime = Long.MIN_VALUE;
                JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
                Map<String, String> properties = new HashMap<String, String>();
                Map<String, String> environment = new HashMap<String, String>();
                List<String> loadedNativeLibraries = new ArrayList<String>();
                info = new VmInfo(vmId, startTime, stopTime,
                        extractor.getJavaVersion(), extractor.getJavaHome(),
                        extractor.getMainClass(), extractor.getCommandLine(),
                        extractor.getVmName(), extractor.getVmInfo(), extractor.getVmVersion(), extractor.getVmArguments(),
                        properties, environment, loadedNativeLibraries);
                // FIXME storage.addVmInfo(info);
                logger.finer("Sent VM_STARTED messsage");
            } catch (MonitorException me) {
                logger.log(Level.WARNING, "error getting vm info for " + vmId, me);
            }

            JvmStatVmListener listener = new JvmStatVmListener(storage, vmId);
            listenerMap.put(vmId, listener);
            vm.addVmListener(listener);
        }
    }

    private void sendStoppedVM(Integer vmId, MonitoredHost host)
            throws URISyntaxException, MonitorException {
        VmIdentifier resolvedVmID = host.getHostIdentifier().resolve(
                new VmIdentifier(vmId.toString()));
        if (resolvedVmID != null) {
            MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(
                    new VmIdentifier(vmId.toString())));
            if (vm != null) {
                JvmStatVmListener listener = listenerMap.remove(vmId);
                vm.removeVmListener(listener);
            }
            // TODO record vm as stopped
        }
    }

}
