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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.SearchField;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.components.ThermostatTableRenderer;
import com.redhat.thermostat.client.ui.SearchProvider.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.utils.DescriptorConverter;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;


@SuppressWarnings("serial")
public class HistogramPanel extends HeapHistogramView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final JPanel panel;

    private ThermostatTable table;

    private final ActionNotifier<HistogramAction> notifier = new ActionNotifier<>(this);


    public HistogramPanel() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        HeaderPanel headerPanel = createHeader();
        panel.add(headerPanel);
    }

    public HeaderPanel createHeader() {
        table = new ThermostatTable();
        table.setDefaultRenderer(Long.class, new NiceNumberFormatter());
        JScrollPane scrollPane = table.wrap();

        final SearchField searchField = new SearchField();
        searchField.setTooltip(translator.localize(LocaleResources.HEAP_DUMP_OBJECT_BROWSE_SEARCH_PATTERN_HELP));
        searchField.setLabel(translator.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_BROWSE_SEARCH_HINT));
        searchField.addSearchListener(new ActionListener<SearchAction>() {
            @Override
            public void actionPerformed(ActionEvent<SearchAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case PERFORM_SEARCH:
                        notifier.fireAction(HistogramAction.SEARCH, searchField.getSearchText());
                        break;
                    default:
                        break;
                }
            }
        });

        JPanel displayContents = new JPanel();

        GroupLayout layout = new GroupLayout(displayContents);
        displayContents.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(searchField)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(searchField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE))
        );

        HeaderPanel headerPanel = new HeaderPanel(translator.localize(LocaleResources.HEAP_DUMP_CLASS_USAGE));
        headerPanel.setContent(displayContents);
        return headerPanel;
    }

    @Override
    public void addHistogramActionListener(ActionListener<HistogramAction> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void setHistogram(final ObjectHistogram histogram) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                table.setModel(new HistogramTableModel(histogram));
            }
        });
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

    private class HistogramTableModel extends NonEditableTableModel {

        private final String[] columnNames = new String[] {
            translator.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_CLASS).getContents(),
            translator.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_INSTANCES).getContents(),
            translator.localize(LocaleResources.HEAP_DUMP_HISTOGRAM_COLUMN_SIZE).getContents(),
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

    }

    @Override
    public Component getUiComponent() {
        return panel;
    }
}

