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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.DiscreteTimeData;

public class VmGcPanel extends JPanel implements VmGcView {

    private static final long serialVersionUID = -4924051863887499866L;

    private final ActionNotifier<Action> notifier = new ActionNotifier<>(this);

    private final Map<String, TimeSeriesCollection> dataset = new HashMap<>();
    private final Map<String, JPanel> subPanels = new HashMap<>();

    private final GridBagConstraints gcPanelConstraints;

    public VmGcPanel() {
        initializePanel();

        gcPanelConstraints = new GridBagConstraints();
        gcPanelConstraints.gridx = 0;
        gcPanelConstraints.gridy = 0;
        gcPanelConstraints.fill = GridBagConstraints.BOTH;
        gcPanelConstraints.weightx = 1;
        gcPanelConstraints.weighty = 1;

        addHierarchyListener(new ComponentVisibleListener() {
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
        return this;
    }

    private void initializePanel() {
        setLayout(new GridBagLayout());
    }

    private JPanel createCollectorDetailsPanel(TimeSeriesCollection timeSeriesCollection, String title) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(Components.smallBorder());
        detailsPanel.setLayout(new BorderLayout());

        detailsPanel.add(Components.header(title), BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                localize(LocaleResources.VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL),
                localize(LocaleResources.VM_GC_COLLECTOR_CHART_GC_TIME_LABEL),
                timeSeriesCollection,
                false, false, false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

    @Override
    public void addChart(final String tag, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries timeSeries = new TimeSeries(tag);
                TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(timeSeries);
                dataset.put(tag, timeSeriesCollection);
                JPanel subPanel = createCollectorDetailsPanel(timeSeriesCollection, title);
                subPanels.put(tag, subPanel);
                add(subPanel, gcPanelConstraints);
                gcPanelConstraints.gridy++;
                revalidate();
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
                remove(subPanel);
                revalidate();
                gcPanelConstraints.gridy--;
            }
        });
    }

    @Override
    public void addData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag).getSeries(tag);
                for (DiscreteTimeData<? extends Number> timeData: copy) {
                    series.add(new FixedMillisecond(timeData.getTimeInMillis()), timeData.getData(), false);
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
                TimeSeries series = dataset.get(tag).getSeries(tag);
                series.clear();
            }
        });
    }
}
