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

import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;

import com.redhat.thermostat.common.heap.HeapDump;

@SuppressWarnings("serial")
public class StatsPanel extends JPanel {
    
    private JPanel leftPanel;
    
    private JButton heapDumpButton;
    private JList<HeapDump> dumpList;
    private DefaultListModel<HeapDump> listModel;
    
    private JLabel max;
    private JLabel current;
    
    public StatsPanel() {
        
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        
        JPanel rightPanel = new JPanel();
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                    .addComponent(leftPanel, GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(rightPanel, GroupLayout.PREFERRED_SIZE, 252, GroupLayout.PREFERRED_SIZE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(rightPanel, GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                .addComponent(leftPanel, GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
        );
        
        JLabel currentLabel = new JLabel("used:");
        currentLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        JLabel maxLabel = new JLabel("capacity:");
        maxLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        current = new JLabel("-");
        current.setHorizontalAlignment(SwingConstants.RIGHT);
        
        max = new JLabel("-");
        max.setHorizontalAlignment(SwingConstants.RIGHT);
        
        heapDumpButton = new JButton("Heap Dump");
        
        JScrollPane dumpListScrollPane = new JScrollPane();
        dumpList = new JList<>();
        listModel = new DefaultListModel<>();
        dumpList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dumpList.setModel(listModel);
        
        GroupLayout gl_rightPanel = new GroupLayout(rightPanel);
        gl_rightPanel.setHorizontalGroup(
            gl_rightPanel.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_rightPanel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_rightPanel.createParallelGroup(Alignment.TRAILING)
                        .addComponent(dumpListScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                        .addComponent(heapDumpButton, GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                        .addGroup(gl_rightPanel.createSequentialGroup()
                            .addGroup(gl_rightPanel.createParallelGroup(Alignment.TRAILING, false)
                                .addComponent(maxLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(currentLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_rightPanel.createParallelGroup(Alignment.TRAILING)
                                .addComponent(current, GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
                                .addComponent(max, GroupLayout.PREFERRED_SIZE, 119, GroupLayout.PREFERRED_SIZE))))
                    .addContainerGap())
        );
        gl_rightPanel.setVerticalGroup(
            gl_rightPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_rightPanel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_rightPanel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(currentLabel, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
                        .addComponent(current))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_rightPanel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(maxLabel)
                        .addComponent(max))
                    .addGap(18)
                    .addComponent(heapDumpButton)
                    .addGap(18)
                    .addComponent(dumpListScrollPane, GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                    .addContainerGap())
        );
        rightPanel.setLayout(gl_rightPanel);
        setLayout(groupLayout);

        dumpListScrollPane.setViewportView(dumpList);

        // initially invisible
        dumpList.setVisible(false);
    }
    
    void setChartPanel(JPanel panel) {
        leftPanel.removeAll();
        leftPanel.add(panel);
        leftPanel.revalidate();
        repaint();
    }

    public void setMax(String capacity) {
        max.setText(capacity);
    }

    public void setUsed(String used) {
        current.setText(used);
    }
    
    void addHeapDumperListener(ActionListener listener) {
        heapDumpButton.addActionListener(listener);
    }

    void addDumpListListener(ListSelectionListener listener) {
        dumpList.addListSelectionListener(listener);
    }
    
    public void disableHeapDumperControl() {
        heapDumpButton.setText("dumping...");
        heapDumpButton.setEnabled(false);
    }

    public void enableHeapDumperControl() {
        heapDumpButton.setText("Heap Dump");
        heapDumpButton.setEnabled(true);
    }

    public void addDump(HeapDump dump) {
        
        listModel.addElement(dump);
        if (!dumpList.isVisible()) {
            dumpList.setVisible(true);
        }
    }

    public void clearDumpList() {
        listModel.clear();
        if (dumpList.isVisible()) {
            dumpList.setVisible(false);
        }
    }
    
    public HeapDump getSelectedHeapDump() {
        return dumpList.getSelectedValue();
    }
}
