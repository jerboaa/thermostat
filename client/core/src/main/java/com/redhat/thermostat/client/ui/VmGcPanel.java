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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.xy.IntervalXYDataset;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.osgi.service.BasicView;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.IntervalTimeData;

public class VmGcPanel extends VmGcView implements SwingComponent {
    
    private JPanel visiblePanel;

    private final Map<String, SampledDataset> dataset = new HashMap<>();
    private final Map<String, JPanel> subPanels = new HashMap<>();

    private final GridBagConstraints gcPanelConstraints;

    public VmGcPanel() {
        super();
        initializePanel();

        gcPanelConstraints = new GridBagConstraints();
        gcPanelConstraints.gridx = 0;
        gcPanelConstraints.gridy = 0;
        gcPanelConstraints.fill = GridBagConstraints.BOTH;
        gcPanelConstraints.weightx = 1;
        gcPanelConstraints.weighty = 1;

        visiblePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new JPanel();
        visiblePanel.setLayout(new GridBagLayout());
    }

    private JPanel createCollectorDetailsPanel(IntervalXYDataset timeSeriesCollection, String title, String units) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(Components.smallBorder());
        detailsPanel.setLayout(new BorderLayout());

        detailsPanel.add(Components.header(title), BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createHistogram(
            null,
            localize(LocaleResources.VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL),
            localize(LocaleResources.VM_GC_COLLECTOR_CHART_GC_TIME_LABEL, units),
            timeSeriesCollection,
            PlotOrientation.VERTICAL,
            false,
            false,
            false);

        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setBarPainter(new StandardXYBarPainter());
        chart.getXYPlot().setDomainAxis(new DateAxis());
        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();

        axis.setRangeType(RangeType.POSITIVE);
        axis.setAutoRange(true);
        axis.setAutoRangeMinimumSize(1);

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

    @Override
    public void addChart(final String tag, final String title, final String units) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset newData = new SampledDataset();
                dataset.put(tag, newData);
                JPanel subPanel = createCollectorDetailsPanel(newData, title, units);
                subPanels.put(tag, subPanel);
                visiblePanel.add(subPanel, gcPanelConstraints);
                gcPanelConstraints.gridy++;
                visiblePanel.revalidate();
            }
        });
    }

    @Override
    public void removeChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dataset.remove(tag);
                JPanel subPanel = subPanels.remove(tag);
                visiblePanel.remove(subPanel);
                visiblePanel.revalidate();
                gcPanelConstraints.gridy--;
            }
        });
    }

    @Override
    public void addData(final String tag, List<IntervalTimeData<Double>> data) {
        final List<IntervalTimeData<Double>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                for (IntervalTimeData<Double> timeData: copy) {
                    series.add(timeData.getStartTimeInMillis(), timeData.getEndTimeInMillis(), timeData.getData());
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                series.clear();
            }
        });
    }

    @Override
    public BasicView getView() {
        return this;
    }
}
