/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.notes.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.common.ActionNotifier;

/** SwingComponent serves as a tag for SwingClient to use this view */
public class VmNotesView implements UIComponent, SwingComponent {

    private JPanel container;
    private JTextArea notes;

    public enum Action {
        LOAD,
        /** Payload contains the text */
        SAVE,
    }

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    public VmNotesView() {
        container = new JPanel();
        container.setLayout(new BorderLayout());

        JPanel toolBar = new JPanel();
        container.add(toolBar, BorderLayout.PAGE_START);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionNotifier.fireAction(Action.SAVE, notes.getText());
            }
        });
        toolBar.add(saveButton);

        notes = new JTextArea("<Notes>");
        container.add(notes, BorderLayout.CENTER);
    }

    @Override
    public Component getUiComponent() {
        return container;
    }

    public ActionNotifier<Action> getNotifier() {
        return actionNotifier;
    }

    public void setContent(final String content) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notes.setText(content);
            }
        });
    }
}
