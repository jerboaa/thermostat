/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.client.swing.internal;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.components.experimental.ThermostatChartPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.RangeType;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuView;
import com.redhat.thermostat.vm.cpu.client.locale.LocaleResources;

public class VmCpuPanel extends VmCpuView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private Duration duration;

    private HeaderPanel visiblePanel;
    
    private final TimeSeriesCollection data = new TimeSeriesCollection();
    private final TimeSeries cpuTimeSeries = new TimeSeries("cpu-stats");

    private ThermostatChartPanel chartPanel;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<VmCpuView.UserAction>(this);

    public VmCpuPanel() {
        super();
        data.addSeries(cpuTimeSeries);

        duration = ThermostatChartPanel.DEFAULT_DATA_DISPLAY;

        initializePanel();

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
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
                translator.localize(LocaleResources.VM_CPU_CHART_TIME_LABEL).getContents(),
                translator.localize(LocaleResources.VM_CPU_CHART_LOAD_LABEL).getContents(),
                data,
                false, false, false);

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setLowerBound(0.0);
        rangeAxis.setRangeType(RangeType.POSITIVE);

        chartPanel = new ThermostatChartPanel(chart, duration);

        visiblePanel.setContent(chartPanel);

        chartPanel.addPropertyChangeListener(ThermostatChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE);
            }
        });
    }

    @Override
    public void addUserActionListener(ActionListener<UserAction> listener) {
        userActionNotifier.addActionListener(listener);
    }

    @Override
    public void removeUserActionListener(ActionListener<UserAction> listener) {
        userActionNotifier.removeActionListener(listener);
    }

    @Override
    public void setAvailableDataRange(Range<Long> availableInterval) {
        // FIXME indicate the total data range to the user somehow
    }

    @Override
    public void setVisibleDataRange(Duration duration) {
        chartPanel.setTimeRangeToShow(duration);
    }

    @Override
    public Duration getUserDesiredDuration() {
        return duration;
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

