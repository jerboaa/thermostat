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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.redhat.swing.laf.dolphin.borders.DolphinDebugBorder;
import com.redhat.swing.laf.dolphin.themes.DolphinTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

public class DolphinSplitPaneDividerBorder extends DolphinDebugBorder {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width,
                            int height) {

        if (!(c instanceof BasicSplitPaneDivider)) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        DolphinThemeUtils.setAntialiasing(graphics);

        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
                
        BasicSplitPaneUI splitPane =
                ((BasicSplitPaneDivider)c).getBasicSplitPaneUI();
        if (splitPane.getOrientation() == JScrollBar.HORIZONTAL) {
            graphics.translate(x, height - 1);
            
            LinearGradientPaint paint =
                    new LinearGradientPaint(new Point2D.Float(0, 0),
                                            new Point2D.Float(width, 0),
                                            new float[] { 0.0f, 0.2f, 0.8f, 1.0f },
                                            new Color[] { c.getBackground(),
                                                          theme.getSplitPaneDividerBorderColor(),
                                                          theme.getSplitPaneDividerBorderColor(),
                                                          c.getBackground()});
            
            graphics.setPaint(paint);
            graphics.drawLine(0, 0, width, 0);

        } else {
            
            LinearGradientPaint paint =
                    new LinearGradientPaint(new Point2D.Float(0, 0),
                                            new Point2D.Float(0, height),
                                            new float[] { 0.0f, 0.2f, 0.8f, 1.0f },
                                            new Color[] { c.getBackground(),
                                                          theme.getSplitPaneDividerBorderColor(),
                                                          theme.getSplitPaneDividerBorderColor(),
                                                          c.getBackground()});
            
            graphics.setPaint(paint);
            graphics.translate(width - 1, y);
            graphics.drawLine(0, 0, 0, height);
        }
        
        graphics.dispose();
    }
}
