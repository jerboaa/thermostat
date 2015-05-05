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

package com.redhat.thermostat.vm.io.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.LegendLabel;
import com.redhat.thermostat.client.swing.components.experimental.SingleValueChartPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.io.client.core.LocaleResources;
import com.redhat.thermostat.vm.io.client.core.VmIoView;
import com.redhat.thermostat.vm.io.common.VmIoStat;

public class VmIoPanel extends VmIoView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int DEFAULT_DURATION_VALUE = 10;
    private static final TimeUnit DEFAULT_DURATION_UNIT = TimeUnit.MINUTES;

    private static final int INDEX_CHARACTERS_READ = 0;
    private static final int INDEX_CHARACTERS_WRITTEN = 1;
    private static final int INDEX_READ_SYSCALLS = 2;
    private static final int INDEX_WRITE_SYSCALLS = 3;
    private static final int INDEX_LAST = 4;

    private Duration duration;

    private HeaderPanel visiblePanel;

    private final TimeSeriesCollection vmIoData = new TimeSeriesCollection();
    private final TimeSeries[] datasets = new TimeSeries[INDEX_LAST];

    private SingleValueChartPanel chartPanel;
    private JPanel legendPanel;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<UserAction>(this);

    public VmIoPanel() {
        super();

        duration = new Duration(DEFAULT_DURATION_VALUE, DEFAULT_DURATION_UNIT);

        initializePanel();
        initializeDataAndComponents();

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new HeaderPanel();
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_IO_TITLE));

        JPanel mainPanel = new JPanel(new BorderLayout());

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                translator.localize(LocaleResources.VM_IO_CHART_TIME_LABEL).getContents(),
                null,
                vmIoData,
                false, false, false);

        chart.getXYPlot().getRangeAxis().setLowerBound(0.0);

        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < INDEX_LAST; i++) {
            renderer.setSeriesPaint(i, ChartColors.getColor(i));
        }

        chartPanel = new SingleValueChartPanel(chart, duration);
        mainPanel.add(chartPanel, BorderLayout.CENTER);

        visiblePanel.setContent(mainPanel);

        chartPanel.addPropertyChangeListener(SingleValueChartPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE);
            }
        });

        legendPanel = new JPanel();
        mainPanel.add(legendPanel, BorderLayout.PAGE_END);
    }

    private void initializeDataAndComponents() {
        LocalizedString[] names = new LocalizedString[INDEX_LAST];
        names[INDEX_CHARACTERS_READ] = translator.localize(LocaleResources.VM_IO_CHART_CHARACTERS_READ_LABEL);
        names[INDEX_CHARACTERS_WRITTEN] = translator.localize(LocaleResources.VM_IO_CHART_CHARACTERS_WRITTEN_LABEL);
        names[INDEX_READ_SYSCALLS] = translator.localize(LocaleResources.VM_IO_CHART_READ_SYSCALLS_LABEL);
        names[INDEX_WRITE_SYSCALLS] = translator.localize(LocaleResources.VM_IO_CHART_WRITE_SYSCALLS_LABEL);

        for (int i = 0; i < INDEX_LAST; i++) {
            LocalizedString localizedName = names[i];
            String theName = localizedName.getContents();
            TimeSeries series = new TimeSeries(theName);
            Color color = ChartColors.getColor(i);

            datasets[i] = series;
            vmIoData.addSeries(series);

            JLabel label = new LegendLabel(localizedName, color);

            legendPanel.add(label);
            legendPanel.revalidate();
            legendPanel.repaint();
        }
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
    public void setVisibleDataRange(int time, TimeUnit unit) {
        chartPanel.setTimeRangeToShow(time, unit);
    }

    @Override
    public Duration getUserDesiredDuration() {
        return duration;
    }

    @Override
    public void addData(final List<VmIoStat> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (VmIoStat stat: data) {
                    RegularTimePeriod period = new FixedMillisecond(stat.getTimeStamp());

                    datasets[INDEX_CHARACTERS_READ].add(period, stat.getCharactersRead(), false);
                    datasets[INDEX_CHARACTERS_WRITTEN].add(period, stat.getCharactersWritten(), false);
                    datasets[INDEX_READ_SYSCALLS].add(period, stat.getReadSyscalls(), false);
                    datasets[INDEX_WRITE_SYSCALLS].add(period, stat.getWriteSyscalls(), false);
                }

                for (int i = 0; i < INDEX_LAST; i++) {
                    datasets[i].fireSeriesChanged();
                }
            }
        });
    }

    @Override
    public void clearData() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < INDEX_LAST; i++) {
                    datasets[i].clear();
                }
            }
        });
    }
}

