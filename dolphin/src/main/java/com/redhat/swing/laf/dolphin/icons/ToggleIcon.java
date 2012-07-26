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

package com.redhat.swing.laf.dolphin.icons;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

class ToggleIcon extends ImageIcon {

    private final ImageIcon unselectedDelegate;
    private final ImageIcon selectedDelegate;
    private final ImageIcon disabledUnselectedDelegate;
    private final ImageIcon disabledSelectedDelegate;

    /** The icons must be the same size */
    public ToggleIcon(String unchecked, String checked, String disabledUnchecked, String disabledChecked) {
        unselectedDelegate = new ImageIcon(getClass().getResource(unchecked));
        selectedDelegate = new ImageIcon(getClass().getResource(checked));
        disabledUnselectedDelegate = new ImageIcon(getClass().getResource(disabledUnchecked));
        disabledSelectedDelegate = new ImageIcon(getClass().getResource(disabledChecked));
    }

    @Override
    public int getIconHeight() {
        return unselectedDelegate.getIconHeight();
    }

    @Override
    public int getIconWidth() {
        return unselectedDelegate.getIconWidth();
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {

        boolean paintIcon = true;

        boolean selected = false;
        boolean enabled = true;

        if (c instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) c;
            if (!button.getModel().isEnabled()) {
                enabled = false;
            }
            if (button.getModel().isSelected()) {
                selected = true;
            }
        }

        if (paintIcon) {
            if (enabled && !selected) {
                unselectedDelegate.paintIcon(c, g, x, y);
            } else if (enabled && selected) {
                selectedDelegate.paintIcon(c, g, x, y);
            } else if (!enabled && !selected){
                disabledUnselectedDelegate.paintIcon(c, g, x, y);
            } else if (!enabled && selected) {
                disabledSelectedDelegate.paintIcon(c, g, x, y);
            }
        }
    }


}
