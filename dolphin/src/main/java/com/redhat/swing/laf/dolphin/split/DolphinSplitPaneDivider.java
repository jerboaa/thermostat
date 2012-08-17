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

package com.redhat.swing.laf.dolphin.split;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import com.redhat.swing.laf.dolphin.themes.DolphinTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

@SuppressWarnings("serial")
public class DolphinSplitPaneDivider extends BasicSplitPaneDivider {

    private static final int MARKER_SIZE = 28;
    
    private DolphinSplitPaneUI ui;
    
    public DolphinSplitPaneDivider(DolphinSplitPaneUI ui) {
        super(ui);
        this.ui = ui;
    }
    
    private void drawMarkers(Graphics2D graphics, Shape shape, int deltaX,
                             int deltaY)
    {
        for (int i = 0; i < 5; i++) {
            graphics.translate(deltaX, deltaY);
            graphics.draw(shape);
        }
    }
    
    @Override
    public void paint(Graphics g) {
     
        Graphics2D graphics = (Graphics2D) g.create();
        DolphinThemeUtils.setAntialiasing(graphics);

        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());
        
        Shape shape = new RoundRectangle2D.Double(0., 0., 1., 1., 4, 4);
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        graphics.setColor(theme.getSplitPaneDividerBorderColor());
        
        int x = 0;
        int y = 0;
        
        if (ui.getSplitPane().getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            x = getWidth()/2 - MARKER_SIZE/2;
            y = getHeight()/2;
            graphics.translate(x, y);
            drawMarkers(graphics, shape, 5, 0);
            
        } else {
            x = getWidth()/2;
            y = getHeight()/2 - MARKER_SIZE/2;
            graphics.translate(x, y);
            drawMarkers(graphics, shape, 0, 5);
        }
        
        graphics.dispose();
        
        super.paint(g);
    }
    
    @Override
    protected JButton createLeftOneTouchButton() {
        return super.createLeftOneTouchButton();
    }
    
    @Override
    protected JButton createRightOneTouchButton() {
        return super.createRightOneTouchButton();
    }
}
