/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GTKThemeUtils {

    private static boolean initialized;
    static {
        try {
            System.loadLibrary("GTKThemeUtils");
            initialized = init();
        } catch (UnsatisfiedLinkError ignore) {}
    }
    
    native private static boolean init();
    native private static boolean hasColor(String id);
    native private static int getColor(String id);

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
    
    public void setNimbusColours() {

        if (!initialized) {
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                // if we at least have the bg colour we can try the rest,
                // otherwise, just skip everything and use nimbus defaults
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
                    Color control = UIManager.getDefaults().getColor("control");
                    int bgColor = getColor("bg_color");
                    control = new Color(bgColor);
                    
                    Color info = control;
                    
                    UIManager.put("nimbusBase", nimbusBase);
                    
                    UIManager.put("control", control);
                    UIManager.put("info", info);

                    Color nimbusFocus = UIManager.getDefaults().getColor("nimbusFocus");
                    if (hasColor("selected_bg_color")) {
                        int fgColor = getColor("selected_bg_color");
                        nimbusFocus = new Color(fgColor);
                        
                        UIManager.put("nimbusFocus", nimbusFocus);
                        UIManager.put("nimbusSelectionBackground", nimbusFocus);
                        UIManager.put("nimbusSelection", nimbusFocus);
                        UIManager.put("menu", nimbusFocus);
                        UIManager.put("Menu.background", nimbusFocus);
                    }
                }
            }
        });
    }
}
