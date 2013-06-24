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

package com.redhat.thermostat.internal.utils.laf;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import com.redhat.thermostat.internal.utils.laf.gtk.GTKThemeUtils;

public class ThemeManager {
    
    private static final Logger logger = Logger.getLogger(ThemeManager.class.getSimpleName());
    private static final ThemeManager theInstance = new ThemeManager();
    
    ThemeManager() {}
    
    public static ThemeManager getInstance() {
        return theInstance;
    }

    private boolean setLAF(String laf) {

        boolean set = false;
        try {
            UIManager.setLookAndFeel(laf);
            set = true;
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                InstantiationException | IllegalAccessException e) {
            logger.log(Level.WARNING, "cannot set look and feel {0}", laf);
        }
        return set;
    }

    /**
     * Sets the Look and Feel for Thermostat based on user preferences.
     * 
     * <br /><br />
     * 
     * If the default theme is used, we try to match if possible the native
     * theme main colours.
     * 
     * <br /><br />
     * 
     * This method must be called in the EDT.
     */
    public void setLAF() {
        
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method expect to be called " +
            		                    "from the Event Dispatching Thread");
        }
        
        boolean tryGTKColors = false;
        
        // check if the user has other preferences...
        String laf = System.getProperty("swing.defaultlaf");
        if (laf == null) {
            laf = "nimbus";
            tryGTKColors = true;
        }

        switch (laf) {
            case "system":
                laf = UIManager.getSystemLookAndFeelClassName();
                break;
            case "nimbus":
                laf = NimbusLookAndFeel.class.getName();
                break;
            case "dolphin":
                laf = "com.redhat.swing.laf.dolphin.DolphinLookAndFeel";
                break;
            default:
                break;
        }

        if (!setLAF(laf)) {
            setLAF(NimbusLookAndFeel.class.getName());
        }
        
        // Thermostat JPopupMenu instances should all be
        // ThermostatPopupmenu, so this is redundant, but done in case
        // some client code doesn't use the internal popup
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        UIManager.getDefaults().put("OptionPane.buttonOrientation", SwingConstants.RIGHT);
        UIManager.getDefaults().put("OptionPane.sameSizeButtons", true);
        
        String desktop = System.getProperty("sun.desktop");
        String os = System.getProperty("os.name");
        if (os != null && os.equalsIgnoreCase("linux") &&
            desktop != null && !desktop.equalsIgnoreCase("kde"))
        {
            UIManager.getDefaults().put("OptionPane.isYesLast", true);
        }
        
        if (tryGTKColors && desktop != null && desktop.equalsIgnoreCase("gnome")) {
            GTKThemeUtils utils = new GTKThemeUtils();
            utils.setNimbusColours();
        }
    }
}
