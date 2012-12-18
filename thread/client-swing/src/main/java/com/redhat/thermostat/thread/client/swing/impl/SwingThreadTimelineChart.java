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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.GradientRoundBorder;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.LongRange;
import com.redhat.thermostat.common.model.LongRangeNormalizer;
import com.redhat.thermostat.thread.client.common.ThreadTimelineBean;
import com.redhat.thermostat.thread.client.common.chart.ChartColors;

@SuppressWarnings("serial")
public class SwingThreadTimelineChart extends JPanel {

    private String leftMarkerMessage; 
    private String rightMarkerMessage;
    
    public static final String HIGHLIGHT_THREAD_STATE_PROPERTY = "highlightThreadThreadProperty";
    private ThreadTimelineBean selectedThread;
    
    private List<ThreadTimelineBean> timeline;
    
    private LongRangeNormalizer normalizer;

    private Point clickArea;
    
    public SwingThreadTimelineChart(List<ThreadTimelineBean> timeline, long rangeStart, long rangeStop) {
        this.timeline = timeline;
    
        LongRange range = new LongRange();
        range.setMin(rangeStart);
        range.setMax(rangeStop);
        
        setBorder(new GradientRoundBorder());
        normalizer = new LongRangeNormalizer(range);
    }

    public void clickAndHighlightArea(Point point) {
        clickArea = point;
        repaint();
    }
   
    public void unsetHighlightArea() {
        clickArea = null;
        selectedThread = null;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        ThreadTimelineBean oldSelectedThread = selectedThread;
        
        normalizer.setMinNormalized(0);
        normalizer.setMaxNormalized(getWidth() - 3);
        
        GraphicsUtils utils = GraphicsUtils.getInstance();
        
        Graphics2D graphics = utils.createAAGraphics(g);
        
        Font font = graphics.getFont();
        graphics.setFont(font.deriveFont(font.getSize() - 2));
        
        int y = getHeight()/3;
        graphics.clearRect(0, 0, getWidth(), getHeight());
        graphics.drawString(timeline.get(0).getName(), 2, y);
        
        y = getHeight()/2;

        graphics.translate(2, y);
        if (clickArea != null) {
            clickArea.translate(0, -y);
        }
        
        int w = getWidth() - 4;
        int h = 15;
        
        Shape shape = utils.getRoundShape(w, h);

        Paint paint = new GradientPaint(0, 0, Palette.DARK_GRAY.getColor(), 0, h, Palette.WHITE.getColor());
        graphics.setPaint(paint);        
        graphics.fill(shape);        

                
        for (ThreadTimelineBean thread : timeline) {
            
            boolean isSelected = false;
            normalizer.setValue(thread.getStartTime());
            int x0 = (int) normalizer.getValueNormalized();

            normalizer.setValue(thread.getStopTime());
            int x1 = (int) normalizer.getValueNormalized();

            Color currentThreadColour = ChartColors.getColor(thread.getState());
            Rectangle currentArea = new Rectangle(x0, 0, x1 - x0, h);
            if (clickArea != null && currentArea.contains(clickArea)) {
                currentThreadColour = Palette.THERMOSTAT_BLU.getColor();
                selectedThread = thread;
                isSelected = true;
                
            } else if (thread.isHighlight()) {
                currentThreadColour = Palette.THERMOSTAT_BLU.getColor();
                isSelected = true;
            }
            
            graphics.setColor(currentThreadColour);
            graphics.fillRect(x0, 1, x1 - x0, h - 1);
            
            if (isSelected) {
                paintMarks(graphics, x0, x1, h, thread.getStartTime(), thread.getStopTime(), ChartColors.getColor(thread.getState()));
                String tooltipString = "";
                if (leftMarkerMessage != null) {
                    tooltipString = leftMarkerMessage + " - ";
                }
                if (rightMarkerMessage != null) {
                    tooltipString += " " + rightMarkerMessage;
                }
                setToolTipText(tooltipString);
            }
        }
        
        graphics.setColor(ChartColors.getColor(timeline.get(timeline.size() - 1).getState()));
        graphics.draw(shape);

        graphics.dispose();
        
        if (selectedThread != null || oldSelectedThread != null) {
            firePropertyChange(HIGHLIGHT_THREAD_STATE_PROPERTY, oldSelectedThread, selectedThread);
        }
    }
    
    private void paintMarks(Graphics2D graphics, int x0, int x1, int y, long start, long stop, Color colour) {
        
        graphics.setColor(colour);
        graphics.fillRect(x0, y + 5, x1 - x0, 2);
        
        GraphicsUtils utils = GraphicsUtils.getInstance();
        FontMetrics metrics = utils.getFontMetrics(this, graphics.getFont());
        
        graphics.setColor(getForeground());
        if (leftMarkerMessage != null) {
            Rectangle2D rect = metrics.getStringBounds(leftMarkerMessage, graphics);
            graphics.drawString(leftMarkerMessage, x0 - (int) rect.getWidth(), y - (int) rect.getY());
        }
        
        if (rightMarkerMessage != null) {
            Rectangle2D rect = metrics.getStringBounds(leftMarkerMessage, graphics);
            graphics.drawString(rightMarkerMessage, x1, y - (int) rect.getY());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                ThreadTimelineBean bean1 = new ThreadTimelineBean();
                bean1.setName("test");
                bean1.setStartTime(0);
                bean1.setStopTime(1000);
                bean1.setState(Thread.State.BLOCKED);
                
                ThreadTimelineBean bean2 = new ThreadTimelineBean();
                bean2.setName("test");
                bean2.setStartTime(1000);
                bean2.setStopTime(2000);
                bean2.setState(Thread.State.RUNNABLE);
                
                ThreadTimelineBean bean3 = new ThreadTimelineBean();
                bean3.setName("test");
                bean3.setStartTime(2000);
                bean3.setStopTime(2100);
                bean3.setState(Thread.State.TIMED_WAITING);
                
                List<ThreadTimelineBean> timeline = new ArrayList<>();
                timeline.add(bean1);
                timeline.add(bean2);
                timeline.add(bean3);

                bean3.setHighlight(true);
                
                SwingThreadTimelineChart chart = new SwingThreadTimelineChart(timeline, 0, 2500);
                chart.setMarkersMessage(new Date(bean3.getStartTime()).toString(), new Date(bean3.getStopTime()).toString());
                
                frame.add(chart);
                frame.setSize(800, 150);
                
                frame.setVisible(true);
            }
        });
    }

    public void setMarkersMessage(String leftMarkerMessage, String rightMarkerMessage) {
        this.leftMarkerMessage = leftMarkerMessage;
        this.rightMarkerMessage = rightMarkerMessage;   
    }
}
