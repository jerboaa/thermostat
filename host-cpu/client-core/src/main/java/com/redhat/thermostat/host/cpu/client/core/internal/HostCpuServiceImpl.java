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

package com.redhat.thermostat.host.cpu.client.core.internal;

import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.client.core.NameMatchingRefFilter;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.host.cpu.client.core.HostCpuService;
import com.redhat.thermostat.host.cpu.client.core.HostCpuViewProvider;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;

public class HostCpuServiceImpl implements HostCpuService {
    
    private static final int ORDER = ORDER_CPU_GROUP;
    private static final Filter<HostRef> FILTER = new NameMatchingRefFilter<>();

    private ApplicationService appSvc;
    private HostInfoDAO hostInfoDAO;
    private CpuStatDAO cpuStatDAO;
    private HostCpuViewProvider viewProvider;
    
    public HostCpuServiceImpl(ApplicationService appSvc, HostInfoDAO hostInfoDAO,
            CpuStatDAO cpuStatDAO, HostCpuViewProvider viewProvider) {
        this.appSvc = appSvc;
        this.hostInfoDAO = hostInfoDAO;
        this.cpuStatDAO = cpuStatDAO;
        this.viewProvider = viewProvider;
    }

    @Override
    public Filter<HostRef> getFilter() {
        return FILTER;
    }

    @Override
    public InformationServiceController<HostRef> getInformationServiceController(
            HostRef ref) {
        return new HostCpuController(appSvc, hostInfoDAO, cpuStatDAO, ref, viewProvider);
    }

    @Override
    public int getOrderValue() {
        return ORDER;
    }

}

