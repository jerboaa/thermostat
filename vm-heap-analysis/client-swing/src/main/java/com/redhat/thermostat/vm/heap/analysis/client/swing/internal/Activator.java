/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // All automatically unregistered upon Activator.stop
        HeapViewProvider viewProvider = new SwingHeapViewProvider();
        context.registerService(HeapViewProvider.class.getName(), viewProvider, null);
        HeapDumpDetailsViewProvider detailsViewProvider = new SwingHeapDumpDetailsViewProvider();
        context.registerService(HeapDumpDetailsViewProvider.class.getName(), detailsViewProvider, null);
        HeapHistogramViewProvider histogramViewProvider = new SwingHeapHistogramViewProvider();
        context.registerService(HeapHistogramViewProvider.class.getName(), histogramViewProvider, null);
        ObjectDetailsViewProvider objectDetailsViewProvider = new SwingObjectDetailsViewProvider();
        context.registerService(ObjectDetailsViewProvider.class.getName(), objectDetailsViewProvider, null);
        ObjectRootsViewProvider objectRootsViewProvider = new SwingObjectRootsViewProvider();
        context.registerService(ObjectRootsViewProvider.class.getName(), objectRootsViewProvider, null);
        HeapDumpListViewProvider heapListViewProvider = new SwingHeapDumpListViewProvider();
        context.registerService(HeapDumpListViewProvider.class.getName(), heapListViewProvider, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
    
}

