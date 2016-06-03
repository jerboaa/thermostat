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

package com.redhat.thermostat.thread.client.swing.internal;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.thread.client.common.view.StackTraceProfilerView;
import com.redhat.thermostat.ui.swing.components.graph.GraphContainer;
import com.redhat.thermostat.ui.swing.model.Trace;
import com.redhat.thermostat.ui.swing.model.graph.GraphModel;

import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.Component;

/**
 */
public class SwingStackTraceProfilerView extends StackTraceProfilerView implements SwingComponent {

    private ComponentVisibilityNotifier visibilityNotifier;

    private ThermostatScrollPane contentPane;
    private GraphModel graphModel;
    private GraphContainer graphContainer;

    public SwingStackTraceProfilerView() {
        graphContainer = new GraphContainer();

        contentPane = new ThermostatScrollPane(graphContainer);
        contentPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        visibilityNotifier = new ComponentVisibilityNotifier();
        visibilityNotifier.initialize(graphContainer, notifier);
    }

    @Override
    public void createModel(final String modelID) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                graphModel = new GraphModel(modelID);
                graphContainer.setModel(graphModel);
            }
        });
    }

    @Override
    public void rebuild() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                graphModel.rebuild();
            }
        });
    }

    @Override
    public void addTrace(final Trace trace) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                graphModel.addTrace(trace);
            }
        });
    }

    @Override
    public void clear() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                graphModel.clear();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return contentPane;
    }
}
