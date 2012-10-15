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

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.heap.HeapHistogramView;
import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.client.ui.SwingComponent;
import com.redhat.thermostat.common.heap.HistogramRecord;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.redhat.thermostat.common.utils.DescriptorConverter;
import com.redhat.thermostat.swing.HeaderPanel;
import com.redhat.thermostat.swing.ThermostatTable;
import com.redhat.thermostat.swing.ThermostatTableRenderer;

@SuppressWarnings("serial")
public class HistogramPanel extends HeapHistogramView implements SwingComponent {

    private final JPanel panel;

    private HeaderPanel headerPanel;

    public HistogramPanel() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        headerPanel = new HeaderPanel(Translate.localize(LocaleResources.HEAP_DUMP_CLASS_USAGE));
        panel.add(headerPanel);
    }

    @Override
    public void display(ObjectHistogram histogram) {
        ThermostatTable table = new ThermostatTable(new HistogramTableModel(histogram));
        table.setDefaultRenderer(Long.class, new NiceNumberFormatter());
        headerPanel.setContent(table.wrap());
    }

    private final class NiceNumberFormatter extends ThermostatTableRenderer {

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

        private final String[] columnNames = new String[] {
            Translate.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_CLASS),
            Translate.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_INSTANCES),
            Translate.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_SIZE),
        };

        private List<HistogramRecord> histogram;

        public HistogramTableModel(ObjectHistogram objHistogram) {
            histogram = new ArrayList<>();
            histogram.addAll(objHistogram.getHistogram());
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
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
            if (row >= histogram.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if (histogram != null) {
                HistogramRecord record = histogram.get(row);
                switch (column) {
                case 0:
                    result = DescriptorConverter.toJavaType(record.getClassname());
                    break;
                case 1:
                    result = Long.valueOf(record.getNumberOf());
                    break;
                case 2:
                    result = Long.valueOf(record.getTotalSize());
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }
            return result;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            if (histogram != null) {
                return histogram.size();
            }
            return 0;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    @Override
    public Component getUiComponent() {
        return panel;
    }
}
