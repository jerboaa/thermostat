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

package com.redhat.thermostat.thread.client.common.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.lang.Thread.State;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import com.redhat.thermostat.thread.client.common.ThreadTableBean;

public class ThreadDeatailsPieChart {
    
    private ThreadTableBean thread;
    
    public ThreadDeatailsPieChart(ThreadTableBean thread) {
        this.thread = thread;
    }
    
    public JFreeChart createChart() {
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        dataset.setValue(State.RUNNABLE, thread.getRunningPercent());
        dataset.setValue(State.WAITING, thread.getWaitingPercent());
        dataset.setValue(State.BLOCKED, thread.getMonitorPercent());
        dataset.setValue(State.TIMED_WAITING, thread.getSleepingPercent());
        
        JFreeChart chart = ChartFactory.createPieChart(
                thread.getName(),       // chart title
                dataset,                // data
                true,                   // include legend
                true,
                false
            );

        chart.setAntiAlias(true);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        
        plot.setStartAngle(290);
        plot.setForegroundAlpha(0.5f);
        
        Color color = ChartColors.getColor(State.RUNNABLE);
        plot.setSectionPaint(State.RUNNABLE, createGradientPaint(color.brighter(), color));
        
        color = ChartColors.getColor(State.WAITING);
        plot.setSectionPaint(State.WAITING, createGradientPaint(color.brighter(), color));
        
        color = ChartColors.getColor(State.BLOCKED);
        plot.setSectionPaint(State.BLOCKED, createGradientPaint(color.brighter(), color));
        
        color = ChartColors.getColor(State.TIMED_WAITING);
        plot.setSectionPaint(State.TIMED_WAITING, createGradientPaint(color.brighter(), color));
        
        plot.setBaseSectionOutlinePaint(Color.WHITE);
        plot.setSectionOutlinesVisible(true);
        plot.setBaseSectionOutlineStroke(new BasicStroke(2.0f));
        
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} - {2}"));
                
        return chart;
    }
    
    private static RadialGradientPaint createGradientPaint(Color c1, Color c2) {
        Point2D center = new Point2D.Float(0, 0);
        float radius = 200;
        float[] dist = {0.0f, 1.0f};
        return new RadialGradientPaint(center, radius, dist, new Color[] {c1, c2});
    }
}
