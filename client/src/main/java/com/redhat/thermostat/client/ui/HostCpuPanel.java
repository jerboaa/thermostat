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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.ChangeableText;
import com.redhat.thermostat.client.DiscreteTimeData;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;

public class HostCpuPanel extends JPanel implements HostCpuView {

    private static final long serialVersionUID = -1840585935194027332L;

    private final ChangeableText cpuModel = new ChangeableText("");
    private final ChangeableText cpuCount = new ChangeableText("");

    private final TimeSeriesCollection datasetCollection = new TimeSeriesCollection();
    private final TimeSeries dataset = new TimeSeries("host-cpu");

    public HostCpuPanel() {
        datasetCollection.addSeries(dataset);
        initializePanel();
    }

    @Override
    public void setCpuCount(String count) {
        cpuCount.setText(count);
    }

    @Override
    public void setCpuModel(String model) {
        cpuModel.setText(model);
    }

    @Override
    public void addCpuLoadData(List<DiscreteTimeData<Double>> data) {
        final ArrayList<DiscreteTimeData<Double>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (DiscreteTimeData<Double> timeData: copy) {
                    dataset.add(new FixedMillisecond(timeData.getTimeInMillis()), timeData.getData(), false);
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
        return this;
    }

    private void initializePanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;

        List<Section> allSections = new ArrayList<Section>();

        Section cpuBasics = new Section(localize(LocaleResources.HOST_CPU_SECTION_OVERVIEW));
        allSections.add(cpuBasics);

        TableEntry entry;
        entry = new TableEntry(localize(LocaleResources.HOST_INFO_CPU_MODEL), cpuModel);
        cpuBasics.add(entry);
        entry = new TableEntry(localize(LocaleResources.HOST_INFO_CPU_COUNT), cpuCount);
        cpuBasics.add(entry);

        final SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        add(table, c);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                localize(LocaleResources.HOST_CPU_USAGE_CHART_TIME_LABEL),
                localize(LocaleResources.HOST_CPU_USAGE_CHART_VALUE_LABEL),
                datasetCollection,
                false, false, false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        add(chartPanel, c);
    }

}
