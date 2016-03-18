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

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.testutils.StubExecutor;
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
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeapDumpDetailsControllerTest {

    private HeapDumpDetailsView view;
    private HeapDumpDetailsViewProvider viewProvider;
    private HeapHistogramViewProvider histogramProvider;
    private HeapHistogramView histogramView;
    private HeapTreeMapViewProvider treeMapProvider;
    private ObjectDetailsViewProvider objectDetailsProvider;
    private ObjectRootsViewProvider objectRootsProvider;
    private ApplicationService appService;

    private HeapDumpDetailsController controller;

    @Before
    public void setUp() {
        viewProvider = mock(HeapDumpDetailsViewProvider.class);
        view = mock(HeapDumpDetailsView.class);
        when(viewProvider.createView()).thenReturn(view);

        histogramView = mock(HeapHistogramView.class);
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

        appService = mock(ApplicationService.class);
        when(appService.getApplicationExecutor()).thenReturn(new StubExecutor());

        controller = new HeapDumpDetailsController(appService, viewProvider, histogramProvider, treeMapProvider,
                objectDetailsProvider, objectRootsProvider);
    }

    @Test(expected = NullPointerException.class)
    public void testSetDumpFailsWithEmptyDump() throws IOException {
        HeapDump emptyDump = mock(HeapDump.class);
        when(emptyDump.getHistogram()).thenReturn(null);

        controller.setDump(emptyDump);
    }

    @Test
    public void testSetDumpWorksWithValidDump() throws IOException {
        HeapDump dump = mock(HeapDump.class);
        ObjectHistogram histogram = mock(ObjectHistogram.class);
        when(dump.getHistogram()).thenReturn(histogram);

        try {
            controller.setDump(dump);
        } catch (NullPointerException e) {
            fail("Did not expect null pointer exception");
        }

        verify(dump).searchObjects(isA(String.class), anyInt());
        verify(view).updateView(isA(HeapHistogramView.class), isA(ObjectDetailsView.class),
                isA(HeapTreeMapView.class));
    }

    @Test
    public void testEmptySearchStringDisplaysFullHistogram() throws IOException {
        HeapDump dump = mock(HeapDump.class);
        ObjectHistogram histogram = mock(ObjectHistogram.class);
        when(dump.getHistogram()).thenReturn(histogram);

        controller.setDump(dump);
        verify(histogramView).setHistogram(histogram);

        ArgumentCaptor<ActionListener> captor =
                ArgumentCaptor.forClass(ActionListener.class);
        verify(histogramView).addHistogramActionListener(captor.capture());
        ActionListener<HeapHistogramView.HistogramAction> listener = captor.getValue();

        ActionEvent<HeapHistogramView.HistogramAction> actionEvent = new ActionEvent<>(this, HeapHistogramView.HistogramAction.SEARCH);
        actionEvent.setPayload("");
        listener.actionPerformed(actionEvent);

        verify(histogramView, times(2)).setHistogram(histogram);

        verify(dump, never()).wildcardSearch(anyString());
        verify(dump, never()).findObject(anyString());
    }

    @Test
    public void testNonEmptySearchStringDisplaysFilteredHistogram() throws IOException {
        HeapDump dump = mock(HeapDump.class);
        ObjectHistogram histogram = mock(ObjectHistogram.class);
        String objectId = "objectId";
        when(dump.getHistogram()).thenReturn(histogram);
        when(dump.wildcardSearch(anyString())).thenReturn(Collections.singleton(objectId));
        JavaHeapObject javaHeapObject = mock(JavaHeapObject.class);
        when(javaHeapObject.getClazz()).thenReturn(mock(JavaClass.class));
        when(dump.findObject(anyString())).thenReturn(javaHeapObject);

        controller.setDump(dump);
        verify(histogramView).setHistogram(histogram);

        ArgumentCaptor<ActionListener> captor =
                ArgumentCaptor.forClass(ActionListener.class);
        verify(histogramView).addHistogramActionListener(captor.capture());
        ActionListener<HeapHistogramView.HistogramAction> listener = captor.getValue();

        ActionEvent<HeapHistogramView.HistogramAction> actionEvent = new ActionEvent<>(this, HeapHistogramView.HistogramAction.SEARCH);
        String searchTerm = "SEARCH_TERM";
        actionEvent.setPayload(searchTerm);
        listener.actionPerformed(actionEvent);

        verify(histogramView, times(1)).setHistogram(histogram); // verify full histogram still displayed only once
        verify(dump).wildcardSearch(searchTerm);
        verify(dump).findObject(objectId);

        ArgumentCaptor<ObjectHistogram> histogramCaptor = ArgumentCaptor.forClass(ObjectHistogram.class);
        verify(histogramView, times(2)).setHistogram(histogramCaptor.capture());
        ObjectHistogram objectHistogram = histogramCaptor.getValue();
        assertThat(objectHistogram.getHistogram().size(), is(1));
    }

}
