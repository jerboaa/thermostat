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

package com.redhat.thermostat.client.swing.components;

import java.awt.Component;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

class ThermostatTableColumnResizer {
    
    // reference Swing Hacks book:
    // http://shop.oreilly.com/product/9780596009076.do
    
    private ThermostatTable table;
    
    public ThermostatTableColumnResizer(ThermostatTable table) {
        this.table = table;
    }
    
    public void resize() {
        TableColumnModel model = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++ ) {
            int maxWidth = 0;
            for (int row = 0; row < table.getRowCount(); row++ ) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Object value = table.getValueAt(row, column);
                Component component = renderer.getTableCellRendererComponent(table, value, false, false, row, column);
                maxWidth = Math.max(component.getPreferredSize().width, maxWidth);
            }
            
            TableColumn tableColumn = model.getColumn(column);
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer(); 
            }
            
            Object headerValue = tableColumn.getHeaderValue();
            Component headerComponent = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, column);
            maxWidth = Math.max(maxWidth, headerComponent.getPreferredSize().width);
            
            tableColumn.setPreferredWidth(maxWidth);
        }
    }
}
