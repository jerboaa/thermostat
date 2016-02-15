/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.internal.utils.laf.gtk;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.swing.JLabel;
import javax.swing.UIManager;

import com.redhat.thermostat.shared.config.NativeLibraryResolver;

public class GTKThemeUtils {

    private static boolean nativeLoaded;
    private static boolean initialized;
    static {
        try {
            String lib = NativeLibraryResolver.getAbsoluteLibraryPath("GTKThemeUtils");
            System.load(lib);
            nativeLoaded = true;
        
        } catch (UnsatisfiedLinkError ignore) {
            nativeLoaded = false;
        }
    }
    
    native private static boolean init();
    native private static boolean hasColor(String id);
    native private static int getColor(String id);
    native private static String getDefaultFont();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Color getColor(String name, float hOffset, float sOffset,
                                 float bOffset, int aOffset) throws Exception
    {    
        Class derivedColorClass = Class.forName("javax.swing.plaf.nimbus.DerivedColor");
        Constructor constructor = derivedColorClass.getDeclaredConstructor(new Class[] { 
                String.class, float.class, float.class, float.class, int.class
        });
        
        constructor.setAccessible(true);
        
        Color color = (Color) constructor.newInstance(new Object[] {
                name, hOffset, sOffset, bOffset, aOffset
        });
        
        return color;
    }
    
    public static void deriveColor(Color color) throws Exception {
        Method rederiveColorMethod = color.getClass().getMethod("rederiveColor");
        rederiveColorMethod.setAccessible(true);
        rederiveColorMethod.invoke(color);
    }

    private Color deriveColor(String colorID, Color defaultColor, float bOffset) {

        Color result = defaultColor;
        
        int bgColor = getColor(colorID);
        Color bg = new Color(bgColor);
            
        float hOffset = 0.0f;
        float sOffset = 0.0f;
        int aOffset = 0;
            
        UIManager.put("gtk-color", bg);
        try {
            Color derivedColor = getColor("gtk-color", hOffset, sOffset, bOffset, aOffset);
            deriveColor(derivedColor);
            result = new Color(derivedColor.getRGB());
                
        } catch (Exception ignore) {}
        
        return result;
    }
    
    public void setNimbusColoursAndFont() {

        if (!nativeLoaded) {
            return;
        }
        
        if (!initialized && !init()) {
            return;
        }
        
        initialized = true;

        if (!Boolean.getBoolean("skip.system.fonts")) {
            String defaultFontDesc = getDefaultFont();
            if (!defaultFontDesc.isEmpty()) {
                Font font = Font.decode(defaultFontDesc);

                // Java2D uses 72dpi based size, Gnome sizes are configurable.
                // Also, it's possible (but unlikely) that Java2D may change this
                // default to accomodate high resolution displays, so rather than
                // figuring out what's the real size given the possible
                // resolution, let's ask this to Java2D directly. This may make
                // our default fonts different than proper native application,
                // but we don't care as long as it looks good. If not, use
                // the property above
                float size = new JLabel().getFont().getSize2D();
                font = font.deriveFont(size);

                // The following list applies to Nimbus and is given by the
                // official documentation at this URL:
                // http://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/_nimbusDefaults.html

                UIManager.put("FileChooser.font", font);
                UIManager.put("RootPane.font", font);
                UIManager.put("TextPane.font", font);
                UIManager.put("FormattedTextField.font", font);
                UIManager.put("Spinner.font", font);
                UIManager.put("PopupMenuSeparator.font", font);
                UIManager.put("Table.font", font);
                UIManager.put("TextArea.font", font);
                UIManager.put("Slider.font", font);
                UIManager.put("InternalFrameTitlePane.font", font);
                UIManager.put("DesktopPane.font", font);
                UIManager.put("Menu.font", font);
                UIManager.put("PasswordField.font", font);
                UIManager.put("InternalFrame.font", font);
                UIManager.put("Button.font", font);
                UIManager.put("Panel.font", font);
                UIManager.put("MenuBar.font", font);
                UIManager.put("ComboBox.font", font);
                UIManager.put("Tree.font", font);
                UIManager.put("EditorPane.font", font);
                UIManager.put("ToggleButton.font", font);
                UIManager.put("TabbedPane.font", font);
                UIManager.put("TableHeader.font", font);
                UIManager.put("List.font", font);
                UIManager.put("PopupMenu.font", font);
                UIManager.put("ToolTip.font", font);
                UIManager.put("Separator.font", font);
                UIManager.put("RadioButtonMenuItem.font", font);
                UIManager.put("RadioButton.font", font);
                UIManager.put("ToolBar.font", font);
                UIManager.put("ScrollPane.font", font);
                UIManager.put("CheckBoxMenuItem.font", font);
                UIManager.put("Viewport.font", font);
                UIManager.put("TextField.font", font);
                UIManager.put("SplitPane.font", font);
                UIManager.put("MenuItem.font", font);
                UIManager.put("OptionPane.font", font);
                UIManager.put("ArrowButton.font", font);
                UIManager.put("Label.font", font);
                UIManager.put("ProgressBar.font", font);
                UIManager.put("ScrollBar.font", font);
                UIManager.put("ScrollBarThumb.font", font);
                UIManager.put("ScrollBarTrack.font", font);
                UIManager.put("SliderThumb.font", font);
                UIManager.put("SliderTrack.font", font);
                UIManager.put("TitledBorder.font", font);

                UIManager.put("thermostat-default-font", font);
            }
        }
        // if we at least have the fg colour we can try the rest,
        // otherwise, just skip everything and use nimbus defaults        
        if (hasColor("fg_color")) {
            int fgColor = getColor("fg_color");
            Color text = new Color(fgColor);
            UIManager.put("text", text);
            if (hasColor("selected_fg_color")) {
                fgColor = getColor("selected_fg_color");
                text = new Color(fgColor);
                UIManager.put("textHighlightText", text);
            }
        }

        // same as before, but with bg colours
        if (hasColor("bg_color")) {
            
            // Those numbers are some kind of magic, they represent the
            // value, or brightness, in the HSV encoding of the colour.
            // The idea is to derive a darker version of the
            // base colour because nimbus will use a brighter version
            // for most components. The version used by nimbus does not
            // exactly match because nimbus use many multi-gradient
            // paints.
            float brightnessOffset = -.300f;
            
            Color nimbusBase = deriveColor("bg_color", UIManager.getDefaults().getColor("nimbusBase"), brightnessOffset);
            int bgColor = getColor("bg_color");
            Color control = new Color(bgColor);
            
            Color info = control;
            
            UIManager.put("nimbusBase", nimbusBase);
            
            UIManager.put("control", control);
            UIManager.put("info", info);
            
            if (hasColor("selected_bg_color")) {
                int fgColor = getColor("selected_bg_color");
                Color nimbusFocus = new Color(fgColor);
                
                UIManager.put("nimbusFocus", nimbusFocus);
                UIManager.put("nimbusSelectionBackground", nimbusFocus);
                UIManager.put("nimbusSelection", nimbusFocus);
                UIManager.put("menu", nimbusFocus);
                UIManager.put("Menu.background", nimbusFocus);
            }
        }
        
    }
}

