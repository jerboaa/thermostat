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

import java.awt.BorderLayout;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.redhat.thermostat.client.heap.HeapDumpDetailsView;
import com.redhat.thermostat.client.ui.SearchFieldView.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaHeapObject;

@SuppressWarnings("serial")
public class HeapDetailsSwing extends JPanel implements HeapDumpDetailsView {

    private JTabbedPane tabPane = new JTabbedPane();

    private ObjectDetailsPanel objectDetailsPanel = new ObjectDetailsPanel();

    private final ActionNotifier<Action> notifier = new ActionNotifier<Action>(this);

    public HeapDetailsSwing() {
        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);

        tabPane.addTab("Histogram", /* dummy */ new JPanel());
        tabPane.addTab("Object Browser", objectDetailsPanel);

        objectDetailsPanel.getSearchField().addActionListener(new ActionListener<SearchAction>() {
            @Override
            public void actionPerformed(ActionEvent<SearchAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                case TEXT_CHANGED:
                    notifier.fireAction(Action.SEARCH);
                    break;
                }
            }
        });

        objectDetailsPanel.getSearchField().setLabel("Search for objects by class name");

        objectDetailsPanel.getObjectList().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                notifier.fireAction(Action.GET_OBJECT_DETAIL);
            }
        });

    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListnener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public void setHeapHistogram(final ObjectHistogram objectHistogram) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                HistogramPanel histogram = new HistogramPanel();
                histogram.display(objectHistogram);

                tabPane.removeTabAt(0);
                tabPane.insertTab("Histogram", null, histogram, null, 0);
                tabPane.setSelectedIndex(0);
                tabPane.revalidate();
            }
        });
    }

    @Override
    public String getSearchText() {
        return objectDetailsPanel.getSearchField().getSearchText();

    }

    @Override
    public HeapObjectUI getSelectedMatchingObject() {
        return objectDetailsPanel.getSelectedMatchingObject();
    }

    @Override
    public void setObjectDetails(JavaHeapObject object) {
        objectDetailsPanel.setObjectDetails(object);
    }

    @Override
    public void setMatchingObjects(Collection<HeapObjectUI> objects) {
        objectDetailsPanel.setMatchingObjects(objects);
    }

    @Override
    public JComponent getUIComponent() {
        return this;
    }

}
