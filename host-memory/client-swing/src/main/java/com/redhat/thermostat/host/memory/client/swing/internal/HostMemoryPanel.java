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

package com.redhat.thermostat.host.memory.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.MultiChartPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView;
import com.redhat.thermostat.host.memory.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class HostMemoryPanel extends HostMemoryView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;

    private final JTextComponent totalMemory = new ValueField("");

    private MultiChartPanel multiChartPanel;

    private final DataGroup DEFAULT_GROUP;

    public HostMemoryPanel() {
        multiChartPanel = new MultiChartPanel();
        DEFAULT_GROUP = multiChartPanel.createGroup();
        initializePanel();

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    @Override
    public void setTotalMemory(final String newValue) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                totalMemory.setText(newValue);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public void addMemoryChart(final String tag, final LocalizedString name) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.addChart(DEFAULT_GROUP, tag, name);
            }
        });
    }

    @Override
    public void removeMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.removeChart(DEFAULT_GROUP, tag);
            }
        });
    }

    @Override
    public void showMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.showChart(DEFAULT_GROUP, tag);
            }
        });
    }

    @Override
    public void hideMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.hideChart(DEFAULT_GROUP, tag);
            }
        });
    }

    @Override
    public void addMemoryData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.addData(tag, copy);
            }
        });
    }

    @Override
    public void clearMemoryData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.clearData(tag);
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

    private void initializePanel() {
        visiblePanel = new JPanel();
        visiblePanel.setOpaque(false);

        String xAxisLabel = translator.localize(LocaleResources.HOST_MEMORY_CHART_TIME_LABEL).getContents();
        String yAxisLabel = translator.localize(LocaleResources.HOST_MEMORY_CHART_SIZE_LABEL, Size.Unit.MiB.name()).getContents();
        multiChartPanel.setDomainAxisLabel(xAxisLabel);
        NumberAxis axis = multiChartPanel.getRangeAxis(DEFAULT_GROUP);
        axis.setLabel(yAxisLabel);

        JLabel lblMemory = new SectionHeader(translator.localize(LocaleResources.HOST_MEMORY_SECTION_OVERVIEW));

        JLabel totalMemoryLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));

        JPanel mainPanel = new JPanel();

        JPanel totalMemoryPanel = new JPanel();
        BorderLayout totalMemoryBorderLayout = new BorderLayout();
        totalMemoryBorderLayout.setHgap(15);
        totalMemoryPanel.setLayout(totalMemoryBorderLayout);
        totalMemoryPanel.add(totalMemoryLabel, BorderLayout.WEST);
        totalMemoryPanel.add(totalMemory, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        BorderLayout northPanelBorderLayout = new BorderLayout();
        northPanelBorderLayout.setVgap(5);
        northPanelBorderLayout.setHgap(10);

        northPanel.setLayout(northPanelBorderLayout);

        northPanel.add(lblMemory, BorderLayout.NORTH);
        northPanel.add(Box.createGlue(), BorderLayout.WEST);
        northPanel.add(totalMemoryPanel, BorderLayout.CENTER);

        BorderLayout mainPanelBorderLayout = new BorderLayout();
        mainPanelBorderLayout.setHgap(5);
        mainPanelBorderLayout.setVgap(10);
        mainPanel.setLayout(mainPanelBorderLayout);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(Box.createGlue(), BorderLayout.WEST);
        mainPanel.add(multiChartPanel, BorderLayout.CENTER);

        BorderLayout visiblePanelBorderLayout = new BorderLayout();
        visiblePanelBorderLayout.setVgap(10);
        visiblePanel.setLayout(visiblePanelBorderLayout);
        visiblePanel.add(Box.createGlue(), BorderLayout.NORTH);
        visiblePanel.add(mainPanel, BorderLayout.CENTER);
        visiblePanel.add(Box.createGlue(), BorderLayout.SOUTH);
    }

    @Override
    public Duration getUserDesiredDuration() {
        return multiChartPanel.getUserDesiredDuration();
    }
}

