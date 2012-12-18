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

package com.redhat.thermostat.client.ui;

import java.awt.Color;

/**
 * A set of preselected colors suitable for using to draw custom
 * painting of charts graphs and figures.
 */
public enum Palette {

    THERMOSTAT_BLU(new Color(74, 93, 117)),
    THERMOSTAT_RED(new Color(226, 46, 42)),
    
    RED(new Color(192, 0, 0)),
    PALE_RED(new Color(192, 80, 77)),
    
    SKY_BLUE(new Color(79, 129, 189)),
    AZUREUS(new Color(0, 176, 190)),
    EGYPTIAN_BLUE(new Color(74, 144, 217)),
    DIRTY_CYAN(new Color(75, 172, 198)),
    PRUSSIAN_BLUE(new Color(0, 49, 83)),
    
    GREEN(new Color(146, 208, 80)),
    TUNDRA_GREEN(new Color(155, 187, 89)),

    POMP_AND_POWER_VIOLET(new Color(128, 100, 162)),
    VIOLET(new Color(112, 48, 160)),

    EARL_GRAY(new Color(128, 128, 128)),
    
    PALE_GRAY(new Color(235, 235, 235)),
    LIGHT_GRAY(new Color(242, 242, 242)),
    GRAY(new Color(216, 216, 216)),
    DARK_GRAY(new Color(168, 172, 168)),

    GRANITA_ORANGE(new Color(247,150,70)),
    
    BLACK(Color.BLACK),
    WHITE(Color.WHITE),

    /* END */ ;
    
    private Color color;
    Palette(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
}
