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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;

import com.redhat.thermostat.agent.JvmStatusListener;
import com.redhat.thermostat.agent.JvmStatusNotifier;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.MemoryStat;
import com.redhat.thermostat.common.NetworkInterfaceInfo;
import com.redhat.thermostat.common.VmCpuStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class SystemBackend extends Backend implements JvmStatusNotifier, JvmStatusListener {

    private static final String NAME = "system";
    private static final String DESCRIPTION = "gathers basic information from the system";
    private static final String VENDOR = "thermostat project";
    private static final String VERSION = "0.01";

    private static final Logger logger = LoggingUtils.getLogger(SystemBackend.class);

    private long procCheckInterval = 1000; // TODO make this configurable.

    private Timer timer = null;

    private HostIdentifier hostId = null;
    private MonitoredHost host = null;
    private JvmStatHostListener hostListener = new JvmStatHostListener();

    private Set<Integer> pidsToMonitor = new CopyOnWriteArraySet<Integer>();

    private List<Category> categories = new ArrayList<Category>();

    private Category hostInfoCategory = new Category("host-info");
    private Key hostNameKey = new Key("hostname", true);
    private Key osNameKey = new Key("os_name", false);
    private Key osKernelKey = new Key("os_kernel", false);
    private Key cpuCountKey = new Key("cpu_num", false);
    private Key cpuModelKey = new Key("cpu_model", false);
    private Key hostMemoryTotalKey = new Key("memory_total", false);

    private Category networkInfoCategory = new Category("network-info");
    private Key ifaceKey = new Key("iface", true);
    private Key ip4AddrKey = new Key("ipv4addr", false);
    private Key ip6AddrKey = new Key("ipv6addr", false);

    private Category cpuStatCategory = new Category("cpu-stats");
    private Key cpu5LoadKey = new Key("5load", false);
    private Key cpu10LoadKey = new Key("10load", false);
    private Key cpu15LoadKey = new Key("15load", false);

    private Category memoryStatCategory = new Category("memory-stats");
    private Key memoryTotalKey = new Key("total", false);
    private Key memoryFreeKey = new Key("free", false);
    private Key memoryBuffersKey = new Key("buffers", false);
    private Key memoryCachedKey = new Key("cached", false);
    private Key memorySwapTotalKey = new Key("swap-total", false);
    private Key memorySwapFreeKey = new Key("swap-free", false);
    private Key memoryCommitLimitKey = new Key("commit-limit", false);

    private Category vmCpuStatCategory = new Category("vm-cpu-stats");
    private Key vmCpuVmIdKey = new Key("vm-id", false);
    private Key vmCpuLoadKey = new Key("processor-usage", false);

    {
        // Set up categories that will later be registered.
        // host-info category.
        hostInfoCategory.addKey(hostNameKey);
        hostInfoCategory.addKey(osNameKey);
        hostInfoCategory.addKey(osKernelKey);
        hostInfoCategory.addKey(cpuModelKey);
        hostInfoCategory.addKey(cpuCountKey);
        hostInfoCategory.addKey(hostMemoryTotalKey);
        hostInfoCategory.lock();
        categories.add(hostInfoCategory);

        // network-info category
        networkInfoCategory.addKey(ifaceKey);
        networkInfoCategory.addKey(ip4AddrKey);
        networkInfoCategory.addKey(ip6AddrKey);
        networkInfoCategory.lock();
        categories.add(networkInfoCategory);

        // cpu-stats category.
        cpuStatCategory.addKey(Key.TIMESTAMP);
        cpuStatCategory.addKey(cpu5LoadKey);
        cpuStatCategory.addKey(cpu10LoadKey);
        cpuStatCategory.addKey(cpu15LoadKey);
        cpuStatCategory.lock();
        categories.add(cpuStatCategory);

        // memory-stat category.
        memoryStatCategory.addKey(Key.TIMESTAMP);
        memoryStatCategory.addKey(memoryTotalKey);
        memoryStatCategory.addKey(memoryFreeKey);
        memoryStatCategory.addKey(memoryBuffersKey);
        memoryStatCategory.addKey(memoryCachedKey);
        memoryStatCategory.addKey(memorySwapTotalKey);
        memoryStatCategory.addKey(memorySwapFreeKey);
        memoryStatCategory.addKey(memoryCommitLimitKey);
        memoryStatCategory.lock();
        categories.add(memoryStatCategory);

        // vm-cpu-stat category
        vmCpuStatCategory.addKey(vmCpuLoadKey);
        vmCpuStatCategory.addKey(vmCpuVmIdKey);
        categories.add(vmCpuStatCategory);

        categories.addAll(JvmStatHostListener.getCategories());
        categories.addAll(JvmStatVmListener.getCategories());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public synchronized boolean activate() {
        if (timer != null) {
            return true;
        }

        addJvmStatusListener(this);

        if (!getObserveNewJvm()) {
            logger.fine("not monitoring new vms");
        }
        store(makeHostChunk(HostInfoBuilder.build()));

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                store(makeCpuChunk(new CpuStatBuilder().build()));
                for (Iterator<NetworkInterfaceInfo> iter = NetworkInfoBuilder.build().getInterfacesIterator(); iter.hasNext();) {
                    store(makeNetworkChunk(iter.next()));
                }
                store(makeMemoryChunk(new MemoryStatBuilder().build()));

                for (Integer pid : pidsToMonitor) {
                    new VmCpuStatBuilder();
                    store(makeVmCpuChunk(VmCpuStatBuilder.build(pid)));
                }
            }
        }, 0, procCheckInterval);

        try {
            hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
            hostListener.setBackend(this);
            host.addHostListener(hostListener);
        } catch (MonitorException me) {
            logger.log(Level.WARNING, "problems with connecting jvmstat to local machine", me);
        } catch (URISyntaxException use) {
            logger.log(Level.WARNING, "problems with connecting jvmstat to local machine", use);
        }

        return true;
    }

    @Override
    public synchronized boolean deactivate() {
        if (timer == null) {
            return true;
        }

        timer.cancel();
        timer = null;

        try {
            host.removeHostListener(hostListener);
        } catch (MonitorException me) {
            logger.log(Level.INFO, "something went wrong in jvmstat's listening to this host");
        }
        host = null;
        hostId = null;

        return true;
    }

    @Override
    public synchronized boolean isActive() {
        return (timer != null);
    }

    @Override
    public String getConfigurationValue(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Iterator<Category> getCategoryIterator() {
        return categories.iterator();
    }

    private Chunk makeCpuChunk(CpuStat cpuStat) {
        Chunk chunk = new Chunk(cpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, Long.toString(cpuStat.getTimeStamp()));
        chunk.put(cpu5LoadKey, Double.toString(cpuStat.getLoad5()));
        chunk.put(cpu10LoadKey, Double.toString(cpuStat.getLoad10()));
        chunk.put(cpu15LoadKey, Double.toString(cpuStat.getLoad15()));
        return chunk;
    }

    private Chunk makeHostChunk(HostInfo hostInfo) {
        Chunk chunk = new Chunk(hostInfoCategory, false);
        chunk.put(hostNameKey, hostInfo.getHostname());
        chunk.put(osNameKey, hostInfo.getOsName());
        chunk.put(osKernelKey, hostInfo.getOsKernel());
        chunk.put(cpuCountKey, Integer.toString(hostInfo.getCpuCount()));
        chunk.put(cpuModelKey, hostInfo.getCpuModel());
        chunk.put(hostMemoryTotalKey, Long.toString(hostInfo.getTotalMemory()));
        return chunk;
    }

    private Chunk makeNetworkChunk(NetworkInterfaceInfo info) {
        Chunk chunk = new Chunk(networkInfoCategory, true);
        chunk.put(ifaceKey, info.getInterfaceName());
        String ip4 = info.getIp4Addr();
        if (ip4 != null) {
            chunk.put(ip4AddrKey, ip4);
        }
        String ip6 = info.getIp6Addr();
        if (ip6 != null) {
            chunk.put(ip6AddrKey, ip6);
        }
        return chunk;
    }

    private Chunk makeMemoryChunk(MemoryStat mem) {
        Chunk chunk = new Chunk(memoryStatCategory, false);
        chunk.put(Key.TIMESTAMP, Long.toString(mem.getTimeStamp()));
        chunk.put(memoryTotalKey, Long.toString(mem.getTotal()));
        chunk.put(memoryFreeKey, Long.toString(mem.getFree()));
        chunk.put(memoryBuffersKey, Long.toString(mem.getBuffers()));
        chunk.put(memoryCachedKey, Long.toString(mem.getCached()));
        chunk.put(memorySwapTotalKey, Long.toString(mem.getSwapTotal()));
        chunk.put(memorySwapFreeKey, Long.toString(mem.getSwapFree()));
        chunk.put(memoryCommitLimitKey, Long.toString(mem.getCommitLimit()));
        return chunk;
    }

    private Chunk makeVmCpuChunk(VmCpuStat stat) {
        Chunk chunk = new Chunk(vmCpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, Long.toString(stat.getTimeStamp()));
        chunk.put(vmCpuVmIdKey, Integer.toString(stat.getVmId()));
        chunk.put(vmCpuLoadKey, Double.toString(stat.getCpuLoad()));
        return chunk;
    }

    @Override
    public boolean attachToNewProcessByDefault() {
        return true;
    }

    @Override
    public void addJvmStatusListener(JvmStatusListener listener) {
        hostListener.addJvmStatusListener(listener);
    }

    @Override
    public void removeJvmStatusListener(JvmStatusListener listener) {
        hostListener.removeJvmStatusListener(listener);
    }

    @Override
    public void jvmStarted(int vmId) {
        if (getObserveNewJvm()) {
            pidsToMonitor.add(vmId);
        }
    }

    @Override
    public void jvmStopped(int vmId) {
        pidsToMonitor.remove(vmId);
    }
}
