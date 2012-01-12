package com.redhat.thermostat.backend.system;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
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

import com.redhat.thermostat.agent.storage.Category;
import com.redhat.thermostat.agent.storage.Chunk;
import com.redhat.thermostat.agent.storage.Key;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatHostListener implements HostListener {

    private static final Logger logger = LoggingUtils.getLogger(JvmStatHostListener.class);

    private static final Category vmInfoCategory = new Category("vm-info");
    private static final Key vmInfoIdKey = new Key("vm-id", true);
    private static final Key vmInfoPidKey = new Key("vm-pid", false);
    private static final Key vmInfoRuntimeVersionKey = new Key("runtime-version", false);
    private static final Key vmInfoJavaHomeKey = new Key("java-home", false);
    private static final Key vmInfoMainClassKey = new Key("main-class", false);
    private static final Key vmInfoCommandLineKey = new Key("command-line", false);
    private static final Key vmInfoVmNameKey = new Key("vm-name", false);
    private static final Key vmInfoVmArgumentsKey = new Key("vm-arguments", false);
    private static final Key vmInfoVmInfoKey = new Key("vm-info", false);
    private static final Key vmInfoVmVersionKey = new Key("vm-version", false);
    private static final Key vmInfoEnvironmentKey = new Key("environment", false);
    private static final Key vmInfoLibrariesKey = new Key("libraries", false);
    private static final Key vmInfoStartTimeKey = new Key("start-time", false);
    private static final Key vmInfoStopTimeKey = new Key("stop-time", false);

    static {
        vmInfoCategory.addKey(vmInfoIdKey);
        vmInfoCategory.addKey(vmInfoPidKey);
        vmInfoCategory.addKey(vmInfoRuntimeVersionKey);
        vmInfoCategory.addKey(vmInfoJavaHomeKey);
        vmInfoCategory.addKey(vmInfoMainClassKey);
        vmInfoCategory.addKey(vmInfoCommandLineKey);
        vmInfoCategory.addKey(vmInfoVmNameKey);
        vmInfoCategory.addKey(vmInfoVmArgumentsKey);
        vmInfoCategory.addKey(vmInfoVmInfoKey);
        vmInfoCategory.addKey(vmInfoVmVersionKey);
        vmInfoCategory.addKey(vmInfoEnvironmentKey);
        vmInfoCategory.addKey(vmInfoLibrariesKey);
        vmInfoCategory.addKey(vmInfoStartTimeKey);
        vmInfoCategory.addKey(vmInfoStopTimeKey);
    }

    private SystemBackend backend;

    private Map<Integer, JvmStatVmListener> listenerMap = new HashMap<Integer, JvmStatVmListener>();

    public static Collection<Category> getCategories() {
        ArrayList<Category> categories = new ArrayList<Category>();
        categories.add(vmInfoCategory);
        return categories;
    }

    public void setBackend(SystemBackend backend) {
        this.backend = backend;
    }

    @Override
    public void disconnected(HostEvent event) {
        logger.warning("Disconnected from host");
    }

    @Override
    public void vmStatusChanged(VmStatusChangeEvent event) {
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
                Map<String, String> environment = ProcessEnvironmentBuilder.build(vmId);
                List<String> loadedNativeLibraries = new ArrayList<String>();
                info = new VmInfo(vmId, startTime, stopTime,
                        extractor.getJavaVersion(), extractor.getJavaHome(),
                        extractor.getMainClass(), extractor.getCommandLine(),
                        extractor.getVmName(), extractor.getVmInfo(), extractor.getVmVersion(), extractor.getVmArguments(),
                        properties, environment, loadedNativeLibraries);
                backend.store(makeVmInfoChunk(info));
                logger.finer("Sent VM_STARTED messsage");
            } catch (MonitorException me) {
                logger.log(Level.WARNING, "error getting vm info for " + vmId, me);
            }

            if (backend.monitorNewVms()) {
                backend.addPid(vmId);
                JvmStatVmListener listener = new JvmStatVmListener(backend, vmId);
                listenerMap.put(vmId, listener);
                vm.addVmListener(listener);
            } else {
                logger.log(Level.FINE, "skipping new vm " + vmId);
            }
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

    private Chunk makeVmInfoChunk(VmInfo info) {
        Chunk chunk = new Chunk(vmInfoCategory, true);

        // FIXME save these as proper objects (ie. not as strings)

        chunk.put(vmInfoIdKey, String.valueOf(info.getVmId()));
        chunk.put(vmInfoPidKey, String.valueOf(info.getVmPid()));
        chunk.put(vmInfoRuntimeVersionKey, info.getJavaVersion());
        chunk.put(vmInfoJavaHomeKey, info.getJavaHome());
        chunk.put(vmInfoMainClassKey, info.getMainClass());
        chunk.put(vmInfoCommandLineKey, info.getJavaCommandLine());
        chunk.put(vmInfoVmArgumentsKey, info.getVmArguments());
        chunk.put(vmInfoVmInfoKey, info.getVmInfo());
        chunk.put(vmInfoVmVersionKey, info.getVmVersion());
        chunk.put(vmInfoEnvironmentKey, info.getEnvironment().toString());
        chunk.put(vmInfoLibrariesKey, info.getLoadedNativeLibraries().toString());
        chunk.put(vmInfoStartTimeKey, String.valueOf(info.getStartTimeStamp()));
        chunk.put(vmInfoStopTimeKey, String.valueOf(info.getStopTimeStamp()));
        return chunk;
    }
}
