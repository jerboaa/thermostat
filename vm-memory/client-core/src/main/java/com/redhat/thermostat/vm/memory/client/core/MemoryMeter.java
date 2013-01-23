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

package com.redhat.thermostat.vm.memory.client.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.Transient;

import javax.swing.JComponent;
import javax.swing.plaf.ColorUIResource;

import sun.swing.SwingUtilities2;

@SuppressWarnings({ "restriction", "serial" })
public class MemoryMeter extends JComponent {
    
    // TODO the font should be customizable
    private static final Font font = new Font("SansSerif", Font.PLAIN, 10);
    
    private static final int TICK_NUM = 100;
    private static final int SMALL_TICK_NUM = 5;
    
    private static final int MAIN_BAR_HEIGHT = 20;
    
    private static final ColorUIResource MAIN_GRADIENT_TOP = new ColorUIResource(0xf1f3f1);
    private static final ColorUIResource MAIN_BORDER_COLOR = new ColorUIResource(0xa8aca8);
    
    private static final ColorUIResource MAIN_BAR_BASE_COLOR_TOP = new ColorUIResource(0xbcd5ef);
    private static final ColorUIResource MAIN_BAR_BASE_COLOR = new ColorUIResource(0x4A90D9);
    
    //private static final ColorUIResource STATS_BG = new ColorUIResource(0xF8F8F8);
    private static final ColorUIResource STATS_BG = new ColorUIResource(0xFFFFFF);
    
    private ColorUIResource tickColor;
    
    private RangeModel primary;
    private RangeModel secondary;
    
    private Insets boundInsets;
    
    private RangeModel internalSecondaryModel;
    
    private StatsModel primaryStats;
    
    private String primaryUnit;
    private String secondaryUnit;

        public void setPrimaryScaleUnit(String primaryUnit) {
        this.primaryUnit = primaryUnit;
    }
    
    public void setSecondayScaleUnit(String secondaryUnit) {
        this.secondaryUnit = secondaryUnit;
    }
    
    public StatsModel getStats() {
        return primaryStats;
    }
    
    public void setStats(StatsModel primaryStats) {
        this.primaryStats = primaryStats;
    }

    public MemoryMeter() {
        
        secondaryUnit = "";
        primaryUnit = "";
        
        boundInsets = new Insets(10, 10, 20, 20);
        
        tickColor = new ColorUIResource(0xdbdddb);
        
        primary = new RangeModel();
        primary.setMinimum(0);
        primary.setMaximum(100);
        primary.setMinNormalized(0);
        primary.setMaxNormalized(100);
        
        secondary = new RangeModel();
        secondary.setMinimum(0);
        secondary.setMaximum(100);
        secondary.setMinNormalized(0);
        secondary.setMaxNormalized(100);
        
        internalSecondaryModel = new RangeModel();
    }
    
    public ColorUIResource getTickColor() {
        return tickColor;
    }
    
    public void setTickColor(ColorUIResource tickColor) {
        this.tickColor = tickColor;
    }
    
    public RangeModel getPrimaryModel() {
        return primary;
    }
    
    public RangeModel getSecondaryModel() {
        return secondary;
    }
    
