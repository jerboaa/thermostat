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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.heap.HeapObjectUI;
import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.ObjectRootsView;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTextPane;

public class ObjectRootsFrame extends JFrame implements ObjectRootsView {

    private final ActionNotifier<Action> notifier = new ActionNotifier<>(this);

    private final DefaultListModel<HeapObjectUI> dataModel;

    private JTextPane objectDetails;

    public ObjectRootsFrame() {
        setTitle(Translate.localize(LocaleResources.OBJECT_ROOTS_VIEW_TITLE));

        dataModel = new DefaultListModel<>();
        JList<HeapObjectUI> pathList = new JList<>(dataModel);
        pathList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pathList.setVisibleRowCount(1);

        JScrollPane scrollPane = new JScrollPane(pathList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JLabel lblNewLabel = new JLabel(Translate.localize(LocaleResources.OBJECT_ROOTS_VIEW_TITLE));

        objectDetails = new JTextPane();
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(objectDetails, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                        .addComponent(lblNewLabel, Alignment.LEADING)
                        .addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblNewLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(objectDetails, GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                    .addContainerGap())
        );
        getContentPane().setLayout(groupLayout);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                notifier.fireAction(Action.VISIBILE);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                notifier.fireAction(Action.HIDDEN);
            }
        });

        pathList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                notifier.fireAction(Action.OBJECT_SELECTED, ((JList<?>)e.getSource()).getSelectedValue());
            }
        });


    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public void showView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pack();
                setVisible(true);
            }
        });
    }

    @Override
    public void hideView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(false);
                dispose();
            }
        });
    }

    @Override
    public void setPathToRoot(List<HeapObjectUI> pathToRoot) {
        final List<HeapObjectUI> path = new ArrayList<>();
        for (HeapObjectUI heapObj : pathToRoot) {
            path.add(heapObj);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dataModel.clear();
                for (HeapObjectUI obj : path) {
                    dataModel.addElement(obj);
                }
            }
        });
    }

    @Override
    public void setObjectDetails(final String information) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                objectDetails.setText(information);
            }
        });
    }

}
