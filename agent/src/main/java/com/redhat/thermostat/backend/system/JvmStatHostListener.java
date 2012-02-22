/*
 * Copyright 2012 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

import com.redhat.thermostat.agent.JvmStatusListener;
import com.redhat.thermostat.agent.JvmStatusNotifier;
import com.redhat.thermostat.agent.storage.Category;
import com.redhat.thermostat.agent.storage.Chunk;
import com.redhat.thermostat.agent.storage.Key;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class JvmStatHostListener implements HostListener, JvmStatusNotifier {

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

    private Set<JvmStatusListener> statusListeners = new CopyOnWriteArraySet<JvmStatusListener>();

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

            if (backend.getObserveNewJvm()) {
                JvmStatVmListener listener = new JvmStatVmListener(backend, vmId);
                listenerMap.put(vmId, listener);
                vm.addVmListener(listener);
            } else {
                logger.log(Level.FINE, "skipping new vm " + vmId);
            }
            for (JvmStatusListener statusListener : statusListeners) {
                statusListener.jvmStarted(vmId);
            }
        }
    }

    private void sendStoppedVM(Integer vmId, MonitoredHost host)
            throws URISyntaxException, MonitorException {
        VmIdentifier resolvedVmID = host.getHostIdentifier().resolve(
                new VmIdentifier(vmId.toString()));
        if (resolvedVmID != null) {
            long stopTime = System.currentTimeMillis();
            listenerMap.remove(vmId);
            for (JvmStatusListener statusListener : statusListeners) {
                statusListener.jvmStopped(vmId);
            }
            backend.update(makeVmInfoUpdateStoppedChunk(vmId, stopTime));
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
        chunk.put(vmInfoVmNameKey, info.getVmName());
        chunk.put(vmInfoVmInfoKey, info.getVmInfo());
        chunk.put(vmInfoVmVersionKey, info.getVmVersion());
        chunk.put(vmInfoEnvironmentKey, info.getEnvironment().toString());
        chunk.put(vmInfoLibrariesKey, info.getLoadedNativeLibraries().toString());
        chunk.put(vmInfoStartTimeKey, String.valueOf(info.getStartTimeStamp()));
        chunk.put(vmInfoStopTimeKey, String.valueOf(info.getStopTimeStamp()));
        return chunk;
    }

    private Chunk makeVmInfoUpdateStoppedChunk(int vmId, long stopTimeStamp) {
        Chunk chunk = new Chunk(vmInfoCategory, false);
        chunk.put(vmInfoIdKey, String.valueOf(vmId));
        chunk.put(vmInfoStopTimeKey, String.valueOf(stopTimeStamp));
        return chunk;
    }

    @Override
    public void addJvmStatusListener(JvmStatusListener listener) {
        statusListeners.add(listener);
    }

    @Override
    public void removeJvmStatusListener(JvmStatusListener listener) {
        statusListeners.remove(listener);
    }
}
