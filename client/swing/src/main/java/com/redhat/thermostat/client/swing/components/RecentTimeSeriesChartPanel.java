/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.jfree.chart.ChartPanel;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.common.locale.Translate;

public class RecentTimeSeriesChartPanel extends JPanel {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final long serialVersionUID = -1733906800911900456L;
    private static final int MINIMUM_DRAW_SIZE = 100;

    private final RecentTimeSeriesChartController controller;

    private JPanel labelContainer;
    private JTextComponent label;

    public RecentTimeSeriesChartPanel(RecentTimeSeriesChartController controller) {
        this.controller = controller;

        this.setLayout(new BorderLayout());

        final ChartPanel cp = controller.getChartPanel();

        cp.setDisplayToolTips(false);
        cp.setDoubleBuffered(true);
        cp.setMouseZoomable(false);
        cp.setPopupMenu(null);

        /*
         * By default, ChartPanel scales itself instead of redrawing things when
         * it's resized. To have it resize automatically, we need to set minimum
         * and maximum sizes. Lets constrain the minimum, but not the maximum
         * size.
         */
        cp.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
        cp.setMaximumDrawHeight(Integer.MAX_VALUE);
        cp.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);
        cp.setMaximumDrawWidth(Integer.MAX_VALUE);

        add(getControlsAndAdditionalDisplay(), BorderLayout.SOUTH);

        add(cp, BorderLayout.CENTER);
    }

    private Component getControlsAndAdditionalDisplay() {
        JPanel container = new JPanel();
        container.setOpaque(false);

        container.setLayout(new BorderLayout());

        container.add(getChartControls(), BorderLayout.LINE_START);
        container.add(getAdditionalDataDisplay(), BorderLayout.LINE_END);

        return container;
    }

    private Component getChartControls() {
        JPanel container = new JPanel();
        container.setOpaque(false);

        final JTextField durationSelector = new JTextField(5);
        final JComboBox<TimeUnit> unitSelector = new JComboBox<>();
        unitSelector.setModel(new DefaultComboBoxModel<>(controller.getTimeUnits()));

        int defaultValue = controller.getTimeValue();
        TimeUnit defaultUnit = controller.getTimeUnit();

        TimeUnitChangeListener timeUnitChangeListener = new TimeUnitChangeListener(controller, defaultValue, defaultUnit);

        durationSelector.getDocument().addDocumentListener(timeUnitChangeListener);
        unitSelector.addActionListener(timeUnitChangeListener);

        durationSelector.setText(String.valueOf(defaultValue));
        unitSelector.setSelectedItem(defaultUnit);

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

    private static class TimeUnitChangeListener implements DocumentListener, ActionListener {

        private final RecentTimeSeriesChartController controller;
        private int value;
        private TimeUnit unit;

        public TimeUnitChangeListener(RecentTimeSeriesChartController controller, int defaultValue, TimeUnit defaultUnit) {
            this.controller = controller;
            this.value = defaultValue;
            this.unit = defaultUnit;
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        private void changed(Document doc) {
            try {
                this.value = Integer.valueOf(doc.getText(0, doc.getLength()));
            } catch (NumberFormatException nfe) {
                // ignore
            } catch (BadLocationException ble) {
                // ignore
            }
            updateChartParameters();
        }

        private void updateChartParameters() {
            controller.setTime(value, unit);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            @SuppressWarnings("unchecked") // We are a TimeUnitChangeListener, specifically.
            JComboBox<TimeUnit> comboBox = (JComboBox<TimeUnit>) e.getSource();
            TimeUnit time = (TimeUnit) comboBox.getSelectedItem();
            this.unit = time;
            updateChartParameters();
        }
    }
}

