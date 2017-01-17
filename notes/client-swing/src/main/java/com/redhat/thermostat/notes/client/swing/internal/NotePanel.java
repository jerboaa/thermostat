/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.ThermostatTextArea;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.notes.client.core.NotesView.NoteAction;
import com.redhat.thermostat.notes.client.core.Utils;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.shared.locale.Translate;

public class NotePanel extends JPanel {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    public static final int TEXT_SIZE = 12;
    public static final int PADDING = 5;

    private Note note;
    private ThermostatTextArea text;
    private JLabel timeStampLabel;
    private ActionNotifier<NoteAction> actionNotifier;

    public NotePanel(final Note note, final ActionNotifier<NoteAction> actionNotifier, final SwingNotesView.FocusRequester<ThermostatTextArea> focusRequester) {
        Utils.assertInEdt();
        this.note = note;
        this.actionNotifier = actionNotifier;

        // wrap in html tags to enable line wrapping
        String date = getPrettyTimeStamp(note.getTimeStamp());
        timeStampLabel = new JLabel(date);
        text = new ThermostatTextArea(note.getContent());
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent event) {
                NotePanel.this.actionNotifier.fireAction(NoteAction.LOCAL_SAVE, note.getId());
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                NotePanel.this.actionNotifier.fireAction(NoteAction.LOCAL_SAVE, note.getId());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                NotePanel.this.actionNotifier.fireAction(NoteAction.LOCAL_SAVE, note.getId());
            }
        });
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusRequester.setComponent(text);
            }
        });
        focusRequester.setComponent(text);
        Icon deleteIcon = new FontAwesomeIcon('\uf014', TEXT_SIZE);
        JButton deleteButton = new JButton(deleteIcon);
        deleteButton.setToolTipText(translator.localize(LocaleResources.NOTES_DELETE).getContents());
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                NotePanel.this.actionNotifier.fireAction(NoteAction.LOCAL_DELETE, note.getId());
            }
        });

        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.ipadx = 0;
        constraints.ipady = PADDING;

        constraints.weightx = 0;
        this.add(timeStampLabel, constraints);

        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        this.add(text, constraints);

        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        this.add(deleteButton, constraints);
    }

    public Note getNote() {
        Utils.assertInEdt();
        return note;
    }

    public void setNote(Note note) {
        Utils.assertInEdt();
        this.note = note;
        setContent(note.getContent());
        setTimeStamp(note.getTimeStamp());
    }

    public String getContent() {
        Utils.assertInEdt();
        return text.getText();
    }

    public long getTimestamp() {
        Utils.assertInEdt();
        return note.getTimeStamp();
    }

    private void setContent(String content) {
        Utils.assertInEdt();
        text.setText(content);
    }

    private void setTimeStamp(long timeStamp) {
        Utils.assertInEdt();
        timeStampLabel.setText(getPrettyTimeStamp(timeStamp));
    }

    private static String getPrettyTimeStamp(long timeStamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("'<html>'yyyy-MM-dd'<br>'HH:mm'</html>'");
        return formatter.format(new Date(timeStamp));
    }

}
