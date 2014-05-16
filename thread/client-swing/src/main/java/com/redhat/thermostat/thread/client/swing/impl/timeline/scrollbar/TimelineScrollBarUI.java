/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline.scrollbar;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.ui.Palette;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

class TimelineScrollBarUI extends BasicScrollBarUI {
    
    static enum ButtonDirection {
        INCREASE,
        DECREASE,
    }

    private UIDefaults uiDefaults;
    
    public TimelineScrollBarUI(UIDefaults uiDefaults) {
        this.uiDefaults = uiDefaults;
    }

    @SuppressWarnings("serial")
    private class TimelineButton extends JButton {
        
        public TimelineButton(ButtonDirection direction) {
            
            switch (direction) {
                case DECREASE:
                    setIcon(new FontAwesomeIcon('\uf104', 16, new Color(0, 0, 0, 100)));
                    break;

                case INCREASE:
                default:
                    setIcon(new FontAwesomeIcon('\uf105', 16, new Color(0, 0, 0, 100)));
                    break;
            }
            
            setOpaque(false);
            setBorder(new EmptyBorder(5, 5, 5, 5));

            // those should really go somewhere in TimelineUtils or common UI
            // properties, including the date format below
            setForeground(Palette.EARL_GRAY.getColor());
            setUI(new TimelineButtonUI());
        }
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        
        scrollBarWidth = 16;
        incrGap = 0;
        decrGap = 0;
        
        minimumThumbSize = new Dimension(50, 5);
    }
    
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return new TimelineButton(ButtonDirection.DECREASE);
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return new TimelineButton(ButtonDirection.INCREASE);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        graphics.setColor(Palette.GRAY.getColor());
        
        graphics.translate(trackBounds.x, trackBounds.y);
        
        int w = trackBounds.width - 1;
        int h = trackBounds.height - 1;
        
        graphics.drawLine(0, h, w, h);
        
        for (int i = 0; i < trackBounds.width; i += 10) {
            graphics.drawLine(i, h - 5, i, h);
            if (i % 100 == 0) {
                graphics.drawLine(i, 0, i, h);
            }
        }

        graphics.setColor(Palette.EARL_GRAY.getColor());
        graphics.drawLine(0, 0, 0, h);
        graphics.drawLine(w, 0, w, h);
        
        graphics.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if(thumbBounds.isEmpty() || !scrollbar.isEnabled())     {
            return;
        }

        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        
        int w = thumbBounds.width - 1;
        int h = thumbBounds.height - 1;

        graphics.translate(thumbBounds.x, thumbBounds.y);

        Color top = utils.deriveWithAlpha(Palette.WHITE.getColor(), 50);
        Color bottom = utils.deriveWithAlpha(Palette.DROID_GRAY.getColor(), 50);
        Paint gradient = new GradientPaint(0, 0, top, 0, h, bottom);
        
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, w, h);        
        
        graphics.setPaint(uiDefaults.getReferenceFieldIconColor());
        
        int y = h/2  + 1;
        
        graphics.drawLine(0, y, w, y);
        graphics.drawLine(0, 0, 0, h);
        graphics.drawLine(w, 0, w, h);
        
        graphics.dispose();
    }
}