    protected Rectangle getOuterBounds() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }
    
    protected Rectangle getBoundsWithInsets(Rectangle bounds) {
        return new Rectangle(bounds.x + boundInsets.left,
                             bounds.y + boundInsets.top,
                             bounds.width  - boundInsets.right,
                             bounds.height - boundInsets.bottom);
    }
        
    /**
     * paint the outher frame, including the light border sorrounding
     */
    protected void paintOuterFrame(Graphics2D graphics, Rectangle bounds) {

        RoundRectangle2D frame = new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);

        Paint paint = new GradientPaint(0, 0, MAIN_GRADIENT_TOP, 0, getHeight(), getBackground());
        graphics.setPaint(paint);
        graphics.fill(frame);
        
        paint = new GradientPaint(0, 0, MAIN_BORDER_COLOR, 0, getHeight(), getBackground());
        graphics.setPaint(paint);
        frame = new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width -1, bounds.height, 6, 6);
        graphics.draw(frame);
    }
    
    /**
     * paint the track sorrounding the main bar
     */
    protected void paintMainBarTrackFill(Graphics2D graphics, Rectangle bounds) {
        
        Paint paint = new GradientPaint(0, 0, MAIN_GRADIENT_TOP, 0, bounds.height, Color.WHITE);
        graphics.setPaint(paint);
        RoundRectangle2D frame = new RoundRectangle2D.Float(0, 0, bounds.width, bounds.height, 6, 6);
        graphics.fill(frame);
    }
    
    /**
     */
    protected void paintMainBarTrackBorder(Graphics2D graphics, Rectangle bounds) {
        Paint paint = new GradientPaint(0, 0, MAIN_BORDER_COLOR, getWidth(), 0, getBackground());
        graphics.setPaint(paint);
        
        RoundRectangle2D frame = new RoundRectangle2D.Float(0, 0, bounds.width - 1, bounds.height, 6, 6);
        graphics.draw(frame);
    }

    
    /**
     * this is the main bar, will it up to what is defined by the model
     */
    protected void paintMainBarFill(Graphics2D graphics, Rectangle bounds) {
        Paint paint = new GradientPaint(0, 0, MAIN_BAR_BASE_COLOR, getWidth() * 2, 0, getBackground());
        graphics.setPaint(paint);
        
        RoundRectangle2D frame =
                new RoundRectangle2D.Float(0, 0, getPrimaryModel().getValueNormalized(),
                                           bounds.height, 6, 6);
        graphics.fill(frame);
        
        String value = String.valueOf(getPrimaryModel().getValue()) + " " + primaryUnit;
        Rectangle2D fontBounds = font.getStringBounds(value, graphics.getFontRenderContext());
        int width = (int) (bounds.width/2 - fontBounds.getWidth()/2) - 1;
        
        if (width > getPrimaryModel().getValueNormalized()) {
            graphics.setPaint(MAIN_BAR_BASE_COLOR);
        } else {
            graphics.setPaint(getBackground());
        }

        int height  = (int) (bounds.height / 2 + fontBounds.getHeight()/2);
        SwingUtilities2.drawString(this, graphics, value, width, height);
    }
    
    /**
     */
    private void paintMainBar(Graphics2D g, Rectangle bounds) {
        
        Graphics2D graphics = (Graphics2D) g.create();
                
        graphics.translate(bounds.x, bounds.y);
        paintMainBarTrackFill(graphics, bounds);
        
        paintMainBarFill(graphics, bounds);
        
        paintMainBarTrackBorder(graphics, bounds);
        graphics.dispose();
    }
    
    /**
     */
    protected void drawBottomBar(Graphics2D g, Rectangle bounds) {

        Graphics2D graphics = (Graphics2D) g.create();
        graphics.translate(bounds.x, bounds.y + bounds.height);
        
        drawTickMark(graphics, bounds);
        
        paintSecondaryBarFill(graphics, bounds);
        
        graphics.dispose();
    }
    
    /**
     */
    protected void drawTickMark(Graphics2D graphics, Rectangle bounds) {
      
        graphics.setPaint(MAIN_BORDER_COLOR);
        
        int smallTop = bounds.height - 5;
        int smallBottom = bounds.height;

        int mainTop = smallTop + 20;
        int mainBottom = smallBottom - 15;
      
        // the first and last tick are always big, this is the first
        graphics.drawLine(0, mainTop, 0, mainBottom);
      
        // the space between the vertical lines
        double tickSpace = ((double) bounds.width) / TICK_NUM;
      
        internalSecondaryModel.setMaxNormalized(bounds.width);      
        int numTicks = 0;

        for (int x = 0; x < bounds.width; x += tickSpace + 0.5) {
            if (numTicks % SMALL_TICK_NUM == 0) {
              
                graphics.drawLine(x, smallTop, x, smallBottom + 5);

                internalSecondaryModel.setValue(x);
            } else {
                graphics.drawLine(x, smallTop, x, smallBottom);
            }
            numTicks++;
        }
        
        // that's the last
        graphics.drawLine(bounds.width, mainTop, bounds.width, mainBottom);
        graphics.drawLine(0, smallTop, bounds.width, smallTop);
        
        drawStrings(graphics, mainBottom, mainTop, bounds.width);
    }
    
    protected void drawStrings(Graphics2D graphics, int top, int bottom, int right) {
      
      // now draw the min/max values of both side of markers      
      // top bar min value
      FontMetrics fm = SwingUtilities2.getFontMetrics(this, font);
      
      String value = String.valueOf(getPrimaryModel().getMinimum()) + " " + primaryUnit;
      int height = top + fm.getAscent()/2;
      SwingUtilities2.drawString(this, graphics, value, 1, height);
      
      value = String.valueOf(getSecondaryModel().getMinimum()) + " " + secondaryUnit;
      height = bottom;
      SwingUtilities2.drawString(this, graphics, value, 1, height);
      
      value = String.valueOf(getPrimaryModel().getMaximum()) + " " + primaryUnit;
      height = top + fm.getAscent()/2;

      int width = (int) (right - font.getStringBounds(value, graphics.getFontRenderContext()).getWidth()) - 1;
      SwingUtilities2.drawString(this, graphics, value, width, height);
      
      value = String.valueOf(getSecondaryModel().getMaximum()) + " " + secondaryUnit;
      height = bottom;
      width = (int) (right - font.getStringBounds(value, graphics.getFontRenderContext()).getWidth()) - 1;
      SwingUtilities2.drawString(this, graphics, value, width, height);
      
      // now draw the actual value for the bottom bar, the top bar is drawn in
      // its fill method
      value = String.valueOf(getSecondaryModel().getValue()) + " " + secondaryUnit;
      width = right/2;
      Rectangle2D bounds = font.getStringBounds(value, graphics.getFontRenderContext());
      width = (int) (width - bounds.getWidth()/2) - 1;
      SwingUtilities2.drawString(this, graphics, value, width, height);
      RoundRectangle2D frame = new RoundRectangle2D.Double(width - 2, height - bounds.getHeight(),
                                                          bounds.getWidth() + 4, bounds.getHeight() + 4,
                                                          4, 4);
      graphics.draw(frame);
    }
    
    protected void paintSecondaryBarFill(Graphics2D graphics, Rectangle bounds) {
        
        graphics.setPaint(MAIN_BAR_BASE_COLOR_TOP);
        graphics.drawLine(1, bounds.height, getSecondaryModel().getValueNormalized() - 1, bounds.height);
        
        graphics.setPaint(MAIN_BAR_BASE_COLOR);
        graphics.fillRect(1, bounds.height + 1, getSecondaryModel().getValueNormalized(), 2);
    }
    
    protected void paintStats(Graphics2D graphics, Rectangle bounds) {
        
        int imageWidth = bounds.width - 2;
        if (imageWidth < 0 || bounds.height < 0) {
            return;
        }
        
        StatsModel stats = getStats();
        drawStats(graphics, stats, bounds.x, bounds.y, imageWidth, bounds.height);
    }
    
    private void drawStats(Graphics2D graphics, StatsModel stats, int x, int y, int imageWidth, int height) {
        if (stats != null) {
            BufferedImage image = stats.getChart(imageWidth, height, STATS_BG,
                                                 new ColorUIResource(getForeground()));
       
            paintStatsLabel(graphics, image, stats.getName());
            graphics.drawImage(image, x, y, null);
        }
    }
    
    protected void paintStatsLabel(Graphics2D graphics, BufferedImage image, String label) {
        int height = SwingUtilities2.getFontMetrics(this, font).getAscent() + 2;
        if (height <= 0) {
            return;
        }
        
        Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
        imageGraphics.setColor(getForeground());
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        SwingUtilities2.drawString(this, imageGraphics, label, 2, height);
    }
    
    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setFont(font);
        
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        Rectangle outerBounds = getOuterBounds();
        Rectangle innerBounds = getBoundsWithInsets(outerBounds);
        Rectangle statsBound =  getBoundsWithInsets(outerBounds);
        
        // move the bar close to the center
        innerBounds.height = MAIN_BAR_HEIGHT;
        innerBounds.y = outerBounds.height/2;
        
        // make the stats area cover the upper portion instead
        statsBound.height = (outerBounds.height/2) - MAIN_BAR_HEIGHT;
        
        resetModels(0, innerBounds.width);

        // some eye candy
        paintOuterFrame(graphics, outerBounds);

        // paint the usage stats
        paintStats(graphics, statsBound);
        
        // main and bottom bars
        paintMainBar(graphics, innerBounds);
        drawBottomBar(graphics, innerBounds);
        
        graphics.dispose();
    }
    
    private void resetModels(int min, int max) {
        
        getPrimaryModel().setMaxNormalized(max);
        getPrimaryModel().setMinNormalized(min);
        
        RangeModel model = getSecondaryModel();
        model.setMaxNormalized(max);
        model.setMinNormalized(min);
        
        internalSecondaryModel.setMaximum(model.getMaximum());
        internalSecondaryModel.setMinimum(model.getMinimum());
        internalSecondaryModel.setValue(model.getValue());
        
        internalSecondaryModel.setMaxNormalized(max);
        internalSecondaryModel.setMinNormalized(0);
    }
    
    @Override
    @Transient
    public Dimension getPreferredSize() {
        return new Dimension(850, 150);
    }
}

