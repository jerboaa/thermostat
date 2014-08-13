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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.notes.client.swing.internal.NotesView.Action;
import com.redhat.thermostat.shared.locale.Translate;

public class NotePanel extends JPanel {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JTextArea text;

    public NotePanel(final NoteViewModel viewModel, final ActionNotifier<Action> actionNotifier) {
        // wrap in html tags to enable line wrapping
        SimpleDateFormat formatter = new SimpleDateFormat("'<html>'yyyy-MM-dd'<br>'HH:mm'</html>'");
        String date = formatter.format(new Date(viewModel.timeStamp));
        JLabel timeStampLabel = new JLabel(date);
        text = new JTextArea(viewModel.text);
        Icon deleteIcon = new FontAwesomeIcon('\uf014', 12);
        JButton deleteButton = new JButton(deleteIcon);
        deleteButton.setToolTipText(translator.localize(LocaleResources.NOTES_DELETE).getContents());
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actionNotifier.fireAction(Action.DELETE, viewModel.tag);
            }
        });

        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.ipadx = 5;
        constraints.ipady = 5;

        constraints.weightx = 0;
        this.add(timeStampLabel, constraints);

        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        this.add(text, constraints);

        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        this.add(deleteButton, constraints);
    }

    public String getContent() {
        return text.getText();
    }

}
