/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.experimental;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.Translate;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import javax.swing.text.JTextComponent;
import java.util.concurrent.TimeUnit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class SingleValueChartPanel extends JPanel {

    static final TimeUnit[] DEFAULT_TIMEUNITS = new TimeUnit[] { TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES };

    public static final String PROPERTY_VISIBLE_TIME_RANGE = "visibleTimeRange";

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int MINIMUM_DRAW_SIZE = 100;

    private ChartPanel chartPanel;

    private JPanel labelContainer;
    private JTextComponent label;

    public SingleValueChartPanel(JFreeChart chart, Duration duration) {

        this.setLayout(new BorderLayout());

        // instead of just disabling display of tooltips, disable their generation too
        if (chart.getPlot() instanceof XYPlot) {
            chart.getXYPlot().getRenderer().setBaseToolTipGenerator(null);
        }

        chart.getXYPlot().getRangeAxis().setAutoRange(true);

        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(duration.unit.toMillis(duration.value));

        chartPanel = new ChartPanel(chart);

        chartPanel.setDisplayToolTips(false);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

        /*
         * By default, ChartPanel scales itself instead of redrawing things when
         * it's resized. To have it resize automatically, we need to set minimum
         * and maximum sizes. Lets constrain the minimum, but not the maximum
         * size.
         */
        chartPanel.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);

        add(chartPanel, BorderLayout.CENTER);
        add(getControlsAndAdditionalDisplay(duration), BorderLayout.SOUTH);

    }

    private Component getControlsAndAdditionalDisplay(Duration duration) {
        JPanel container = new JPanel();
        container.setOpaque(false);

        container.setLayout(new BorderLayout());

        container.add(getChartControls(duration), BorderLayout.LINE_START);
        container.add(getAdditionalDataDisplay(), BorderLayout.LINE_END);

        return container;
    }

    private Component getChartControls(Duration duration) {
        JPanel container = new JPanel();
        container.setOpaque(false);

        final JTextField durationSelector = new JTextField(5);
        final JComboBox<TimeUnit> unitSelector = new JComboBox<>();
        unitSelector.setModel(new DefaultComboBoxModel<>(DEFAULT_TIMEUNITS));

        TimeUnitChangeListener timeUnitChangeListener = new TimeUnitChangeListener(new ActionListener() {
            @Override
            public void actionPerformed(final com.redhat.thermostat.common.ActionEvent actionEvent) {
                Duration d = (Duration) actionEvent.getPayload();
                SingleValueChartPanel.this.firePropertyChange(PROPERTY_VISIBLE_TIME_RANGE, null, d);
            }
        }, duration.value, duration.unit);

        durationSelector.getDocument().addDocumentListener(timeUnitChangeListener);
        unitSelector.addActionListener(timeUnitChangeListener);

        durationSelector.setText(String.valueOf(duration.value));
        unitSelector.setSelectedItem(duration.unit);

        container.add(new JLabel(translator.localize(LocaleResources.CHART_DURATION_SELECTOR_LABEL).getContents()));
        container.add(durationSelector);
        container.add(unitSelector);

        return container;
    }

    private Component getAdditionalDataDisplay() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        labelContainer = new JPanel();
        labelContainer.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(labelContainer, constraints);
        return panel;
    }

    public void setTimeRangeToShow(int timeValue, TimeUnit timeUnit) {
        XYPlot plot = chartPanel.getChart().getXYPlot();

        // Don't drop old data; just dont' show it.
        plot.getDomainAxis().setAutoRange(true);
        plot.getDomainAxis().setFixedAutoRange(timeUnit.toMillis(timeValue));
    }

    public void setDataInformationLabel(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label == null) {
                    label = new ValueField(text);
                    labelContainer.add(label);
                }

                label.setText(text);
            }
        });
    }

}

