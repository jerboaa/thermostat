/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal.vmlist;

import java.awt.Color;

import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.UIManager;

import com.redhat.thermostat.client.ui.Palette;

public class UIDefaultsImpl implements com.redhat.thermostat.client.swing.UIDefaults {
    
    private static final UIDefaultsImpl palette = new UIDefaultsImpl();
    public static UIDefaultsImpl getInstance() {
        return palette;
    }

    @Override
    public Font getDefaultFont() {
        Font font = (Font) UIManager.get("thermostat-default-font");
        return font != null ? font : new JLabel().getFont();
    }

    public Color getSelectedComponentFGColor() {
        Color color = (Color) UIManager.get("thermostat-selection-fg-color");
        return color != null ? color : Palette.LIGHT_GRAY.getColor();        
    }

    public Color getComponentFGColor() {
        Color color = (Color) UIManager.get("thermostat-fg-color");
        return color != null ? color : Palette.DROID_BLACK.getColor();            
    }

    public Color getSelectedComponentBGColor() {
        Color color = (Color) UIManager.get("thermostat-selection-bg-color");
        return color != null ? color : Palette.THERMOSTAT_BLU.getColor();
    }

    public Color getComponentSecondaryFGColor() {
        return Palette.DARK_GRAY.getColor();
    }

    @Override
    public int getIconSize() {
        return 16;
    }
    
    @Override
    public Color getComponentBGColor() {
        Color color = (Color) UIManager.get("thermostat-bg-color");
        return color != null ? color : Palette.LIGHT_GRAY.getColor();
    }

    @Override
    public int getReferenceFieldDefaultIconSize() {
        return 32;
    }

    @Override
    public int getIconDecorationSize() {
        return 12;
    }
    
    @Override
    public Color getDecorationIconColor() {
        return Palette.THERMOSTAT_RED.getColor();
    }
    
    @Override
    public Color getIconColor() {
        return Palette.EARL_GRAY.getColor();
    }
    
    @Override
    public Color getReferenceFieldIconColor() {
        return getSelectedComponentBGColor();
    }
    
    @Override
    public Color getReferenceFieldIconSelectedColor() {
        return getSelectedComponentFGColor();
    }
}

