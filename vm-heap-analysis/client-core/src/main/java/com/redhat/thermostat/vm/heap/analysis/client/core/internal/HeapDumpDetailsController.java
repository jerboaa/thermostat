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

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView.HistogramAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

public class HeapDumpDetailsController {

    private static final Logger log = LoggingUtils.getLogger(HeapDumpDetailsController.class);

    private final ApplicationService appService;

    private HeapDumpDetailsView view;
    private HeapHistogramView heapHistogramView;
    private HeapDump heapDump;
    private HeapTreeMapView heapTreeMapView;
    private HeapHistogramViewProvider histogramViewProvider;
    private ObjectDetailsViewProvider objectDetailsViewProvider;
    private ObjectRootsViewProvider objectRootsViewProvider;

    public HeapDumpDetailsController(ApplicationService appService, HeapDumpDetailsViewProvider viewProvider, HeapHistogramViewProvider histogramProvider, HeapTreeMapViewProvider treeMapProvider, ObjectDetailsViewProvider objectDetailsProvider, ObjectRootsViewProvider objectRootsProvider) {
        this.appService = appService;
        this.histogramViewProvider = histogramProvider;
        this.objectDetailsViewProvider = objectDetailsProvider;
        this.objectRootsViewProvider = objectRootsProvider;
        view = viewProvider.createView();
        heapTreeMapView = treeMapProvider.createView();
    }

    public void setDump(HeapDump dump) {
        HeapDump previous = this.heapDump;
        this.heapDump = dump;

        if (dump.equals(previous)) {
            return;
        }

        ObjectHistogram histogram = readHistogram();
        Objects.requireNonNull(histogram);

        heapHistogramView = histogramViewProvider.createView();
        heapHistogramView.setHistogram(histogram);
        heapHistogramView.addHistogramActionListener(new ActionListener<HistogramAction>() {
            @Override
            public void actionPerformed(ActionEvent<HistogramAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case SEARCH:
                        searchForObject((String) actionEvent.getPayload());
                        break;
                    default:
                        throw new NotImplementedException("unknown action fired by " + actionEvent.getSource());
                }
            }
        });

        heapTreeMapView.display(histogram);

        ObjectDetailsController controller = new ObjectDetailsController(appService, dump,
                objectDetailsViewProvider, objectRootsViewProvider);
        ObjectDetailsView detailsView = controller.getView();

        view.updateView(heapHistogramView, detailsView, heapTreeMapView);

        // do a dummy search right now to prep the index
        dump.searchObjects("A_RANDOM_PATTERN", 1);
    }

    private ObjectHistogram readHistogram() {
        try {
            return heapDump.getHistogram();
        } catch (IOException e) {
            log.log(Level.SEVERE, "unexpected error while reading heap dump", e);
            return null;
        }
    }

    private void searchForObject(final String searchText) {
        appService.getApplicationExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ObjectHistogram toDisplay;
                if (searchText == null || searchText.trim().isEmpty()) {
                    toDisplay = readHistogram();
                    if (toDisplay == null) {
                        return;
                    }
                } else {
                    toDisplay = new ObjectHistogram();
                    Collection<String> objectIds = heapDump.wildcardSearch(searchText);
                    for (String id : objectIds) {
                        JavaHeapObject heapObject = heapDump.findObject(id);
                        toDisplay.addThing(heapObject);
                    }
                }
                heapHistogramView.setHistogram(toDisplay);
            }
        });
    }

    public BasicView getView() {
        return view;
    }

}
