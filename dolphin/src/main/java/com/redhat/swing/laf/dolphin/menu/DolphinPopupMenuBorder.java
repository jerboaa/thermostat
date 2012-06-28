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

package com.redhat.swing.laf.dolphin.menu;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JToolBar;
import javax.swing.text.JTextComponent;

import com.redhat.swing.laf.dolphin.borders.DolphinDebugBorder;
import com.redhat.swing.laf.dolphin.themes.DolphinThemeUtils;

public class DolphinPopupMenuBorder extends DolphinDebugBorder {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height)
    {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.translate(x, y);
        
        graphics.setPaint(DolphinThemeUtils.getCurrentTheme().getMenuBorderDefaultColor());

        graphics.drawLine(0, 0, 0, height);
        graphics.drawLine(0, height - 1, width - 1, height - 1);
        graphics.drawLine(width -1, height - 1, width - 1, 0);
        graphics.drawLine(width -1, 0, 0, 0);
        
        graphics.dispose();
    }
    
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {

        insets.top = 1;
        insets.left = 1;
        insets.right = 1;
        insets.bottom = 1;
        
        return insets;
    }
}
