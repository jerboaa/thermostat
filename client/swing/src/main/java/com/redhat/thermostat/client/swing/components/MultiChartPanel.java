/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

/**
 * Displays multiple time-series data sets in a single chart, allowing users to
 * select the data range and the individual data sets to show.
 * <p>
 * Each time-series data belongs to a data group ({@link DataGroup}). A data
 * group is used to collected a related set of data that represent similar
 * units. For example, all data items representing a size.
 * <p>
 * A {@code tag} identifies each unique set of data points. Each {@code tag}
 * belongs to a particular data group.
 */
public class MultiChartPanel extends JPanel {

    private static final String KEY_TAG = "tag";
    private static final String KEY_GROUP = "group";

    private final JFreeChart chart;
    private final RecentTimeSeriesChartController chartController;

    private final CheckboxListener checkboxListener = new CheckboxListener();

    private final JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

    private final Map<String, TimeSeries> dataset = new HashMap<>();
    private final Map<String, JCheckBox> checkBoxes = new HashMap<>();
    private final Map<String, Color> colors = new HashMap<>();

    private int highestGroupIndex = -1;

    public MultiChartPanel() {

        chart = createChart();
        chartController = new RecentTimeSeriesChartController(chart);

        setOpaque(false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(chartController);
        chartPanel.setOpaque(false);

        ThermostatScrollPane checkBoxContainer = new ThermostatScrollPane(checkBoxPanel);

        checkBoxPanel.setOpaque(false);
        checkBoxPanel.setBorder(new EmptyBorder(5, 5, 15, 5));
        checkBoxContainer.setBorder(new EmptyBorder(0, 0, 0, 0));

        BorderLayout mainPanelBorderLayout = new BorderLayout();
        setLayout(mainPanelBorderLayout);

        add(chartPanel, BorderLayout.CENTER);
        add(checkBoxContainer, BorderLayout.SOUTH);
    }

    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, // Title
                null, // x axis label
                null, // y axis label
                null, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );

        chart.getPlot().setBackgroundPaint(new Color(255,255,255,0));
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRangeMinimumSize(100);
        rangeAxis.setRangeType(RangeType.POSITIVE);

