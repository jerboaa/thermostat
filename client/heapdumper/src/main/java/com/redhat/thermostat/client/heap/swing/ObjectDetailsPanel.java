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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.Callable;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;

import com.redhat.thermostat.client.heap.HeapDumpDetailsView.HeapObjectUI;
import com.redhat.thermostat.client.ui.EdtHelper;
import com.redhat.thermostat.client.ui.SearchFieldSwingView;
import com.redhat.thermostat.client.ui.SearchFieldView;
import com.sun.tools.hat.internal.model.JavaHeapObject;

import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import javax.swing.JSplitPane;

/**
 * A Panel that displays a list of JavaHeapObject and details about a single, selected one.
 */
class ObjectDetailsPanel extends JPanel {

    private final SearchFieldSwingView searchField;

    private final DefaultListModel<HeapObjectUI> model = new DefaultListModel<>();
    private final JList<HeapObjectUI> list = new JList<>(model);

    private final JTextPane objectDetailsPane;

    public ObjectDetailsPanel() {

        JLabel searchLabel = new JLabel("Search for Object");

        searchField = new SearchFieldSwingView();

        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(0.2 /* 20% */);

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(searchLabel)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(splitPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                                .addComponent(searchField, GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE))))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(searchLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(searchField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)
                    .addContainerGap())
        );

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(list);

        splitPane.setLeftComponent(scrollPane);

        JPanel panel = new JPanel();
        splitPane.setRightComponent(panel);

        panel.setLayout(new BorderLayout(0, 0));

        objectDetailsPane = new JTextPane();
        objectDetailsPane.setEditable(false);
        panel.add(objectDetailsPane);
        setLayout(groupLayout);
    }

    public SearchFieldView getSearchField() {
        return searchField;
    }

    public JList<HeapObjectUI> getObjectList() {
        return list;
    }

    public void setMatchingObjects(final Collection<HeapObjectUI> objects) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.clear();
                for (HeapObjectUI object: objects) {
                    model.addElement(object);
                }
            }
        });
    }

    public void setObjectDetails(final JavaHeapObject obj) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String text = "Object ID:" + obj.getIdString() + "\n" +
                              "Type:" + obj.getClazz().getName() + "\n" +
                              "Size:" + String.valueOf(obj.getSize()) + " bytes" + "\n" +
                              "Heap allocated:" + String.valueOf(obj.isHeapAllocated()) + "\n";
                objectDetailsPane.setText(text);
            }
        });
    }

    /**
     * @return null if no selected object
     */
    public HeapObjectUI getSelectedMatchingObject() {
        try {
            return new EdtHelper().callAndWait(new Callable<HeapObjectUI>() {
                @Override
                public HeapObjectUI call() throws Exception {
                    return list.getSelectedValue();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            return null;
        }
    }
}
