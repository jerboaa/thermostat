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

package com.redhat.thermostat.vm.classstat.client.swing;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.components.experimental.SingleValueChartPanel;
import com.redhat.thermostat.common.model.Range;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
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
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.classstat.client.core.VmClassStatView;
import com.redhat.thermostat.vm.classstat.client.locale.LocaleResources;

public class VmClassStatPanel extends VmClassStatView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static final int DEFAULT_VALUE = 10;
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;

    private Duration duration;

    private HeaderPanel visiblePanel;
    
    private final TimeSeriesCollection dataset = new TimeSeriesCollection();

    private SingleValueChartPanel chartPanel;

    private ActionNotifier<UserAction> userActionActionNotifier = new ActionNotifier<VmClassStatView.UserAction>(this);

    private final ActionNotifier<Action> notifier = new ActionNotifier<Action>(this);

    public VmClassStatPanel() {
        visiblePanel = new HeaderPanel();
        // any name works
        dataset.addSeries(new TimeSeries("class-stat"));

        duration = new Duration(DEFAULT_VALUE, DEFAULT_UNIT);

        visiblePanel.setHeader(t.localize(LocaleResources.VM_LOADED_CLASSES));

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                t.localize(LocaleResources.VM_CLASSES_CHART_REAL_TIME_LABEL).getContents(),
                t.localize(LocaleResources.VM_CLASSES_CHART_LOADED_CLASSES_LABEL).getContents(),
                dataset,
                false, false, false);

        TickUnits tickUnits = new TickUnits();
        tickUnits.add(new NumberTickUnit(1));
        tickUnits.add(new NumberTickUnit(10));
        tickUnits.add(new NumberTickUnit(100));
        tickUnits.add(new NumberTickUnit(1000));
        tickUnits.add(new NumberTickUnit(10000));
        tickUnits.add(new NumberTickUnit(100000));
        tickUnits.add(new NumberTickUnit(1000000));

        NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        axis.setStandardTickUnits(tickUnits);
        axis.setRangeType(RangeType.POSITIVE);
        axis.setAutoRangeMinimumSize(10);

        chartPanel = new SingleValueChartPanel(chart, duration);

        visiblePanel.setContent(chartPanel);

        chartPanel.addPropertyChangeListener(SingleValueChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                userActionActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE);
            }
        });

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void addClassCount(List<DiscreteTimeData<Long>> data) {
        final List<DiscreteTimeData<Long>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.getSeries(0);
                for (DiscreteTimeData<Long> data: copy) {
                    RegularTimePeriod period = new FixedMillisecond(data.getTimeInMillis());
                    if (series.getDataItem(period) == null) {
                        series.add(period, data.getData(), false);
                    }
                }
                series.fireSeriesChanged();
            }
        });

    }

    @Override
    public void addUserActionListener(final ActionListener<UserAction> listener) {
        userActionActionNotifier.addActionListener(listener);
    }

    @Override
    public void removeUserActionListener(final ActionListener<UserAction> listener) {
        userActionActionNotifier.removeActionListener(listener);
    }

    @Override
    public Duration getUserDesiredDuration() {
        return duration;
    }

    @Override
    public void setVisibleDataRange(final int time, final TimeUnit unit) {
        chartPanel.setTimeRangeToShow(time, unit);
    }

    @Override
    public void setAvailableDataRange(final Range<Long> availableDataRange) {
        // FIXME indicate the total data range to the user somehow
    }

    @Override
    public void clearClassCount() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.getSeries(0);
                series.clear();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }
}

