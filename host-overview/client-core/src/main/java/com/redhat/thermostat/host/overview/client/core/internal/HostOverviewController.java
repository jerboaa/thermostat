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

package com.redhat.thermostat.host.overview.client.core.internal;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.host.overview.client.core.HostOverviewView;
import com.redhat.thermostat.host.overview.client.core.HostOverviewViewProvider;
import com.redhat.thermostat.host.overview.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class HostOverviewController implements InformationServiceController<HostRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final HostInfoDAO hostInfoDAO;
    private final NetworkInterfaceInfoDAO networkInfoDAO;

    private final Timer backgroundUpdateTimer;
    private final Timer statsTimer;

    private final List<String> knownNetworkIfaces = new ArrayList<>();

    private final HostOverviewView view;

    public HostOverviewController(ApplicationService appSvc,
                                  HostInfoDAO hostInfoDAO,
                                  NetworkInterfaceInfoDAO networkInfoDAO,
                                  final CpuStatDAO cpuDao,
                                  final MemoryStatDAO memoryDao,
                                  final HostRef ref,
                                  HostOverviewViewProvider provider)
    {        
        this.hostInfoDAO = hostInfoDAO;
        this.networkInfoDAO = networkInfoDAO;

        final Vector<String> networkTableColumnVector;
        networkTableColumnVector = new Vector<String>();
        networkTableColumnVector.add(translator.localize(LocaleResources.NETWORK_INTERFACE_COLUMN).getContents());
        networkTableColumnVector.add(translator.localize(LocaleResources.NETWORK_IPV4_COLUMN).getContents());
        networkTableColumnVector.add(translator.localize(LocaleResources.NETWORK_IPV6_COLUMN).getContents());

        backgroundUpdateTimer = appSvc.getTimerFactory().createTimer();
        backgroundUpdateTimer.setAction(new Runnable() {
            @Override
            public void run() {
                HostInfo hostInfo = HostOverviewController.this.hostInfoDAO.getHostInfo(ref);
                view.setHostName(hostInfo.getHostname());
                view.setOsName(hostInfo.getOsName());
                view.setOsKernel(hostInfo.getOsKernel());
                view.setCpuModel(hostInfo.getCpuModel());
                view.setCpuCount(String.valueOf(hostInfo.getCpuCount()));

                String readableTotalMemory = new Size(hostInfo.getTotalMemory(), Size.Unit.B).toString();
                view.setTotalMemory(readableTotalMemory);

                List<NetworkInterfaceInfo> networkInfo =
                        HostOverviewController.this.networkInfoDAO.getNetworkInterfaces(ref);

                boolean firstRun = knownNetworkIfaces.isEmpty();
                if (firstRun) {
                    List<Object[]> data = new ArrayList<Object[]>();
                    for (NetworkInterfaceInfo info: networkInfo) {
                        String ifaceName = info.getInterfaceName();
                        String ipv4 = info.getIp4Addr();
                        String ipv6 = info.getIp6Addr();
                        data.add(new String[] { ifaceName, ipv4, ipv6 });
                        knownNetworkIfaces.add(ifaceName);
                    }
                    view.setInitialNetworkTableData(data.toArray(new Object[0][0]));
                } else {
                    for (NetworkInterfaceInfo info: networkInfo) {
                        String ifaceName = info.getInterfaceName();
                        String ipv4 = info.getIp4Addr();
                        String ipv6 = info.getIp6Addr();
                        int index = knownNetworkIfaces.indexOf(ifaceName);
                        int row = index;
                        view.updateNetworkTableData(index, 0, ifaceName);
                        view.updateNetworkTableData(row, 1, ipv4);
                        view.updateNetworkTableData(row, 2, ipv6);
                    }
                }
            }
        });
        backgroundUpdateTimer.setSchedulingType(SchedulingType.FIXED_RATE);
        backgroundUpdateTimer.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdateTimer.setInitialDelay(0);
        backgroundUpdateTimer.setDelay(5);

        view = provider.createView();

        view.setNetworkTableColumns(networkTableColumnVector.toArray());

        view.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        stop();
                        break;
                    case VISIBLE:
                        start();
                        break;
                    default:
                        throw new NotImplementedException("unhandled: " + actionEvent.getActionId());
                }
            }
        });
        
        statsTimer = appSvc.getTimerFactory().createTimer();
        statsTimer.setDelay(500);
        statsTimer.setInitialDelay(0);
        statsTimer.setTimeUnit(TimeUnit.MILLISECONDS);
        statsTimer.setSchedulingType(SchedulingType.FIXED_RATE);
        statsTimer.setAction(new Runnable() {
            @Override
            public void run() {
                List<CpuStat> cpuStats = cpuDao.getLatestCpuStats(ref, System.currentTimeMillis() - 5000);
                if (!cpuStats.isEmpty()) {
                    CpuStat stat = cpuStats.get(0);
                    double [] cpuUsage = stat.getPerProcessorUsage();
                    // dumb average
                    int cpus = cpuUsage.length;
                    double total = 0;
                    for (double thisCPU : cpuUsage) {
                        total += thisCPU;
                    }
                    total = total/cpus;
                    view.setCPUPercentage((float) total);
                }
                
                List<MemoryStat> memoryStats =
                        memoryDao.getLatestMemoryStats(ref, System.currentTimeMillis() - 5000);
                if (!memoryStats.isEmpty()) {
                    MemoryStat stat = memoryStats.get(0);
                    
                    long total = stat.getTotal();
                    long buffered = stat.getBuffers();
                    long cached = stat.getCached();
                    long free = stat.getFree();
                    long used = total - free;
                    
                    long usedMemory =  (used - (buffered + cached));
                    
                    float usedMemoryPercent = (float) usedMemory/total*100;
                    view.setMemoryPercentage(usedMemoryPercent);
                }
            }
        });
    }

    private void start() {
        backgroundUpdateTimer.start();
        statsTimer.start();
    }

    private void stop() {
        backgroundUpdateTimer.stop();
        statsTimer.stop();
    }

    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.HOST_INFO_TAB_OVERVIEW);
    }
}

