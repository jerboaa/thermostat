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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
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

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.client.ui.SampledDataset;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;

public class VmGcPanel extends VmGcView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String GC_ALGO_LABEL_NAME = translator.localize(LocaleResources.VM_GC_CONFIGURED_COLLECTOR).getContents();

    private static final Color WHITE = new Color(255,255,255,0);
    private static final Color BLACK = new Color(0,0,0,0);
    private static final float TRANSPARENT = 0.0f;
    
    private HeaderPanel visiblePanel = new HeaderPanel();
    private JPanel chartPanelContainer = new JPanel();
    private JPanel containerPanel = new JPanel();
    private JLabel gcAlgoLabelDescr;
    private JLabel commonNameLabel;

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

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
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
        visiblePanel.setContent(containerPanel);
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_GC_TITLE));
        containerPanel.setLayout(new GridBagLayout());
        GridBagConstraints commonNameConstraints = getWeightedGridBagConstraint(0.03);
        commonNameConstraints.gridy = 0;
        GridBagConstraints chartPanelConstraints = getWeightedGridBagConstraint(0.97);
        chartPanelConstraints.gridy = 1;
        chartPanelContainer.setLayout(new GridBagLayout());
        JPanel commonNamePanel = createCollectorsCommonPanel();
        containerPanel.add(commonNamePanel, commonNameConstraints);
        containerPanel.add(chartPanelContainer, chartPanelConstraints);
    }
    
    private GridBagConstraints getWeightedGridBagConstraint(double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = weightY;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }
    
    private JPanel createCollectorsCommonPanel() {
        JPanel commonPanel = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEFT);
        layout.setHgap(3);
        commonPanel.setLayout(layout);
        // Common name for a GC algo might not be shown at all. Thus
        // fill with empty labels first and set a visible text only once
        // we know there is a known mapping.
        this.gcAlgoLabelDescr = new JLabel(""); // intentionally empty string
        this.commonNameLabel = new JLabel(""); // intentionally empty string
        commonPanel.add(gcAlgoLabelDescr);
        commonPanel.add(commonNameLabel);
        return commonPanel;
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

        chart.getPlot().setBackgroundPaint(WHITE);
        chart.getPlot().setBackgroundImageAlpha(TRANSPARENT);
        chart.getPlot().setOutlinePaint(BLACK);

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
                chartPanelContainer.add(subPanel, gcPanelConstraints);
                gcPanelConstraints.gridy++;
                containerPanel.revalidate();
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
                chartPanelContainer.remove(subPanel);
                gcPanelConstraints.gridy--;
                containerPanel.revalidate();
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
    public void setCommonCollectorName(final CollectorCommonName commonName) {
        // only set values if we are able to show more info about the in-use
        // GC-algo.
        if (commonName != CollectorCommonName.UNKNOWN_COLLECTOR) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gcAlgoLabelDescr.setText(GC_ALGO_LABEL_NAME);
                    commonNameLabel.setText(commonName.getHumanReadableString());
                }
            });
        }
    }
}

