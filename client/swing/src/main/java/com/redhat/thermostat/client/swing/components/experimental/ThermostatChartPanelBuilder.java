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

package com.redhat.thermostat.client.swing.components.experimental;

import java.util.Objects;

import org.jfree.chart.JFreeChart;

import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel.UnitRange;
import com.redhat.thermostat.common.Duration;

/**
 * A builder for {@code ThermostatChartPanel}.
 * 
 */
public class ThermostatChartPanelBuilder {
    
    // XY-Plot is fixed auto-range by default
    private boolean fixedRange = true;
    private Duration duration = ThermostatChartPanel.DEFAULT_DATA_DISPLAY;
    private JFreeChart chart;
    private UnitRange unitRange = UnitRange.DEFAULT;

    /**
     * Sets the initial duration for {@code ThermostatChartPanel}. Default is
     * 10 minutes.
     * 
     * @param duration The initial duration to set
     * @return this builder.
     * 
     * @throws NullPointerException if {@code duration} was null.
     */
    public ThermostatChartPanelBuilder duration(Duration duration) {
        this.duration = Objects.requireNonNull(duration);
        return this;
    }
    
    /**
     * Sets the chart for {@code ThermostatChartPanel}. Defaults to
     * {@code null}.
     * 
     * @param chart The chart to set
     * @return this builder.
     * 
     * @throws NullPointerException if {@code chart} was null.
     */
    public ThermostatChartPanelBuilder chart(JFreeChart chart) {
        this.chart = Objects.requireNonNull(chart);
        return this;
    }
    
    /**
     * Sets whether {@code ThermostatChartPanel} should have fixed auto ranges.
     * Only applicable for {@link XYPlot} charts.
     * 
     * @param fixedRange the new value
     * @return this builder
     * @see #chart
     */
    public ThermostatChartPanelBuilder xyPlotFixedAutoRange(boolean fixedRange) {
        this.fixedRange = fixedRange;
        return this;
    }
    
    /**
     * Sets the unit range for the {@code ThermostatChartPanel}. Default is
     * coarse.
     * 
     * @param range The unit range to set.
     * @return this builder
     * 
     * @throws NullPointerException if {@code range} was null.
     */
    public ThermostatChartPanelBuilder unitRange(UnitRange range) {
        this.unitRange = Objects.requireNonNull(range);
        return this;
    }
    
    /**
     * 
     * @return A configured {@code ThermostatChartPanel}.
     */
    public ThermostatChartPanel build() {
        return new ThermostatChartPanel(chart, duration, fixedRange, unitRange);
    }
    
}