        return chart;
    }

    public void setDomainAxisLabel(String label) {
        ValueAxis axis = chart.getXYPlot().getDomainAxis();
        axis.setLabel(label);
    }

    /**
     * side effect: sets up the axis for the groupIndex if it was not setup
     * before
     */
    public NumberAxis getRangeAxis(final DataGroup group) {
        NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis(group.index);
        if (axis == null) {
            try {
                axis = (NumberAxis) chart.getXYPlot().getRangeAxis().clone();
                chart.getXYPlot().setRangeAxis(group.index, axis);
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("cloneable can not clone?!?!?");
            }
        }
        return axis;
    }

    /**
     * The chart will not be displayed until {@link #showChart(String)} is called.
     */
    public void addChart(final DataGroup group, final String tag, final LocalizedString name) {
        assertTagIsAbsent(tag);

        int colorIndex = colors.size();
        colors.put(tag, ChartColors.getColor(colorIndex));

        TimeSeries series = new TimeSeries(tag);
        dataset.put(tag, series);

        if (!group.addedToChart) {
            chart.getXYPlot().setDataset(group.index, group.collection);
            chart.getXYPlot().mapDatasetToRangeAxis(group.index, group.index);

            XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
            chart.getXYPlot().setRenderer(group.index, renderer);

            getRangeAxis(group); // side-effect: set up the axis

            group.addedToChart = true;
        }

        JCheckBox newCheckBox = new LegendCheckBox(name, colors.get(tag));
        newCheckBox.putClientProperty(KEY_TAG, tag);
        newCheckBox.putClientProperty(KEY_GROUP, group);
        newCheckBox.setSelected(true);
        newCheckBox.addActionListener(checkboxListener);
        newCheckBox.setOpaque(false);
        checkBoxes.put(tag, newCheckBox);
        checkBoxPanel.add(newCheckBox);

        updateColors();
    }

    public void removeChart(final DataGroup group, final String tag) {
        assertTagIsPresent(tag);

        TimeSeries series = dataset.remove(tag);
        TimeSeriesCollection collection = group.collection;
        collection.removeSeries(series);
        JCheckBox box = checkBoxes.remove(tag);
        checkBoxPanel.remove(box);

        updateColors();
    }

    public void showChart(final DataGroup group, final String tag) {
        assertTagIsPresent(tag);

        TimeSeries series = dataset.get(tag);
        TimeSeriesCollection collection = group.collection;
        collection.addSeries(series);

        updateColors();
    }

    public void hideChart(final DataGroup group, final String tag) {
        assertTagIsPresent(tag);

        TimeSeries series = dataset.get(tag);
        TimeSeriesCollection collection = group.collection;
        collection.removeSeries(series);

        updateColors();
    }

    public void addData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        assertTagIsPresent(tag);

        final TimeSeries series = dataset.get(tag);
        for (DiscreteTimeData<? extends Number> timeData: data) {
            RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
            if (series.getDataItem(period) == null) {
                series.add(new FixedMillisecond(timeData.getTimeInMillis()), timeData.getData(), false);
            }
        }
        series.fireSeriesChanged();
    }

    public void clearData(final String tag) {
        assertTagIsPresent(tag);

        TimeSeries series = dataset.get(tag);
        series.clear();
    }

    public Duration getUserDesiredDuration() {
        if (chartController == null) {
            return new Duration(10, TimeUnit.MINUTES);
        }
        return new Duration(chartController.getTimeValue(), chartController.getTimeUnit());
    }

    private void assertTagIsPresent(String tag) {
        if (!dataset.containsKey(tag)) {
            throw new IllegalArgumentException("Does not contain chart with tag '" + tag + "'");
        }
        if (!checkBoxes.containsKey(tag)) {
            throw new IllegalArgumentException("Does not contain chart with tag '" + tag + "'");
        }
        if (!colors.containsKey(tag)) {
            throw new IllegalArgumentException("Does not contain chart with tag '" + tag + "'");
        }
    }

    private void assertTagIsAbsent(String tag) {
        if (dataset.containsKey(tag)) {
            throw new IllegalArgumentException("Already contains chart with tag '" + tag + "'");
        }
        if (checkBoxes.containsKey(tag)) {
            throw new IllegalArgumentException("Already contains chart with tag '" + tag + "'");
        }
        if (colors.containsKey(tag)) {
            throw new IllegalArgumentException("Already contains chart with tag '" + tag + "'");
        }
    }

    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYPlot plot = chart.getXYPlot();
        for (int j = 0; j < plot.getDatasetCount(); j++) {
            XYItemRenderer itemRenderer = plot.getRenderer(j);
            XYDataset series = plot.getDataset(j);
            for (int i = 0; i < series.getSeriesCount(); i++) {
                String tag = (String) series.getSeriesKey(i);
                Color color = colors.get(tag);
                itemRenderer.setSeriesPaint(i, color);
            }
        }
    }

    private class CheckboxListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            JCheckBox source = (JCheckBox) e.getSource();
            boolean show = source.isSelected();
            String tag = (String) source.getClientProperty(KEY_TAG);
            DataGroup group = (DataGroup) source.getClientProperty(KEY_GROUP);
            if (show) {
                showChart(group, tag);
            } else {
                hideChart(group, tag);
            }
        }
    }

    public DataGroup createGroup() {
        return new DataGroup();
    }

    public class DataGroup {

        private final int index;
        private final TimeSeriesCollection collection;
        private boolean addedToChart = false;

        private DataGroup() {
            highestGroupIndex++;
            this.index = highestGroupIndex;
            
            this.collection = new TimeSeriesCollection();
        }
    }
    
}
