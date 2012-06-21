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

package com.redhat.thermostat.client.heap.swing;

import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import com.redhat.thermostat.client.heap.Histogram;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class HistogramPanel extends JPanel {
    
    private HeaderPanel headerPanel;
    
    public HistogramPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        headerPanel = new HeaderPanel("Classes Usage");
        add(headerPanel);
    }

    public void display(Histogram histogram) {
        JTable table = new JTable(new HistogramTableModel(histogram));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Long.class, new NiceNumberFormatter());
        JScrollPane scrollPane = new JScrollPane(table);
        headerPanel.setContent(scrollPane);
    }

    @SuppressWarnings("serial")
    private final class NiceNumberFormatter extends DefaultTableCellRenderer {

        private final DecimalFormat formatter = new DecimalFormat("###,###.###");

        private NiceNumberFormatter() {
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        protected void setValue(Object v) {
            String formatted = formatter.format(v);
            setText(formatted);
        }
    }

    private class HistogramTableModel extends DefaultTableModel {
        
        private Histogram histogram;
        public HistogramTableModel(Histogram histogram) {
            this.histogram = histogram;
        }

        @Override
        public String getColumnName(int column) {
            if (histogram != null) {
                return histogram.getHistogramColums()[column];
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0) {
                return String.class;
            } else {
                return Long.class;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            Object result = null;
            if (histogram != null) {
                result = histogram.getData().get(row)[column];
            }
            return result;
        }
        
        @Override
        public int getColumnCount() {
            if (histogram != null) {
                return histogram.getHistogramColums().length;
            }
            return 0;
        }
        
        @Override
        public int getRowCount() {
            if (histogram != null) {
                return histogram.getData().size();
            }
            return 0;
        }
    }
}
