/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.redhat.thermostat.client.ui.Palette;

/**
 * A {@link TableCellRenderer} that colors rows to make them easier to read.
 */
@SuppressWarnings("serial")
public class ThermostatTableRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (result == null || isSelected) {
            // do nothing
        } else if (!isEven(row)) {
            result.setBackground(Palette.LIGHT_GRAY.getColor());
        } else {
            result.setBackground(Palette.WHITE.getColor());
        }
        
        return result;
    }

    @Override
    protected void setValue(Object value) {
        // lets this renderer display icons correctly
        if (value instanceof Icon) {
            setAlignmentX(CENTER_ALIGNMENT);
            setIcon((Icon) value);
        } else {
            setAlignmentX(LEFT_ALIGNMENT);
            super.setValue(value);
        }
    }

    private boolean isEven(int row) {
        return row % 2 == 0;
    }
}

