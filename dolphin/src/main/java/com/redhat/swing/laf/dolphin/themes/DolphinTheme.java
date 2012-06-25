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

package com.redhat.swing.laf.dolphin.themes;

import java.awt.Color;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.OceanTheme;

public abstract class DolphinTheme extends OceanTheme {

    abstract public boolean borderPaintsFocus();
    
    abstract public ColorUIResource getButtonGradientTopColor();
    abstract public ColorUIResource getButtonGradientBottomColor();
    abstract public ColorUIResource getButtonGradientTopRolloverColor(); 
    abstract public ColorUIResource getButtonGradientBottomRolloverColor();
    abstract public ColorUIResource getButtonGradientPressedTopColor();
    abstract public ColorUIResource getButtonGradientPressedBottomColor();
    abstract public ColorUIResource getButtonGradientDisabledTopColor();
    abstract public ColorUIResource getButtonGradientDisabledBottomColor();
    abstract public ColorUIResource getButtonFocusGradientTopColor();
    abstract public ColorUIResource getButtonFocusGradientBottomColor();
        
    abstract public ColorUIResource getBorderGradientTopColor();
    abstract public ColorUIResource getBorderGradientBottomColor();
    abstract public ColorUIResource getBorderGradientTopDisabledColor();
    abstract public ColorUIResource getBorderGradientBottomDisabledColor();
    
    abstract public ColorUIResource getTitledBorderBorderColor();
    
    abstract public boolean getTreeRendererFillBackground();
    abstract public boolean getTreeLineTypeDashed();
    
    abstract public ColorUIResource getSelectionColor();
    abstract public ColorUIResource getSelectionColorForeground();
    abstract public ColorUIResource getTreeHeaderColor();
    
    abstract public AbstractBorder getTextAreaBorder();
    abstract public AbstractBorder getTextFieldBorder();
    abstract public AbstractBorder getButtonBorder();
    public abstract AbstractBorder getScrollPaneBorder();
    
    public boolean treePaintWholeRow() {
        return !Boolean.getBoolean("dolphin.tree.nofill");
    }
    
    public void setTreePaintWholeRow(boolean value) {
        System.setProperty("dolphin.tree.nofill", String.valueOf(value));
    }

    public boolean getTabPaneKeepBackgroundColor() {
        return Boolean.getBoolean("dolphin.tab.keepbg");
    }
    
    public void setTabPaneKeepBackgroundColor(boolean value) {
        System.setProperty("dolphin.tab.keepbg", String.valueOf(value));
    }
    
    public abstract ColorUIResource getwindowBackgroundColor();
    
    public abstract ColorUIResource getMenuBackgroundColor();
    public abstract ColorUIResource getMenuForegroundColor();
    public abstract ColorUIResource getTabAreaBackground();
    public abstract ColorUIResource getTabAreaForeground();

    public abstract ColorUIResource getTabTopGradient();
    public abstract ColorUIResource getTabBottomGradient();
    
    public abstract ColorUIResource getUnselectedTabTopGradient();
    public abstract ColorUIResource getUnselectedTabBottomGradient();

    abstract public Color getThumbColor();

    abstract public Color getScrollBarTrackColor();

    abstract public Color getThumbFocusedColor();
    abstract public Color getThumbMovingColor();
    
    abstract public Color getSplitPaneDividerBorderColor();
}
