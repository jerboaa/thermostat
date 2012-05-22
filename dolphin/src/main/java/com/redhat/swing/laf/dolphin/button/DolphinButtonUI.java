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

package com.redhat.swing.laf.dolphin.button;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

import com.redhat.swing.laf.dolphin.themes.DolphinTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

public class DolphinButtonUI extends MetalButtonUI {

    private static final DolphinButtonUI singleton = new DolphinButtonUI();
    private boolean canPaintFocus;
    
    public static ComponentUI createUI(JComponent b) {
        return singleton;
    }

    @Override
    public void installDefaults(AbstractButton b) {
    
        super.installDefaults(b);
        canPaintFocus = !UIManager.getBoolean("Button.borderPaintsFocus");
    }
    
    @Override
    public void update(Graphics g, JComponent c) {
        
        paint(g, c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        
        Graphics2D graphics = (Graphics2D) g.create();
        
        graphics.clearRect(0, 0, c.getWidth(), c.getHeight());
        DolphinThemeUtils.setAntialiasing(graphics);

        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        
        Color topGradient = null;
        Color bottomGradient = null;
        if (!c.isEnabled()) {
            topGradient = theme.getButtonGradientDisabledTopColor();
            bottomGradient = theme.getButtonGradientDisabledBottomColor();
        } else {
            if (model.isRollover()) {
                topGradient = theme.getButtonGradientTopRolloverColor();
                bottomGradient = theme.getButtonGradientBottomRolloverColor();
            } else {
                topGradient = theme.getButtonGradientTopColor();
                bottomGradient = theme.getButtonGradientBottomColor();
            }
        }
        
        Paint paint = new GradientPaint(0, 0, topGradient, 0, c.getHeight(),
                                        bottomGradient);
        graphics.setPaint(paint);

        Shape shape = DolphinThemeUtils.getRoundShape(c.getWidth(), c.getHeight());
        
        graphics.fill(shape);
        graphics.dispose();
        
        super.paint(g, c);        
    }
    
    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {    

        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();

        Graphics2D graphics = (Graphics2D) g.create();
        
        Paint paint =
            new GradientPaint(0, 0, theme.getButtonGradientPressedTopColor(),
                              0, b.getHeight(),
                              theme.getButtonGradientPressedBottomColor());
        
        graphics.setPaint(paint);

        Shape shape = DolphinThemeUtils.getRoundShape(b.getWidth(), b.getHeight());
        graphics.fill(shape);

        graphics.dispose();
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                              Rectangle textRect, Rectangle iconRect) {
        
        if (!canPaintFocus)
            return;
        
        DolphinTheme theme = DolphinThemeUtils.getCurrentTheme();
        
        Graphics2D graphics = (Graphics2D) g.create();
        DolphinThemeUtils.setAntialiasing(graphics);
        
        graphics.translate(viewRect.x - 1, viewRect.y -1);
        graphics.setStroke(new BasicStroke(1));
  
        ColorUIResource topGradient = theme.getButtonFocusGradientTopColor();
        ColorUIResource bottomGradient = theme.getButtonFocusGradientBottomColor();

        Paint paint = new GradientPaint(0, 0, topGradient, 0, viewRect.height,
                                        bottomGradient);
        graphics.setPaint(paint);

        Shape shape = DolphinThemeUtils.getRoundShape(viewRect.width + 2, viewRect.height + 2);

        graphics.draw(shape);
        graphics.dispose();
    }
}
