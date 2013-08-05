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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.ThermostatThinScrollBar;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class SwingHeapDumpListView extends HeapDumpListView implements SwingComponent {

    private JPanel container;
    private JScrollPane scrollPane;
    private HeapDumpModel model;
    private JList<HeapDump> list;

    public SwingHeapDumpListView() {
        container = new JPanel();
        container.setName(getClass().getName());
        container.setLayout(new BorderLayout(0, 0));
        container.setOpaque(false);

        model = new HeapDumpModel();
        list = new JList<>(model);
        list.setName(getClass().getName() + "_LIST");
        list.setBorder(null);
        list.setOpaque(false);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    HeapDump dump = model.get(index);
                    listNotifier.fireAction(ListAction.DUMP_SELECTED, dump);
                }
            }
        });
        
        list.setCellRenderer(new HeapCellRenderer());
        
        scrollPane = new JScrollPane(list);
        scrollPane.setVerticalScrollBar(new ThermostatThinScrollBar(ThermostatThinScrollBar.VERTICAL));
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        container.add(scrollPane, BorderLayout.CENTER);
    }
    
    @Override
    public Component getUiComponent() {
        return container;
    }
    
    @Override
    public void setDumps(List<HeapDump> dumps) {
        
        final List<HeapDump> _dumps = new ArrayList<>(dumps);
        Collections.sort(_dumps, new DumpsComparator());
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.clear();
                for (HeapDump  dump : _dumps) {
                    model.addElement(dump);
                    container.repaint();
                }
            }
        });
    }
    
    @SuppressWarnings("serial")
    private class HeapDumpModel extends DefaultListModel<HeapDump> {}
    
    private class DumpsComparator implements Comparator<HeapDump> {
        @Override
        public int compare(HeapDump o1, HeapDump o2) {
            // TODO: descending order only for now, we should allow the users
            // to sort this via the UI though
            int result = Long.compare(o1.getTimestamp(), o2.getTimestamp());
            return -result;
        }
    }
    
    private class HeapCellRenderer implements ListCellRenderer<HeapDump> {

        @Override
        public Component getListCellRendererComponent(JList<? extends HeapDump> list, HeapDump value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            
            ShadowLabel label = new ShadowLabel(new LocalizedString(value.toString()));
            label.setForeground(Palette.ROYAL_BLUE.getColor());

            if (!isSelected) {
                label.setOpaque(false);
            } else {
                label.setOpaque(true);
                label.setBackground(Palette.ELEGANT_CYAN.getColor());
            }
            
            return label;
        }
        
    }
}
