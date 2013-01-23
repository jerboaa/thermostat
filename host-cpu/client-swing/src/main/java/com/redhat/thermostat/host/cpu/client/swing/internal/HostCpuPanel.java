/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.host.cpu.client.core.HostCpuView;
import com.redhat.thermostat.host.cpu.client.locale.LocaleResources;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.swing.components.experimental.WrapLayout;

public class HostCpuPanel extends HostCpuView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;

    private final JTextComponent cpuModel = new ValueField("${CPU_MODEL}");
    private final JTextComponent cpuCount = new ValueField("${CPU_COUNT}");

    private final TimeSeriesCollection datasetCollection = new TimeSeriesCollection();
    private final Map<Integer, TimeSeries> datasets = new HashMap<>();
    private final Map<String, Color> colors = new HashMap<>();
    private final Map<String, JLabel> labels = new HashMap<>();

    private JFreeChart chart;

    private JPanel legendPanel;

    public HostCpuPanel() {
        super();
        initializePanel();

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
    public void addCpuUsageChart(final int cpuIndex, final String humanReadableName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = new TimeSeries(humanReadableName);
                Color color = ChartColors.getColor(colors.size());
                colors.put(humanReadableName, color);

                datasets.put(cpuIndex, series);
                datasetCollection.addSeries(series);

                updateColors();

                JLabel label = createLabelWithLegend(humanReadableName, color);
                labels.put(humanReadableName, label);

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
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_TIME_LABEL),
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_VALUE_LABEL),
                datasetCollection,
                false, false, false);

        chart.getPlot().setBackgroundPaint( new Color(255,255,255,0) );
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));
        chartPanel.setOpaque(false);

        legendPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
        legendPanel.setOpaque(false);

        GroupLayout groupLayout = new GroupLayout(visiblePanel);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(legendPanel, GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
                        .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
                        .addComponent(summaryLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(cpuCountLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addGap(18)
                                    .addComponent(cpuCount, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addComponent(cpuModelLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addGap(18)
                                    .addComponent(cpuModel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGap(11))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(summaryLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cpuModelLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cpuModel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(10)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cpuCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cpuCountLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(legendPanel, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        visiblePanel.setLayout(groupLayout);
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

    private JLabel createLabelWithLegend(String text, Color color) {
        String hexColor = "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
        return new JLabel("<html> <font color='" + hexColor + "'>\u2588</font> " + text + "</html>");
    }
}

