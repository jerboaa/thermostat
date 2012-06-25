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

import javax.swing.UIDefaults;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;

import com.redhat.swing.laf.dolphin.button.DolphinButtonBorder;
import com.redhat.swing.laf.dolphin.text.DolphinTextAreaBorder;
import com.redhat.swing.laf.dolphin.text.DolphinTextBorder;

public class DolphinDefaultTheme extends DolphinTheme {

    private static final ColorUIResource WHITE = new ColorUIResource(Color.WHITE);
    
    private static final ColorUIResource WINDOW_BACKGROUND = new ColorUIResource(0xEDEDED);
    private static final ColorUIResource BUTTON_GRADIENT_TOP = new ColorUIResource(0xf1f3f1);
    private static final ColorUIResource BUTTON_GRADIENT_TOP_ROLLOVER = new ColorUIResource(0xfcfdfc);
    private static final ColorUIResource BUTTON_GRADIENT_BOTTOM = new ColorUIResource(0xdbdddb);
    private static final ColorUIResource BUTTON_GRADIENT_BOTTOM_ROLLOVER = new ColorUIResource(0xe6eae6);
    private static final ColorUIResource BUTTON_GRADIENT_PRESSED_TOP = new ColorUIResource(0xa6aca6);
    private static final ColorUIResource BUTTON_GRADIENT_PRESSED_BOTTOM = new ColorUIResource(0xed9dad9);
    private static final ColorUIResource BUTTON_GRADIENT_DISABLED_TOP = new ColorUIResource(0xf4f4f2);
    private static final ColorUIResource BUTTON_GRADIENT_DISABLED_BOTTOM = new ColorUIResource(0xf4f4f2);
    private static final ColorUIResource BUTTON_GRADIENT_FOCUS_TOP = new ColorUIResource(0xbcd5ef);
    private static final ColorUIResource BUTTON_GRADIENT_FOCUS_BOTTOM = new ColorUIResource(0x93b0d3);
    
    private static final ColorUIResource BORDER_GRADIENT_DEFAULT_TOP = new ColorUIResource(0xa7aba7);
    private static final ColorUIResource BORDER_GRADIENT_DEFAULT_BOTTOM = new ColorUIResource(0xa8aca8);
    private static final ColorUIResource BORDER_GRADIENT_DISABLED_TOP = new ColorUIResource(0xbabcb8);
    private static final ColorUIResource BORDER_GRADIENT_DISABLED_BOTTOM = new ColorUIResource(0xbabcb8);
    
    private static final ColorUIResource TEXT_DEFAULT_COLOR = new ColorUIResource(0x2e3436);
    
    private static final ColorUIResource TITLED_BORDER_BORDER_COLOR = new ColorUIResource(0xa8aca8);
    
    private static final ColorUIResource SELECTION_COLOR = new ColorUIResource(0x4A90D9);
    private static final ColorUIResource SELECTION_FOREGROUND = WHITE;
    
    private static final ColorUIResource TREE_HEADER_COLOR = new ColorUIResource(0x1A58AD);
    
    private static final ColorUIResource TAB_TOP_GRADIENT_COLOR = new ColorUIResource(0xF9F9F9);

    private static final ColorUIResource TAB_UNSELECTED_TOP_GRADIENT_COLOR = new ColorUIResource(0xe6e6e6);
    private static final ColorUIResource TAB_UNSELECTED_BOTTOM_GRADIENT_COLOR = new ColorUIResource(0xcbcbcb);

    private static final ColorUIResource THUMB_COLOR = new ColorUIResource(0x9B9D9E);
    private static final ColorUIResource TRACK_COLOR = new ColorUIResource(0xD6D6D6);
    private static final ColorUIResource THUMB_FOCUSED_COLOR = new ColorUIResource(0x828586);
    private static final ColorUIResource THUMB_MOVING_COLOR = SELECTION_COLOR;
    
    @Override
    public ColorUIResource getwindowBackgroundColor() {
        return WINDOW_BACKGROUND;
    }
    
    @Override
    public boolean borderPaintsFocus() {
        return false;
    }
    
    @Override
    public ColorUIResource getSystemTextColor() {
        return TEXT_DEFAULT_COLOR;
    }
    
    @Override
    public ColorUIResource getControlTextColor() {
        return getSystemTextColor();
    }
    
    @Override
    public ColorUIResource getButtonGradientTopColor() {
        return BUTTON_GRADIENT_TOP;
    }

    @Override
    public ColorUIResource getButtonGradientTopRolloverColor() {
        return BUTTON_GRADIENT_TOP_ROLLOVER;
    }

    @Override
    public ColorUIResource getButtonGradientBottomColor() {
        return BUTTON_GRADIENT_BOTTOM;
    }

    @Override
    public ColorUIResource getButtonGradientBottomRolloverColor() {
        return BUTTON_GRADIENT_BOTTOM_ROLLOVER;
    }

    @Override
    public ColorUIResource getButtonGradientPressedTopColor() {
        return BUTTON_GRADIENT_PRESSED_TOP;
    }

