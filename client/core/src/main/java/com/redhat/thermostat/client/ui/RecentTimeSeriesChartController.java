/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.util.concurrent.TimeUnit;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.redhat.thermostat.common.Duration;

public class RecentTimeSeriesChartController {

    private static final int DEFAULT_VALUE = 10;
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;

    private JFreeChart chart;
    private ChartPanel panel;
    private long timeValue = DEFAULT_VALUE;
    private TimeUnit timeUnit = DEFAULT_UNIT;

    public RecentTimeSeriesChartController(JFreeChart chart) {
        this.chart = chart;
        this.panel = new ChartPanel(chart);

        // instead of just disabling display of tooltips, disable their generation too
        if (chart.getPlot() instanceof XYPlot) {
            chart.getXYPlot().getRenderer().setBaseToolTipGenerator(null);
        }

        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(timeUnit.toMillis(timeValue));

        chart.getXYPlot().getRangeAxis().setAutoRange(true);

    }

    public ChartPanel getChartPanel() {
        return panel;
    }

    public TimeUnit[] getTimeUnits() {
        return new TimeUnit[] { TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES };
    }

    public long getTimeValue() {
        return timeValue;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTime(Duration duration) {
        this.timeValue = duration.getValue();
        this.timeUnit = duration.getUnit();

        updateChart();
    }

    private void updateChart() {
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(timeUnit.toMillis(timeValue));
    }

}

