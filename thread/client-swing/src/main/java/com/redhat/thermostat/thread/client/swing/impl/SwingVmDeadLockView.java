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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatScrollBar;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTextArea;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.DeadlockParser;
import com.redhat.thermostat.thread.client.common.DeadlockParser.Information;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;

public class SwingVmDeadLockView extends VmDeadLockView implements SwingComponent {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private final JPanel actualComponent = new JPanel();
    private final JButton checkForDeadlockButton;

    private final JSplitPane deadlockTextAndVisualization = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private final JPanel graphical = new JPanel();
    private final ThermostatTextArea description = new ThermostatTextArea();
    /**
     * Whether to set the divider's location. Do this only once to set a sane
     * initial value but don't change anything after and allow the user to tweak
     * this as appropriate.
     */
    private boolean dividerLocationSet = false;

    public SwingVmDeadLockView() {
        actualComponent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        checkForDeadlockButton = new JButton(translate.localize(LocaleResources.CHECK_FOR_DEADLOCKS).getContents());
        checkForDeadlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deadLockNotifier.fireAction(VmDeadLockViewAction.CHECK_FOR_DEADLOCK);
            }
        });

        actualComponent.add(checkForDeadlockButton, c);

        c.anchor = GridBagConstraints.LINE_START;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        description.setEditable(false);

        JScrollPane scrollPane = new ThermostatScrollPane(description);

        graphical.setLayout(new BorderLayout());

        deadlockTextAndVisualization.setLeftComponent(scrollPane);
        deadlockTextAndVisualization.setRightComponent(graphical);

        actualComponent.add(deadlockTextAndVisualization, c);

        new ComponentVisibilityNotifier().initialize(actualComponent, notifier);
    }

    @Override
    public void setDeadLockInformation(final Information parsed, final String rawText) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                graphical.removeAll();

                if (!dividerLocationSet) {
                    // 0.7 is chosen empirically to show a bit more of the text than the gui
                    deadlockTextAndVisualization.setDividerLocation(0.7);
                    deadlockTextAndVisualization.revalidate();
                    dividerLocationSet = true;
                }

                if (parsed != null) {
                    FontMetrics metrics = graphical.getGraphics().getFontMetrics();
                    graphical.add(createGraph(parsed, metrics), BorderLayout.CENTER);
                }

                graphical.revalidate();
                graphical.repaint();

                description.setText(rawText);
            }
        });
    }

    private mxGraphComponent createGraph(Information info, FontMetrics fontMetrics) {

        final mxGraph graph = new mxGraph() {

            /* Show tooltips for vertices and edges */
            @Override
            public String getToolTipForCell(Object source) {
                mxCell cell = ((mxCell) source);

                if (cell.getValue() instanceof GraphItem) {
                    return ((GraphItem) cell.getValue()).getTooltip();
                } else {
                    return super.getToolTipForCell(cell);
                }
            }


            /* Prevent modifying the contents of edges or vertices */
            @Override
            public boolean isCellEditable(Object cell) {
                return false;
            }

            /* Prevent moving edges away from the vertices */
            @Override
            public boolean isCellSelectable(Object cell) {
                return !model.isEdge(cell);
            }

        };

        Object parent = graph.getDefaultParent();

        addDeadlockToGraph(info, graph, parent, fontMetrics);

        graph.setAutoSizeCells(true);
        graph.setCellsResizable(true);

        final mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setTextAntiAlias(true);
        graphComponent.setToolTips(true);
        graphComponent.setConnectable(false);

        graphComponent.setHorizontalScrollBar(new ThermostatScrollBar(ThermostatScrollBar.HORIZONTAL));
        graphComponent.setVerticalScrollBar(new ThermostatScrollBar(ThermostatScrollBar.VERTICAL));

        Map<String, Object> style = graph.getStylesheet().getDefaultVertexStyle();
        style.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER);
        graph.getStylesheet().setDefaultVertexStyle(style);

        mxGraphLayout layout = new mxCircleLayout(graph);
        layout.execute(graph.getDefaultParent());

        layout = new mxEdgeLabelLayout(graph);
        layout.execute(graph.getDefaultParent());

        return graphComponent;
    }

    @Override
    public void setCheckDeadlockControlEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SwingVmDeadLockView.this.checkForDeadlockButton.setEnabled(enabled);
            }
        });
    }

    private static void addDeadlockToGraph(Information info, mxGraph graph, Object parent, FontMetrics metrics) {
        graph.getModel().beginUpdate(); // batch updates
        try {
            Map<String, Object> idToCell = new HashMap<>();
            Map<String, String> idToLabel = new HashMap<>();

            for (DeadlockParser.Thread thread : info.threads) {
                String label = getThreadLabel(thread);
                String tooltip = getThreadTooltip(thread);
                idToLabel.put(thread.id, label);
                GraphItem node = new GraphItem(label, tooltip);
                final int PADDING = 20;
                int width = metrics.stringWidth(label) + PADDING;
                int height = metrics.getHeight() + PADDING;
                Object threadNode = graph.insertVertex(parent, thread.id, node, 0, 0, width, height);
                idToCell.put(thread.id, threadNode);
            }

            for (DeadlockParser.Thread thread : info.threads) {
                String label = translate.localize(LocaleResources.DEADLOCK_WAITING_ON).getContents();
                String tooltip = getEdgeTooltip(thread, idToLabel);
                GraphItem edge = new GraphItem(label, tooltip);
                graph.insertEdge(parent, thread.waitingOn.name, edge, idToCell.get(thread.id), idToCell.get(thread.waitingOn.ownerId));
            }

        }  finally {
           graph.getModel().endUpdate();
        }
    }

    private static String getThreadLabel(DeadlockParser.Thread thread) {
        return translate.localize(LocaleResources.DEADLOCK_THREAD_NAME, thread.name, thread.id).getContents();
    }

    private static String getThreadTooltip(DeadlockParser.Thread thread) {
        return translate.localize(
                LocaleResources.DEADLOCK_THREAD_TOOLTIP,
                StringUtils.htmlEscape(thread.waitingOn.name),
                stackTraceToHtmlString(thread.stackTrace))
            .getContents();
    }

    private static String stackTraceToHtmlString(List<String> items) {
        StringBuilder result = new StringBuilder();
        for (String item : items) {
            result.append(StringUtils.htmlEscape(item)).append("<br/>");
        }
        return result.toString();
    }

    private static String getEdgeTooltip(DeadlockParser.Thread thread, Map<String, String> idToLabel) {
        return translate.localize(
                LocaleResources.DEADLOCK_EDGE_TOOLTIP,
                idToLabel.get(thread.id),
                idToLabel.get(thread.waitingOn.ownerId))
            .getContents();
    }

    @Override
    public Component getUiComponent() {
        return actualComponent;
    }

    static class GraphItem implements Serializable {

        private final String label;
        private final String tooltip;

        public GraphItem(String label, String tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, tooltip);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GraphItem other = (GraphItem) obj;
            return Objects.equals(label, other.label) && Objects.equals(tooltip, other.tooltip);
        }

        @Override
        public String toString() {
            return label;
        }

        public String getTooltip() {
            return tooltip;
        }
    }

}
