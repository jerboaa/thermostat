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

package com.redhat.thermostat.vm.heap.analysis.client.core.chart;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.util.Date;

import javax.swing.plaf.ColorUIResource;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.ui.BytesTickUnit;

public class OverviewChart {
        
    private static final ColorUIResource MAIN_BAR_BASE_COLOR = new ColorUIResource(0x4A90D9);

    private static final String lock = new String("chartLock");

    private TimeSeries total;
    private TimeSeries used;
    private String title;
    private String xAxis;
    private String yAxis;

    private JFreeChart chart;
    
    public OverviewChart(String title, String xAxis, String yAxis, String mainSeries, String secondarySeries) {
        
        this.title = title;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        
        total = new TimeSeries(mainSeries);
        total.setDescription(mainSeries);
        
        used = new TimeSeries(secondarySeries);
        used.setDescription(secondarySeries);
    }
    
    public JFreeChart getChart() {
        return chart;
    }
    
    public void createChart(int height, Color bgColor) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        
        synchronized (lock) {
            dataset.addSeries(total);
            dataset.addSeries(used);
        }
        
        chart = ChartFactory.createTimeSeriesChart(
                title,
                xAxis,
                yAxis,
                dataset,
                true,  // legend
                false,  // tool tips
                false   // URLs
        );

        Paint paint = new GradientPaint(0, 0, MAIN_BAR_BASE_COLOR, 0, height, bgColor);
        Paint paint2 = new GradientPaint(0, 0, Color.GREEN, 0, height, bgColor);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        XYDifferenceRenderer r = new XYDifferenceRenderer(paint, paint2, false);
        r.setRoundXCoordinates(true);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setRangeCrosshairLockedOnData(true);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setRenderer(r);

        ValueAxis domainAxis = new DateAxis(xAxis);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        plot.setDomainAxis(domainAxis);
        plot.setForegroundAlpha(0.5f);
        TickUnits tickUnits = new TickUnits();
        tickUnits.add(new BytesTickUnit(1.));
        tickUnits.add(new BytesTickUnit(10.));
        tickUnits.add(new BytesTickUnit(100.));
        tickUnits.add(new BytesTickUnit(1000.));
        tickUnits.add(new BytesTickUnit(10000.));
        tickUnits.add(new BytesTickUnit(100000.));
        tickUnits.add(new BytesTickUnit(1000000.));
        tickUnits.add(new BytesTickUnit(10000000.));
        tickUnits.add(new BytesTickUnit(100000000.));
        tickUnits.add(new BytesTickUnit(1000000000.));
        tickUnits.add(new BytesTickUnit(10000000000.));
        tickUnits.add(new BytesTickUnit(100000000000.));
        tickUnits.add(new BytesTickUnit(1000000000000.));
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(tickUnits);
        yAxis.setRangeType(RangeType.POSITIVE);
        yAxis.setAutoRangeMinimumSize(10);
        yAxis.setAutoRange(true);
    }

    public void addData(long timeStamp, long used, long total) {
        
        Millisecond millisecond = new Millisecond(new Date(timeStamp));
        synchronized (lock) {            
            if (this.total.getValue(millisecond) == null) {
                this.total.add(millisecond, total, false);
                this.total.removeAgedItems(true);
            }
            
            if (this.used.getValue(millisecond) == null) {
                this.used.add(millisecond, used, false);
                this.used.removeAgedItems(true);
            }
        }
        
    }

    public void notifyListenersOfModelChange() {
        synchronized (lock) {
            this.total.fireSeriesChanged();
            this.used.fireSeriesChanged();
        }
    }

    public void setRange(int seconds) {
        total.setMaximumItemCount(seconds);
        used.setMaximumItemCount(seconds);
    }
}

