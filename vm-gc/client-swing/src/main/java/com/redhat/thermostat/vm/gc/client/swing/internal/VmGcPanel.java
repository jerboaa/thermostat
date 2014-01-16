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

package com.redhat.thermostat.vm.gc.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.xy.IntervalXYDataset;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.client.ui.SampledDataset;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;

public class VmGcPanel extends VmGcView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private HeaderPanel visiblePanel = new HeaderPanel();
    private JPanel realPanel = new JPanel();

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
        visiblePanel.setContent(realPanel);
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_GC_TITLE));
        realPanel.setLayout(new GridBagLayout());
    }

    private JPanel createCollectorDetailsPanel(IntervalXYDataset collectorData, LocalizedString title, String units) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        detailsPanel.setLayout(new BorderLayout());

        detailsPanel.add(new SectionHeader(title), BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createHistogram(
            null,
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL).getContents(),
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_GC_TIME_LABEL, units).getContents(),
            collectorData,
            PlotOrientation.VERTICAL,
            false,
            false,
            false);

        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setBarPainter(new StandardXYBarPainter());

        setupPlotAxes(chart.getXYPlot());

        chart.getXYPlot().setDomainCrosshairLockedOnData(true);
        chart.getXYPlot().setDomainCrosshairVisible(true);

        final RecentTimeSeriesChartPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        chart.addProgressListener(new ChartProgressListener() {

            @Override
            public void chartProgress(ChartProgressEvent event) {
                if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
                    return;
                }

                double rangeCrossHairValue = event.getChart().getXYPlot().getRangeCrosshairValue();
                chartPanel.setDataInformationLabel(String.valueOf(rangeCrossHairValue));
            }
        });

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

    private void setupPlotAxes(XYPlot plot) {
        setupDomainAxis(plot);
        setupRangeAxis(plot);
    }

    private void setupDomainAxis(XYPlot plot) {
        plot.setDomainAxis(new DateAxis());
    }

    private void setupRangeAxis(XYPlot plot) {
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        rangeAxis.setRangeType(RangeType.POSITIVE);
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(1);
    }

    @Override
    public void addChart(final String tag, final LocalizedString title, final String units) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset newData = new SampledDataset();
                dataset.put(tag, newData);
                JPanel subPanel = createCollectorDetailsPanel(newData, title, units);
                subPanels.put(tag, subPanel);
                realPanel.add(subPanel, gcPanelConstraints);
                gcPanelConstraints.gridy++;
                realPanel.revalidate();
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
                realPanel.remove(subPanel);
                realPanel.revalidate();
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
}

