/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice.WindowTranslucency;

class TranslucencyUtils {
    public static final boolean TRANSLUCENT;
    public static final float TRANSPARENCY;
    public static final float OPAQUE = 1.0f;

    static {
        ThermostatPopupMenu.setDefaultLightWeightPopupEnabled(false);

        boolean translucent = !Boolean.getBoolean("com.redhat.thermostat.popup.opaque");
        if (translucent) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            translucent = gd.isWindowTranslucencySupported(WindowTranslucency.TRANSLUCENT);
        }
        
        float transparency = 0.90f;
        String transparencyProp = System.getProperty("com.redhat.thermostat.popup.transparency");
        if (transparencyProp != null) {
            try {
                transparency = Float.parseFloat(transparencyProp);
                if (transparency > 1 || transparency < 0) {
                    translucent = false;
                }
            } catch (Throwable ignore) {}
        }
        
        TRANSLUCENT = translucent;
        TRANSPARENCY = transparency;
    }
}