    @Override
    public ColorUIResource getButtonGradientPressedBottomColor() {
        return BUTTON_GRADIENT_PRESSED_BOTTOM;
    }
    
    @Override
    public ColorUIResource getButtonGradientDisabledTopColor() {
        return BUTTON_GRADIENT_DISABLED_TOP;
    }

    @Override
    public ColorUIResource getButtonGradientDisabledBottomColor() {
        return BUTTON_GRADIENT_DISABLED_BOTTOM;
    }
    
    @Override
    public ColorUIResource getBorderGradientTopColor() {
        return BORDER_GRADIENT_DEFAULT_TOP;
    }

    @Override
    public ColorUIResource getBorderGradientBottomColor() {
        return BORDER_GRADIENT_DEFAULT_BOTTOM;
    }

    @Override
    public ColorUIResource getBorderGradientTopDisabledColor() {
        return BORDER_GRADIENT_DISABLED_TOP;
    }

    @Override
    public ColorUIResource getBorderGradientBottomDisabledColor() {
        return BORDER_GRADIENT_DISABLED_BOTTOM;
    }

    @Override
    public ColorUIResource getButtonFocusGradientTopColor() {
        return BUTTON_GRADIENT_FOCUS_TOP;
    }

    @Override
    public ColorUIResource getButtonFocusGradientBottomColor() {
        return BUTTON_GRADIENT_FOCUS_BOTTOM;
    }

    @Override
    public ColorUIResource getTitledBorderBorderColor() {
        return TITLED_BORDER_BORDER_COLOR;
    }

    @Override
    public boolean getTreeRendererFillBackground() {
        return true;
    }
    
    @Override
    public boolean getTreeLineTypeDashed() {
        return false;
    }
    
    @Override
    public ColorUIResource getSelectionColor() {
        return SELECTION_COLOR;
    }
    
    @Override
    public ColorUIResource getSelectionColorForeground() {
        return SELECTION_FOREGROUND;
    }
    
    @Override
    public ColorUIResource getTreeHeaderColor() {
        return TREE_HEADER_COLOR;
    }
    
    @Override
    public AbstractBorder getButtonBorder() {
        return new DolphinButtonBorder();
    }
    
    @Override
    public AbstractBorder getTextFieldBorder() {
        return new DolphinTextBorder();
    }
    
    @Override
    public ColorUIResource getMenuBackgroundColor() {
        return SELECTION_FOREGROUND;
    }
    
    @Override
    public ColorUIResource getMenuForegroundColor() {
        return SELECTION_COLOR;
    }
    
    @Override
    public AbstractBorder getTextAreaBorder() {
        return new DolphinTextAreaBorder();
    }
    
    @Override
    public AbstractBorder getScrollPaneBorder() {
        return new DolphinTextAreaBorder();
    }
    
    @Override
    public ColorUIResource getTabAreaBackground() {
        return WHITE;
    }
    
    @Override
    public ColorUIResource getTabAreaForeground() {
        return TEXT_DEFAULT_COLOR;
    }
    
    @Override
    public ColorUIResource getTabBottomGradient() {
        return WHITE;
    }
    
    @Override
    public ColorUIResource getTabTopGradient() {
        return TAB_TOP_GRADIENT_COLOR;
    }
    
    @Override
    public ColorUIResource getUnselectedTabBottomGradient() {
        return TAB_UNSELECTED_BOTTOM_GRADIENT_COLOR;
    }
    
    @Override
    public ColorUIResource getUnselectedTabTopGradient() {
        return TAB_UNSELECTED_TOP_GRADIENT_COLOR;
    }
    
    @Override
    public Color getThumbColor() {
        return THUMB_COLOR;
    }
    
    @Override
    public Color getScrollBarTrackColor() {
        return TRACK_COLOR;
    }
    
    @Override
    public Color getThumbFocusedColor() {
        return THUMB_FOCUSED_COLOR;
    }
    
    @Override
    public Color getThumbMovingColor() {
        return THUMB_MOVING_COLOR;
    }
    
    @Override
    public Color getSplitPaneDividerBorderColor() {
        return BORDER_GRADIENT_DEFAULT_TOP;
    }
    
    @Override
    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);
        
        Object[] uiDefaults = {
//            "MenuBarUI",    "com.ladybug.swing.plaf.icedlook.menu.IcedLookMenuBarUI",
               "TabbedPane.contentAreaColor", getTabAreaBackground(),
               "TabbedPane.tabAreaBackground", getTabAreaBackground(),
               "TabbedPane.selectHighlight", getSelectionColor(),
               "TabbedPane.selected", getSelectionColor(),
               "TabbedPane.focus", getSelectionColor(),
               //"TabbedPane.light", getTabAreaBackground(),
               "TabbedPane.background", getTabAreaBackground(),
               "TabbedPane.foreground", getTabAreaForeground(),
        };
        
        table.putDefaults(uiDefaults);        
    }

}
