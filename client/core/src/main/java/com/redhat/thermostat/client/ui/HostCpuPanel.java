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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.osgi.service.BasicView;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.DiscreteTimeData;

public class HostCpuPanel extends HostCpuView implements SwingComponent {

    private JPanel visiblePanel;
    
    private final JTextComponent cpuModel = new ValueField("${CPU_MODEL}");
    private final JTextComponent cpuCount = new ValueField("${CPU_COUNT}");

    private final TimeSeriesCollection datasetCollection = new TimeSeriesCollection();
    private final TimeSeries dataset = new TimeSeries("host-cpu");

    public HostCpuPanel() {
        super();
        datasetCollection.addSeries(dataset);
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
    public void addCpuLoadData(List<DiscreteTimeData<Double>> data) {
        final ArrayList<DiscreteTimeData<Double>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
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
    public void clearCpuLoadData() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dataset.clear();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {

        visiblePanel = new JPanel();
        
        JLabel summaryLabel = Components.header(localize(LocaleResources.HOST_CPU_SECTION_OVERVIEW));

        JLabel cpuModelLabel = Components.label(localize(LocaleResources.HOST_INFO_CPU_MODEL));

        JLabel cpuCountLabel = Components.label(localize(LocaleResources.HOST_INFO_CPU_COUNT));

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                localize(LocaleResources.HOST_CPU_USAGE_CHART_TIME_LABEL),
                localize(LocaleResources.HOST_CPU_USAGE_CHART_VALUE_LABEL),
                datasetCollection,
                false, false, false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        GroupLayout groupLayout = new GroupLayout(visiblePanel);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(chartPanel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 947, Short.MAX_VALUE)
                        .addComponent(summaryLabel, Alignment.LEADING)
                        .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(cpuCountLabel)
                                    .addGap(18)
                                    .addComponent(cpuCount, GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE))
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addComponent(cpuModelLabel)
                                    .addGap(18)
                                    .addComponent(cpuModel, GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)))))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(summaryLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cpuModelLabel)
                        .addComponent(cpuModel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(10)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cpuCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cpuCountLabel))
                    .addGap(18)
                    .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE))
        );
        visiblePanel.setLayout(groupLayout);
    }

    @Override
    public BasicView getView() {
        return this;
    }

}
