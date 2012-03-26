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
import java.util.Collections;
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
import com.redhat.thermostat.common.dao.CpuStatConverter;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.HostInfoConverter;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.MemoryStatConverter;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoConverter;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmCpuStatConverter;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmGcStatDAO;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.model.NetworkInterfaceInfo;
import com.redhat.thermostat.common.storage.Category;
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

    private static List<Category> categories = new ArrayList<Category>();


    static {
        // Set up categories that will later be registered.
        categories.add(CpuStatDAO.cpuStatCategory);
        categories.add(HostInfoDAO.hostInfoCategory);
        categories.add(MemoryStatDAO.memoryStatCategory);
        categories.add(NetworkInterfaceInfoDAO.networkInfoCategory);
        categories.add(VmClassStatDAO.vmClassStatsCategory);
        categories.add(VmCpuStatDAO.vmCpuStatCategory);
        categories.add(VmGcStatDAO.vmGcStatsCategory);
        categories.add(VmInfoDAO.vmInfoCategory);
        categories.add(VmMemoryStatDAO.vmMemoryStatsCategory);
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
        store(new HostInfoConverter().hostInfoToChunk(new HostInfoBuilder(new ProcDataSource()).build()));

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ProcDataSource dataSource = new ProcDataSource();
                store(new CpuStatConverter().cpuStatToChunk(new CpuStatBuilder(dataSource).build()));
                for (NetworkInterfaceInfo info: NetworkInfoBuilder.build()) {
                    store(new NetworkInterfaceInfoConverter().networkInfoToChunk(info));
                }
                store(new MemoryStatConverter().memoryStatToChunk(new MemoryStatBuilder(dataSource).build()));

                for (Integer pid : pidsToMonitor) {
                    new VmCpuStatBuilder();
                    store(new VmCpuStatConverter().vmCpuStatToChunk(VmCpuStatBuilder.build(pid)));
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
    protected Collection<Category> getCategories() {
        return Collections.unmodifiableCollection(categories);
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
