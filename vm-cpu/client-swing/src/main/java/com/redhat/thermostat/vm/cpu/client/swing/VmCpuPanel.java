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

package com.redhat.thermostat.vm.cpu.client.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuView;
import com.redhat.thermostat.vm.cpu.client.locale.LocaleResources;

public class VmCpuPanel extends VmCpuView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private HeaderPanel visiblePanel;
    
    private final TimeSeriesCollection data = new TimeSeriesCollection();
    private final TimeSeries cpuTimeSeries = new TimeSeries("cpu-stats");

    public VmCpuPanel() {
        super();
        data.addSeries(cpuTimeSeries);

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
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new HeaderPanel();
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_CPU_TITLE));

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                translator.localize(LocaleResources.VM_CPU_CHART_TIME_LABEL),
                translator.localize(LocaleResources.VM_CPU_CHART_LOAD_LABEL),
                data,
                false, false, false);

        chart.getXYPlot().getRangeAxis().setLowerBound(0.0);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        visiblePanel.setContent(chartPanel);
    }

    @Override
    public void addData(List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (DiscreteTimeData<? extends Number> data: copy) {
                    RegularTimePeriod period = new FixedMillisecond(data.getTimeInMillis());
                    if (cpuTimeSeries.getDataItem(period) == null) {
                        cpuTimeSeries.add(period, data.getData(), false);
                    }
                }
                cpuTimeSeries.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearData() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cpuTimeSeries.clear();
            }
        });
    }
}
