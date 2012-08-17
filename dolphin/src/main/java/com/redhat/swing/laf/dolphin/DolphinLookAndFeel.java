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

package com.redhat.swing.laf.dolphin;

import java.awt.Color;

import javax.swing.UIDefaults;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.redhat.swing.laf.dolphin.icons.DolphinAscendingArrowIcon;
import com.redhat.swing.laf.dolphin.icons.DolphinCheckBoxIcon;
import com.redhat.swing.laf.dolphin.icons.DolphinDescendingArrowIcon;
import com.redhat.swing.laf.dolphin.icons.DolphinRadioIcon;
import com.redhat.swing.laf.dolphin.split.DolphinSplitPaneDividerBorder;
import com.redhat.swing.laf.dolphin.themes.DolphinDefaultTheme;
import com.redhat.swing.laf.dolphin.themes.DolphinTheme;

public class DolphinLookAndFeel extends MetalLookAndFeel {

    private static final DolphinTheme theme = new DolphinDefaultTheme();
    static {
        MetalLookAndFeel.setCurrentTheme(theme);
    }
    
    public static DolphinTheme getTheme() {
        return theme;
    }
    
    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        
        Object[] uiDefaults = {
            "swing.boldMetal", Boolean.FALSE,
            
            "ButtonUI",     "com.redhat.swing.laf.dolphin.button.DolphinButtonUI",
            "ToggleButtonUI",   "com.redhat.swing.laf.dolphin.button.DolphinToggleButtonUI",
            "CheckBoxUI",   "com.redhat.swing.laf.dolphin.button.DolphinCheckBoxUI",
            "TreeUI",       "com.redhat.swing.laf.dolphin.tree.DolphinTreeUI",
            "TextFieldUI",  "com.redhat.swing.laf.dolphin.text.DolphinTextFieldUI",
            "PasswordFieldUI", "com.redhat.swing.laf.dolphin.text.DolphinPasswordFieldUI",
            "TabbedPaneUI", "com.redhat.swing.laf.dolphin.tab.DolphinTabbedPaneUI",
            "ScrollBarUI",  "com.redhat.swing.laf.dolphin.scrollbars.DolphinScrollBarUI",
            "SplitPaneUI",  "com.redhat.swing.laf.dolphin.split.DolphinSplitPaneUI",
            "SeparatorUI",  "com.redhat.swing.laf.dolphin.split.DolphinSeparatorUI",
            "PopupMenuSeparatorUI", "com.redhat.swing.laf.dolphin.split.DolphinSeparatorUI",
            "TableUI",      "com.redhat.swing.laf.dolphin.table.DolphinTableUI",
            "TableUI",      "com.redhat.swing.laf.dolphin.table.DolphinTableUI",
            "TableHeaderUI", "com.redhat.swing.laf.dolphin.table.DolphinTableHeaderUI",
            
            // menu ui defined in theme specific "addCustomEntriesToTable"
        };
        
        table.putDefaults(uiDefaults);
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        
        Object[] uiDefaults = {

                "window", theme.getWindowBackgroundColor(),
                "Panel.background", theme.getWindowBackgroundColor(),
                
                "text", theme.getSystemTextColor(),
                "textHighlight", theme.getSelectionColor(),
                "textHighlightText", theme.getSelectionColor(),
                
                "TitledBorder.border", theme.getTitledBorderBorderColor(),

                "Button.focus", theme.getSelectionColor(),
                "Button.border", theme.getButtonBorder(),
                "Button.borderPaintsFocus", theme.borderPaintsFocus(),
                "Button.invertedForeground", theme.getSelectionColorForeground(),

                "ToggleButton.focus", theme.getSelectionColor(),
                "ToggleButton.border", theme.getButtonBorder(),
                "ToggleButton.borderPaintsFocus", theme.borderPaintsFocus(),
                "ToggleButton.invertedForeground", theme.getSelectionColorForeground(),
                "ToggleButton.rollover", Boolean.TRUE,

                "CheckBox.icon", new IconUIResource(new DolphinCheckBoxIcon()),

                "RadioButton.icon", new IconUIResource(new DolphinRadioIcon()),

                "Tree.repaintWholeRow", Boolean.TRUE,
                "Tree.lineTypeDashed", theme.getTreeLineTypeDashed(),
                "Tree.rendererFillBackground", theme.getTreeRendererFillBackground(),
                "Tree.selectionForeground", theme.getSelectionColorForeground(),
                "Tree.selectionBackground", theme.getSelectionColor(),
                
                "TextField.border", theme.getTextFieldBorder(),
                "TextField.caretBlinkRate", 0,
                "TextField.selectionBackground", theme.getSelectionColor(),
                "TextField.selectionForeground", theme.getSelectionColorForeground(),

                "PasswordField.border", theme.getTextFieldBorder(),
                "PasswordField.caretBlinkRate", 0,
                "PasswordField.selectionBackground", theme.getSelectionColor(),
                "PasswordField.selectionForeground", theme.getSelectionColorForeground(),
                
                "TextArea.border", theme.getTextAreaBorder(),
                "TextArea.selectionBackground", theme.getSelectionColor(),
                "TextArea.selectionForeground", theme.getSelectionColorForeground(),
                "TextArea.caretBlinkRate",  0,
                
                "ScrollPane.border", theme.getScrollPaneBorder(),
                
                "List.selectionBackground", theme.getSelectionColor(),
                "List.selectionForeground", theme.getSelectionColorForeground(),        
                
                "ScrollBar.width", 15,
                "SplitPane.dividerSize", new Integer(7),
                "SplitPaneDivider.border", new DolphinSplitPaneDividerBorder(),
                "SplitPane.border", null,
                "SplitPane.oneTouchButtonSize", theme.getOneTouchButtonSize(),
                "SplitPane.oneTouchButtonOffset", theme.getOneTouchButtonOffset(),
                
                "TableHeader.cellBorder", theme.getTableCellBorder(),
                "TableHeader.focusCellBorder", theme.getTableFocusCellBorder(),
                "TableHeader.background", theme.getTableHeaderBackgroundColor(),
                "TableHeader.foreground", theme.getTableHeaderForegroundColor(),
                "TableHeader.font", theme.getTableHeaderFont(),
                "Table.ascendingSortIcon", new IconUIResource(new DolphinAscendingArrowIcon()),
                "Table.descendingSortIcon", new IconUIResource(new DolphinDescendingArrowIcon()),
                "TableHeader.rightAlignSortArrow", true,
        };
        
        table.putDefaults(uiDefaults);  
    }

    @Override
    public String getID() {
        return "Dolphin Look And Feel";
    }

    @Override
    public String getDescription() {
        return "Dolphin Look And Feel";
    }

    @Override
    public String getName() {
        return getID();
    }
}
