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

package com.redhat.thermostat.client.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;

import sun.swing.SwingUtilities2;

@SuppressWarnings("restriction")
public class GraphicsUtils {

    private static GraphicsUtils instance = new GraphicsUtils();
    public static GraphicsUtils getInstance() {
        return instance;
    }
    
    public Graphics2D createAAGraphics(Graphics g) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return graphics;
    }
    
    public void drawStringWithShadow(JComponent component, Graphics2D graphics, String string, Color foreground, int x, int y) {
        // paint it twice to give a subtle drop shadow effect
        
        graphics.setColor(new Color(0f, 0f, 0f, 0.1f));
        SwingUtilities2.drawString(component, graphics, string, x, y + 1);
        
        graphics.setColor(foreground);
        SwingUtilities2.drawString(component, graphics, string, x, y);
    }
    
    public void drawString(JComponent component, Graphics2D graphics, String string, Color foreground, int x, int y) {
        graphics.setColor(foreground);
        SwingUtilities2.drawString(component, graphics, string, x, y);
    }
    
    public FontMetrics getFontMetrics(JComponent component, Font font) {
        return SwingUtilities2.getFontMetrics(component, font);
    }
    
    public Shape getRoundShape(int width, int height) {
        return new RoundRectangle2D.Double(0, 0, width, height, 4, 4);
    }
    
    public void setGradientPaint(Graphics2D g, int x, int height, Color start, Color stop) {
        Paint paint = new GradientPaint(x, 0, start, 0, height, stop);
        g.setPaint(paint);
    }
}
