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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.beans.Transient;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.GradientPanel;

import com.redhat.thermostat.client.swing.components.timeline.TimelineUtils;

import com.redhat.thermostat.client.ui.Palette;

import com.redhat.thermostat.common.model.LongRange;
import com.redhat.thermostat.common.model.LongRangeNormalizer;

import com.redhat.thermostat.thread.client.common.Timeline;
import com.redhat.thermostat.thread.client.common.TimelineInfo;

@SuppressWarnings("serial")
public class TimelineComponent extends GradientPanel {
    
    private boolean selected = false;
    
    private Timeline timeline;
    private JScrollPane scrollPane;
    private LongRange range;
    
    private long millsUnitIncrement;
    private int pixelUnitIncrement;
    
    public TimelineComponent(LongRange range, Timeline timeline, JScrollPane scrollPane)
    {
        super(Palette.LIGHT_GRAY.getColor(), Palette.WHITE.getColor());
        this.range = range;
        this.scrollPane = scrollPane;
        this.timeline = timeline;
        
        millsUnitIncrement = 1_000;
        pixelUnitIncrement = 20;
    }

    public void setUnitIncrementInPixels(int increment) {
        this.pixelUnitIncrement = increment;
    }
    
    public void setUnitIncrementInMillis(long increment) {
        this.millsUnitIncrement = increment;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
        Rectangle bounds = g.getClipBounds();

        if (!selected) {
            super.paintComponent(g);
        } else {
            graphics.setColor(Palette.EGYPTIAN_BLUE.getColor());
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        
        int currentValue = scrollPane.getHorizontalScrollBar().getValue();
        int totalInc = pixelUnitIncrement;
        TimelineUtils.drawMarks(range, graphics, bounds, currentValue, false, totalInc);

        drawBoldMarks(graphics, currentValue, bounds, totalInc);
        Color lastColor = drawTimeline(graphics, currentValue, bounds);
        
        drawThreadName(graphics, bounds, lastColor);
        
        graphics.dispose();
    }
    
    private void drawThreadName(Graphics2D graphics, Rectangle bounds, Color lastColor) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        
        Color up = utils.deriveWithAlpha(Palette.WHITE.getColor(), 200);
        Color bottom = utils.deriveWithAlpha(Palette.GRAY.getColor(), 200);
        Paint gradient = new GradientPaint(0, 0, up, 0, getHeight(), bottom);
        
        Font font = TimelineUtils.FONT;
        
        graphics.setFont(font);
        graphics.setPaint(gradient);
        
        String value = timeline.getName();
        
        int stringWidth = (int) font.getStringBounds(value, graphics.getFontRenderContext()).getWidth() - 1;
        int stringHeight = (int) font.getStringBounds(value, graphics.getFontRenderContext()).getHeight();
        graphics.fillRect(bounds.x + 1, bounds.y + 12, stringWidth + 4, stringHeight + 4);
        
        graphics.setColor(Palette.THERMOSTAT_BLU.getColor());                
        graphics.drawString(value, bounds.x + 1, bounds.y + stringHeight + 12);
        
        graphics.setColor(lastColor);                
        graphics.drawLine(bounds.x + 1, bounds.y + 12 + stringHeight + 4, bounds.x + stringWidth + 4, bounds.y + 12 + stringHeight + 4);
    }
    
    private Color drawTimeline(Graphics2D graphics, int currentValue, Rectangle bounds) {
        
        if (timeline.size() == 0) {
            return Palette.GRAY.getColor();
        }
        
        TimelineInfo[] infos = timeline.toArray();
        Color lastColor = infos[infos.length - 1].getColor().getColor();
        
        LongRangeNormalizer normalizer = new LongRangeNormalizer(range, 0, getWidth());
        
        for (int i = 0; i < infos.length - 1; i++) {
            TimelineInfo info1 = infos[i];
            TimelineInfo info2 = infos[i + 1];
            
            normalizer.setValue(info1.getTimeStamp());
            int x0 = (int) normalizer.getValueNormalized();

            normalizer.setValue(info2.getTimeStamp());
            int x1 = (int) normalizer.getValueNormalized();
            
            graphics.setColor(info1.getColor().getColor());
            graphics.fillRect(x0, 5, x1 - x0 + 1, 5);
        }
        
        normalizer.setValue(infos[infos.length - 1].getTimeStamp());
        int x0 = (int) normalizer.getValueNormalized();

        normalizer.setValue(infos[infos.length - 1].getTimeStamp() + 250);
        int x1 = (int) normalizer.getValueNormalized();

        graphics.setColor(lastColor);        
        graphics.fillRect(x0, 5, x1 - x0 + 1, 5);
        
        return lastColor;
    }
    
    private void drawBoldMarks(Graphics2D graphics, int currentValue, Rectangle bounds, int totalInc) {

        long round = range.getMin() % (10 * millsUnitIncrement);
        int shift = (int) (round / millsUnitIncrement) * totalInc;
        
        int lowerBound = bounds.x - (4 * totalInc);
        int x = ((bounds.x - currentValue) - shift);
        
        int increment = 0;
        int height = getHeight();

        graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
        int upperBound = (bounds.x + bounds.width);
        for (int i = x; i < upperBound; i += totalInc) {
            if (increment % 10 == 0 && (i >= lowerBound)) {
                graphics.drawLine(i, 0, i, height);
            }
            increment++;
        }
    }
    
    public LongRange getRange() {
        return range;
    }
    
    @Override
    public int getHeight() {
        return 40;
    }
    
    @Override
    public int getWidth() {
         
        long divisor = millsUnitIncrement / pixelUnitIncrement;
        
        long span = range.getMax() - range.getMin();
        int width = (int) (span / divisor);
        return width;
    }
    
    @Override
    public Dimension getSize() {
        return getPreferredSize();
    }
    
    @Override
    @Transient
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
}

