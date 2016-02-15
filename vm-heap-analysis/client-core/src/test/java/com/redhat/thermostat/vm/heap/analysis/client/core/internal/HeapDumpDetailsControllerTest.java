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

import junit.framework.Assert;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;

public class HeapDumpDetailsControllerTest {

    private HeapDumpDetailsView view;
    private HeapDumpDetailsViewProvider viewProvider;
    private HeapHistogramViewProvider histogramProvider;
    private HeapTreeMapViewProvider treeMapProvider;
    private ObjectDetailsViewProvider objectDetailsProvider;
    private ObjectRootsViewProvider objectRootsProvider;

    @Before
    public void setUp() {
        viewProvider = mock(HeapDumpDetailsViewProvider.class);
        view = mock(HeapDumpDetailsView.class);
        when(viewProvider.createView()).thenReturn(view);

        HeapHistogramView histogramView = mock(HeapHistogramView.class);
        histogramProvider = mock(HeapHistogramViewProvider.class);
        when(histogramProvider.createView()).thenReturn(histogramView);

        HeapTreeMapView treeMapView = mock(HeapTreeMapView.class);
        treeMapProvider = mock(HeapTreeMapViewProvider.class);
        when(treeMapProvider.createView()).thenReturn(treeMapView);

        ObjectDetailsView objectView = mock(ObjectDetailsView.class);
        objectDetailsProvider = mock(ObjectDetailsViewProvider.class);
        when(objectDetailsProvider.createView()).thenReturn(objectView);

        ObjectRootsView objectRootsView = mock(ObjectRootsView.class);
        objectRootsProvider = mock(ObjectRootsViewProvider.class);
        when(objectRootsProvider.createView()).thenReturn(objectRootsView);
    }

    @After
    public void tearDown() {
        viewProvider = null;
        histogramProvider = null;
        objectDetailsProvider = null;
        objectRootsProvider = null;
    }

    @Test
    public void testSetDumpFailsWithEmptyDump() throws IOException {
        HeapDumpDetailsController controller = setupController();

        HeapDump emptyDump = mock(HeapDump.class);
        when(emptyDump.getHistogram()).thenReturn(null);

        boolean caught = false;
        try {
            controller.setDump(emptyDump);
        } catch (NullPointerException e) {
            caught = true;
        }
        assertTrue("Null pointer exception expected", caught);
    }

    @Test
    public void testSetDumpWorksWithValidDump() throws IOException {
        HeapDumpDetailsController controller = setupController();

        HeapDump dump = mock(HeapDump.class);
        ObjectHistogram histogram = mock(ObjectHistogram.class);
        when(dump.getHistogram()).thenReturn(histogram);

        try {
            controller.setDump(dump);
        } catch (NullPointerException e) {
            Assert.fail("Did not expect null pointer exception");
        }

        verify(dump).searchObjects(isA(String.class), anyInt());
        verify(view).updateView(isA(HeapHistogramView.class), isA(ObjectDetailsView.class),
                isA(HeapTreeMapView.class));
    }

    private HeapDumpDetailsController setupController() {
        ApplicationService appService = mock(ApplicationService.class);
        return new HeapDumpDetailsController(
                appService, viewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider);
    }

}
