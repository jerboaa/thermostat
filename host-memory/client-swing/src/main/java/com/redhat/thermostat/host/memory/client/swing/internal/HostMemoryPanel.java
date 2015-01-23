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

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.Box;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.RecentTimeSeriesChartPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView;
import com.redhat.thermostat.host.memory.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.swing.components.experimental.WrapLayout;

public class HostMemoryPanel extends HostMemoryView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;

    private final MemoryCheckboxListener memoryCheckboxListener = new MemoryCheckboxListener();

    private final JTextComponent totalMemory = new ValueField("");

    private final JPanel memoryCheckBoxPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    private final CopyOnWriteArrayList<GraphVisibilityChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final TimeSeriesCollection memoryCollection = new TimeSeriesCollection();
    private final Map<String, TimeSeries> dataset = new HashMap<>();
    private final Map<String, JCheckBox> checkBoxes = new HashMap<>();
    private final Map<String, Color> colors = new HashMap<>();

    private JFreeChart chart;
    private RecentTimeSeriesChartController chartController;

    public HostMemoryPanel() {
        super();
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
                int colorIndex = colors.size();
                colors.put(tag, ChartColors.getColor(colorIndex));
                TimeSeries series = new TimeSeries(tag);
                dataset.put(tag, series);
                JCheckBox newCheckBox = new JCheckBox(createLabelWithLegend(name, colors.get(tag)));
                newCheckBox.setActionCommand(tag);
                newCheckBox.setSelected(true);
                newCheckBox.addActionListener(memoryCheckboxListener);
                newCheckBox.setOpaque(false);
                checkBoxes.put(tag, newCheckBox);
                memoryCheckBoxPanel.add(newCheckBox);

                updateColors();
            }
        });

    }

    private String createLabelWithLegend(LocalizedString text, Color color) {
        String hexColor = "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
        return "<html> <font color='" + hexColor + "'>\u2588</font> " + text.getContents() + "</html>";
    }

    @Override
    public void removeMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.remove(tag);
                memoryCollection.removeSeries(series);
                JCheckBox box = checkBoxes.remove(tag);
                memoryCheckBoxPanel.remove(box);

                updateColors();
            }
        });
    }

    @Override
    public void showMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.addSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void hideMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.removeSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void addMemoryData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final TimeSeries series = dataset.get(tag);
                for (DiscreteTimeData<? extends Number> timeData: copy) {
                    RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
                    if (series.getDataItem(period) == null) {
                        Long sizeInBytes = (Long) timeData.getData();
                        Double sizeInMegaBytes = Size.bytes(sizeInBytes).convertTo(Size.Unit.MiB).getValue();
                        series.add(new FixedMillisecond(timeData.getTimeInMillis()), sizeInMegaBytes, false);
                    }
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearMemoryData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                series.clear();
            }
        });
    }

    @Override
    public void addGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.remove(listener);
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

        chart = createMemoryChart();

        chartController = new RecentTimeSeriesChartController(chart);
        JPanel chartPanel = new RecentTimeSeriesChartPanel(chartController);
        chartPanel.setOpaque(false);

        JLabel lblMemory = new SectionHeader(translator.localize(LocaleResources.HOST_MEMORY_SECTION_OVERVIEW));

        JLabel totalMemoryLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));

        memoryCheckBoxPanel.setOpaque(false);

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
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(Box.createGlue(), BorderLayout.EAST);
        mainPanel.add(memoryCheckBoxPanel, BorderLayout.SOUTH);

        BorderLayout visiblePanelBorderLayout = new BorderLayout();
        visiblePanelBorderLayout.setVgap(10);
        visiblePanel.setLayout(visiblePanelBorderLayout);
        visiblePanel.add(Box.createGlue(), BorderLayout.NORTH);
        visiblePanel.add(mainPanel, BorderLayout.CENTER);
        visiblePanel.add(Box.createGlue(), BorderLayout.SOUTH);
    }

    private JFreeChart createMemoryChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, // Title
                translator.localize(LocaleResources.HOST_MEMORY_CHART_TIME_LABEL).getContents(), // x-axis Label
                translator.localize(LocaleResources.HOST_MEMORY_CHART_SIZE_LABEL, Size.Unit.MiB.name()).getContents(), // y-axis Label
                memoryCollection, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );

        chart.getPlot().setBackgroundPaint( new Color(255,255,255,0) );
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRangeMinimumSize(100);

        return chart;
    }

    private void fireShowHideHandlers(boolean show, String tag) {
        for (GraphVisibilityChangeListener listener: listeners) {
            if (show) {
                listener.show(tag);
            } else {
                listener.hide(tag);
            }
        }
    }

    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYItemRenderer itemRenderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < memoryCollection.getSeriesCount(); i++) {
            String tag = (String) memoryCollection.getSeriesKey(i);
            Color color = colors.get(tag);
            itemRenderer.setSeriesPaint(i, color);
        }
    }

    private class MemoryCheckboxListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            JCheckBox source = (JCheckBox) e.getSource();
            fireShowHideHandlers(source.isSelected(), source.getActionCommand());
        }

    }

    @Override
    public Duration getUserDesiredDuration() {
        if (chartController == null) {
            return new Duration(10, TimeUnit.MINUTES);
        }
        return new Duration(chartController.getTimeValue(), chartController.getTimeUnit());
    }
}

