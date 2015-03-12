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

package com.redhat.thermostat.host.cpu.client.swing.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.Box;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.LegendLabel;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.host.cpu.client.core.HostCpuView;
import com.redhat.thermostat.host.cpu.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.swing.components.experimental.WrapLayout;

public class HostCpuPanel extends HostCpuView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;

    private final JTextComponent cpuModel = new ValueField("");
    private final JTextComponent cpuCount = new ValueField("");

    private final TimeSeriesCollection datasetCollection = new TimeSeriesCollection();
    private final Map<Integer, TimeSeries> datasets = new HashMap<>();
    private final Map<String, Color> colors = new HashMap<>();
    private final Map<String, JLabel> labels = new HashMap<>();

    private RecentTimeSeriesChartController chartController;

    private JFreeChart chart;

    private JPanel legendPanel;

    public HostCpuPanel() {
        super();
        initializePanel();

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
    public void setCpuCount(final String count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cpuCount.setText(count);
            }
        });
    }

    @Override
    public void setCpuModel(final String model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cpuModel.setText(model);
            }
        });
    }

    @Override
    public void addCpuUsageChart(final int cpuIndex, final LocalizedString name) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String theName = name.getContents();
                TimeSeries series = new TimeSeries(theName);
                Color color = ChartColors.getColor(colors.size());
                colors.put(theName, color);

                datasets.put(cpuIndex, series);
                datasetCollection.addSeries(series);

                updateColors();

                JLabel label = new LegendLabel(name, color);
                labels.put(theName, label);

                legendPanel.add(label);
                legendPanel.revalidate();
            }
        });
    }

    @Override
    public void addCpuUsageData(final int cpuIndex, List<DiscreteTimeData<Double>> data) {
        final ArrayList<DiscreteTimeData<Double>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries dataset = datasets.get(cpuIndex);
                for (DiscreteTimeData<Double> timeData: copy) {
                    RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
                    if (dataset.getDataItem(period) == null) {
                        dataset.add(period, timeData.getData(), false);
                    }
                }
                dataset.fireSeriesChanged();
            }
        });
    }

    @Override
    public Duration getUserDesiredDuration() {
        if (chartController == null) {
            return new Duration(10, TimeUnit.MINUTES);
        }
        return new Duration(chartController.getTimeValue(), chartController.getTimeUnit());
    }

    @Override
    public void clearCpuUsageData() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Iterator<Map.Entry<Integer, TimeSeries>> iter = datasets.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry<Integer, TimeSeries> entry = iter.next();
                    datasetCollection.removeSeries(entry.getValue());
                    entry.getValue().clear();

                    iter.remove();

                }
                updateColors();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {

        visiblePanel = new JPanel();

        JLabel summaryLabel = new SectionHeader(translator.localize(LocaleResources.HOST_CPU_SECTION_OVERVIEW));

        JLabel cpuModelLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_CPU_MODEL));

        JLabel cpuCountLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_CPU_COUNT));

        chart = ChartFactory.createTimeSeriesChart(
                null,
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_TIME_LABEL).getContents(),
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_VALUE_LABEL).getContents(),
                datasetCollection,
                false, false, false);

        chart.getPlot().setBackgroundPaint( new Color(255,255,255,0) );
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        chartController = new RecentTimeSeriesChartController(chart);
        JPanel chartPanel = new RecentTimeSeriesChartPanel(chartController);
        chartPanel.setOpaque(false);

        legendPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
        legendPanel.setOpaque(false);

        JPanel mainPanel = new JPanel();

        BorderLayout northPanelBorderLayout = new BorderLayout();
        northPanelBorderLayout.setVgap(5);
        northPanelBorderLayout.setHgap(10);
        JPanel northPanel = new JPanel();
        northPanel.setLayout(northPanelBorderLayout);

        BorderLayout cpuCommonBorderLayout = new BorderLayout();
        cpuCommonBorderLayout.setVgap(5);
        JPanel cpuCommonPanel = new JPanel();
        cpuCommonPanel.setLayout(cpuCommonBorderLayout);

        BorderLayout cpuModelBorderLayout = new BorderLayout();
        cpuModelBorderLayout.setHgap(25);

        JPanel cpuModelPanel = new JPanel();
        cpuModelPanel.setLayout(cpuModelBorderLayout);
        cpuModelPanel.add(cpuModelLabel, BorderLayout.WEST);
        cpuModelPanel.add(cpuModel, BorderLayout.CENTER);

        BorderLayout cpuCountBorderLayout = new BorderLayout();
        cpuCountBorderLayout.setHgap(25);

        JPanel cpuCountPanel = new JPanel();
        cpuCountPanel.setLayout(cpuCountBorderLayout);
        cpuCountPanel.add(cpuCountLabel, BorderLayout.WEST);
        cpuCountPanel.add(cpuCount, BorderLayout.CENTER);

        cpuCommonPanel.add(cpuModelPanel, BorderLayout.NORTH);
        cpuCommonPanel.add(cpuCountPanel, BorderLayout.SOUTH);

        northPanel.add(summaryLabel, BorderLayout.NORTH);
        northPanel.add(Box.createGlue(), BorderLayout.WEST);
        northPanel.add(cpuCommonPanel, BorderLayout.CENTER);

        BorderLayout mainPanelBorderLayout = new BorderLayout();
        mainPanelBorderLayout.setHgap(5);
        mainPanelBorderLayout.setVgap(10);
        mainPanel.setLayout(mainPanelBorderLayout);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(Box.createGlue(), BorderLayout.WEST);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(Box.createGlue(), BorderLayout.EAST);
        mainPanel.add(legendPanel, BorderLayout.SOUTH);

        BorderLayout visiblePanelBorderLayout = new BorderLayout();
        visiblePanelBorderLayout.setVgap(10);
        visiblePanel.setLayout(visiblePanelBorderLayout);
        visiblePanel.add(Box.createGlue(), BorderLayout.NORTH);
        visiblePanel.add(mainPanel, BorderLayout.CENTER);
        visiblePanel.add(Box.createGlue(), BorderLayout.SOUTH);
    }

    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYItemRenderer itemRenderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < datasetCollection.getSeriesCount(); i++) {
            String tag = (String) datasetCollection.getSeriesKey(i);
            Color color = colors.get(tag);
            itemRenderer.setSeriesPaint(i, color);
        }
    }

}

