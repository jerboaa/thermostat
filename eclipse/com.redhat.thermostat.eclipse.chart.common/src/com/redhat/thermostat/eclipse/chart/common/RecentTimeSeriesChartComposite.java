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

package com.redhat.thermostat.eclipse.chart.common;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.eclipse.ThermostatConstants;

public class RecentTimeSeriesChartComposite extends Composite {
    public static final String TEST_ID_UNIT_COMBO = "RecentTimeSeriesChartComposite.timeUnit";
    public static final String TEST_ID_DURATION_TEXT = "RecentTimeSeriesChartComposite.timeDuration";
    
    private static final int MINIMUM_DRAW_SIZE = 100;
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final RecentTimeSeriesChartController controller;

    private Composite labelContainer;
    private Label label;
    private Combo unitSelector;
    private Text durationSelector;

    public RecentTimeSeriesChartComposite(Composite parent, int style, JFreeChart chart) {
        super(parent, style);
        this.setLayout(new GridLayout());
        this.controller = new RecentTimeSeriesChartController(this, chart);

        final ChartPanel cp = controller.getChartComposite();

        cp.setDisplayToolTips(false);
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

        getControlsAndAdditionalDisplay(this);
    }

    private Composite getControlsAndAdditionalDisplay(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout containerLayout = new GridLayout(2, false);
        containerLayout.marginHeight = 0;
        container.setLayout(containerLayout);

        getChartControls(container);
        getAdditionalDataDisplay(container);

        return container;
    }

    private Composite getChartControls(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        RowLayout topLayout = new RowLayout(SWT.HORIZONTAL);
        topLayout.center = true;
        topLayout.marginHeight = 0;
        container.setLayout(topLayout);
        
        Label prompt = new Label(container, SWT.NONE);
        prompt.setText(translator.localize(LocaleResources.CHART_DURATION_SELECTOR_LABEL));

        durationSelector = new Text(container, SWT.BORDER);
        durationSelector.setData(ThermostatConstants.TEST_TAG, TEST_ID_DURATION_TEXT);
        // Make 5 chars wide
        GC gc = new GC(durationSelector);
        FontMetrics metrics = gc.getFontMetrics();
        int width = metrics.getAverageCharWidth() * 5;
        int height = metrics.getHeight();
        gc.dispose();
        durationSelector.setLayoutData(new RowData(computeSize(width, height)));
        
        unitSelector = new Combo(container, SWT.NONE);
        unitSelector.setData(ThermostatConstants.TEST_TAG, TEST_ID_UNIT_COMBO);
        TimeUnit[] units = controller.getTimeUnits();
        for (TimeUnit unit : units) {
            unitSelector.add(unit.toString());
        }

        int defaultValue = controller.getTimeValue();
        TimeUnit defaultUnit = controller.getTimeUnit();

        TimeUnitChangeListener timeUnitChangeListener = new TimeUnitChangeListener(controller, defaultValue, defaultUnit);

        durationSelector.addModifyListener(timeUnitChangeListener);
        unitSelector.addSelectionListener(timeUnitChangeListener);

        durationSelector.setText(String.valueOf(defaultValue));
        int defaultPos = Arrays.asList(units).indexOf(defaultUnit);
        unitSelector.select(defaultPos);

        return container;
    }

    private Composite getAdditionalDataDisplay(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        GridLayout topLayout = new GridLayout();
        topLayout.marginHeight = 0;
        top.setLayout(topLayout);
        top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        labelContainer = new Composite(top, SWT.NONE);
        return top;
    }

    public void setDataInformationLabel(final String text) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (label == null) {
                    label = new Label(labelContainer, SWT.NONE);
                }

                label.setText(text);
            }
        });
    }

    private class TimeUnitChangeListener implements SelectionListener, ModifyListener {

        private final RecentTimeSeriesChartController controller;
        private int value;
        private TimeUnit unit;

        public TimeUnitChangeListener(RecentTimeSeriesChartController controller, int defaultValue, TimeUnit defaultUnit) {
            this.controller = controller;
            this.value = defaultValue;
            this.unit = defaultUnit;
        }

        private void updateChartParameters() {
            controller.setTime(value, unit);
        }

        @Override
        public void modifyText(ModifyEvent e) {
            if (durationSelector.equals(e.widget)) {
                try {
                    this.value = Integer.valueOf(durationSelector.getText());
                    updateChartParameters();
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            }
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (unitSelector.equals(e.widget)) {
                int idx = unitSelector.getSelectionIndex();
                if (idx >= 0) {
                    unit = controller.getTimeUnits()[idx];
                    updateChartParameters();
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
    }
    
    public RecentTimeSeriesChartController getController() {
        return controller;
    }
}
