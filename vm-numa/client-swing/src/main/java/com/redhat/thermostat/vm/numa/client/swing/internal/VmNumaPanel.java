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

package com.redhat.thermostat.vm.numa.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.RangeType;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.experimental.SingleValueChartPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.numa.client.core.VmNumaView;
import com.redhat.thermostat.vm.numa.client.core.locale.LocaleResources;

public class VmNumaPanel extends VmNumaView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private HeaderPanel visiblePanel;

    private static final int DEFAULT_VALUE = 10;
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;

    private Duration duration;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<>(this);

    private final Map<String, TimeSeriesCollection> datasets = new HashMap<>();

    private SingleValueChartPanel chartPanel;

    public VmNumaPanel() {
        super();
        initializePanel();

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    private void initializePanel() {
        visiblePanel = new HeaderPanel();
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_NUMA_TITLE));

        JPanel p = initializeChartsPanel();
        visiblePanel.setContent(p);
    }

    private JPanel initializeChartsPanel() {
        duration = new Duration(DEFAULT_VALUE, DEFAULT_UNIT);
        chartPanel = new SingleValueChartPanel(duration);

        chartPanel.addPropertyChangeListener(SingleValueChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE);
            }
        });

        return chartPanel;
    }

    private JFreeChart initializeChart(String title, TimeSeriesCollection collection) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                translator.localize(LocaleResources.VM_NUMA_TIME_LABEL).getContents(),
                translator.localize(LocaleResources.VM_NUMA_MEMORY_LABEL).getContents(),
                collection,
                true, false, false);

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setLowerBound(0.0);
        rangeAxis.setRangeType(RangeType.POSITIVE);

        chart.getXYPlot().getRangeAxis().setAutoRange(true);

        chart.getXYPlot().getDomainAxis().setAutoRange(true);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        return chart;
    }

    @Override
    public void addChart(int numNumaNodes, String name) {
        TimeSeriesCollection collection = new TimeSeriesCollection();
        datasets.put(name, collection);

        for (int i = 0; i < numNumaNodes; i++) {
            TimeSeries series = new TimeSeries("Node: " + i);
            collection.addSeries(series);
        }
        JFreeChart chart = initializeChart(name, collection);
        chartPanel.addChart(chart);
    }

    @Override
    public void showNumaUnavailable() {
        JPanel statusPanel = new JPanel(new BorderLayout());

        String wrappedText = "<html>" + translator.localize(LocaleResources.VM_NUMA_UNAVAILABLE).getContents() + "</html>";
        JLabel descriptionLabel = new JLabel(wrappedText);
        statusPanel.add(descriptionLabel, BorderLayout.PAGE_START);

        visiblePanel.setContent(statusPanel);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
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
    public Duration getUserDesiredDuration() {
        return duration;
    }

    @Override
    public void setVisibleDataRange(int time, TimeUnit unit) {
        chartPanel.setTimeRangeToShow(time, unit);
    }

    @Override
    public void addData(final String seriesName, final int nodeNumber, final DiscreteTimeData<Double> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeriesCollection collection = datasets.get(seriesName);
                TimeSeries dataset = collection.getSeries("Node: " + nodeNumber);
                RegularTimePeriod period = new FixedMillisecond(data.getTimeInMillis());
                if (dataset.getDataItem(period) == null) {
                    dataset.add(period, data.getData(), false);
                }
                dataset.fireSeriesChanged();
            }
        });
    }
}
