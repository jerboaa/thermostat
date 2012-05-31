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

package com.redhat.thermostat.client.stats.memory;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;

import javax.swing.plaf.ColorUIResource;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

public class StatsModel {

    private String name;
    
    private TimeSeries dataSet;
    
    public StatsModel() {
        dataSet = new TimeSeries("");
    }
    
    BufferedImage getChart(int width, int height, ColorUIResource bgColor, ColorUIResource fgColor) {
        JFreeChart chart = createChart(bgColor, fgColor);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        chart.draw((Graphics2D) image.getGraphics(), new Rectangle2D.Double(0, 0, width, height), null);
        
        return image;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        dataSet.setDescription(name);
        this.name = name;
    }
    
    public void addData(long timestamp, double value) {
        Millisecond millisecond = new Millisecond(new Date(timestamp));
        if (dataSet.getValue(millisecond) == null) {
            dataSet.add(millisecond, value);
        }
    }
    
    /**
     * Creates a chart.
     *
     * @return a chart.
     */
    private JFreeChart createChart(ColorUIResource bgColor, ColorUIResource fgColor) {

        XYDataset priceData = new TimeSeriesCollection(dataSet);

        XYPlot plot = new XYPlot();
        
        plot.setDomainGridlinesVisible(false);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeCrosshairVisible(false);
                
        DateAxis dateAxis = new DateAxis();
        
        dateAxis.setTickLabelsVisible(false);
        dateAxis.setTickMarksVisible(false);
        dateAxis.setAxisLineVisible(false);
        dateAxis.setNegativeArrowVisible(false);
        dateAxis.setPositiveArrowVisible(false);
        dateAxis.setVisible(false);
        
        NumberAxis numberAxis = new NumberAxis();
        numberAxis.setTickLabelsVisible(false);
        numberAxis.setTickMarksVisible(false);
        numberAxis.setAxisLineVisible(false);
        numberAxis.setNegativeArrowVisible(false);
        numberAxis.setPositiveArrowVisible(false);
        numberAxis.setVisible(false);
        numberAxis.setAutoRangeIncludesZero(false);
        
        plot.setDomainAxis(dateAxis);
        plot.setRangeAxis(numberAxis);
        plot.setDataset(priceData);
        
        plot.setInsets(new RectangleInsets(-1, -1, 0, 0));
        
        plot.setRenderer(new StandardXYItemRenderer(StandardXYItemRenderer.LINES));
        plot.setBackgroundPaint(bgColor);
        
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        
        plot.getRenderer().setSeriesPaint(0, fgColor);
        chart.setAntiAlias(true);
        chart.setBorderVisible(false);
        
        return chart;

    }
}
