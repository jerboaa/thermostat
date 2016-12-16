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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.redhat.thermostat.client.core.NameMatchingRefFilter;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumperService;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class HeapDumperServiceImpl implements HeapDumperService {
    
    private static final int ORDER = ORDER_MEMORY_GROUP + 60;
    private ApplicationService appService;
    private VmInfoDAO vmInfoDao;
    private VmMemoryStatDAO vmMemoryStatDao;
    private HeapDAO heapDao;

    private Filter<VmRef> filter = new NameMatchingRefFilter<>();

    private HeapViewProvider viewProvider;
    private HeapDumpDetailsViewProvider detailsViewProvider;
    private HeapHistogramViewProvider histogramViewProvider;
    private HeapTreeMapViewProvider treeMapViewProvider;
    private ObjectDetailsViewProvider objectDetailsViewProvider;
    private ObjectRootsViewProvider objectRootsViewProvider;

    private ProgressNotifier notifier;
    
    private HeapDumpListViewProvider heapDumpListViewProvider;

    private Map<VmRef, HeapDumpController> controllers = new ConcurrentHashMap<>();
    
    public HeapDumperServiceImpl(ApplicationService appService,
            VmInfoDAO vmInfoDao, VmMemoryStatDAO vmMemoryStatDao,
            HeapDAO heapDao, HeapViewProvider viewProvider,
            HeapDumpDetailsViewProvider detailsViewProvider,
            HeapHistogramViewProvider histogramViewProvider,
            HeapTreeMapViewProvider treeMapViewProvider,
            ObjectDetailsViewProvider objectDetailsViewProvider,
            ObjectRootsViewProvider objectRootsViewProvider,
            HeapDumpListViewProvider heapDumpListViewProvider,
            ProgressNotifier notifier) {
        this.vmInfoDao = vmInfoDao;
        this.vmMemoryStatDao = vmMemoryStatDao;
        this.heapDao = heapDao;
        this.appService = appService;
        this.viewProvider = viewProvider;
        this.detailsViewProvider = detailsViewProvider;
        this.histogramViewProvider = histogramViewProvider;
        this.treeMapViewProvider = treeMapViewProvider;
        this.objectDetailsViewProvider = objectDetailsViewProvider;
        this.objectRootsViewProvider = objectRootsViewProvider;
        this.heapDumpListViewProvider = heapDumpListViewProvider;
        this.notifier = notifier;
    }

    @Override
    public InformationServiceController<VmRef> getInformationServiceController(VmRef ref) {
        if (controllers.get(ref) == null) {
            controllers.put(ref, new HeapDumpController(vmMemoryStatDao, vmInfoDao, heapDao, ref, appService, viewProvider, detailsViewProvider, histogramViewProvider, treeMapViewProvider, objectDetailsViewProvider, objectRootsViewProvider, heapDumpListViewProvider, notifier));
        }
        return controllers.get(ref);
    }

    @Override
    public Filter<VmRef> getFilter() {
        return filter;
    }

    @Override
    public int getOrderValue() {
        return ORDER;
    }
}
