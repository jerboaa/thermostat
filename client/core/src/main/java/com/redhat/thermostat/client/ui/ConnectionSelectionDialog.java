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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionType;

public class ConnectionSelectionDialog extends JDialog {

    private static final long serialVersionUID = -3149845673473434408L;

    private static final int ICON_LABEL_GAP = 5;

    private boolean cancelled = false;
    private final Connection model;

    public ConnectionSelectionDialog(JFrame owner, Connection model) {
        super(owner);
        setTitle(localize(LocaleResources.STARTUP_MODE_SELECTION_DIALOG_TITLE));
        this.model = model;
        setupUi();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // note: this is only fired when the user tries to close the window
        // not when we try to close the window
        addWindowListener(new CancelListener(this));
    }

    private void setupUi() {
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        add(createModeSelectionUi(), BorderLayout.CENTER);

        FlowLayout bottomPanelLayout = new FlowLayout(FlowLayout.TRAILING);
        JPanel bottomPanel = new JPanel(bottomPanelLayout);
        add(bottomPanel, BorderLayout.PAGE_END);
        bottomPanel.add(Box.createGlue());

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 5, ICON_LABEL_GAP, ICON_LABEL_GAP));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(buttonsPanel);

        JButton cancelButton = new JButton(localize(LocaleResources.BUTTON_CANCEL));
        cancelButton.setMargin(new Insets(0, 15, 0, 15));
        cancelButton.addActionListener(new CancelListener(this));
        buttonsPanel.add(cancelButton);
    }

    private JPanel createModeSelectionUi() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP));
        GridBagLayout layout = new GridBagLayout();
        container.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        Insets normalInsets = new Insets(ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP, ICON_LABEL_GAP);
        c.insets = normalInsets;
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0;

        JLabel info = new JLabel(localize(LocaleResources.STARTUP_MODE_SELECTION_INTRO));
        container.add(info, c);

        c.gridy++;
        String localButtonHtml = buildHtml(localize(LocaleResources.STARTUP_MODE_SELECTION_TYPE_LOCAL),
                                           IconResource.COMPUTER.getUrl());
        JButton localButton = new JButton(localButtonHtml);
        container.add(localButton, c);

        c.gridy++;
        String remoteButtonHtml = buildHtml(localize(LocaleResources.STARTUP_MODE_SELECTION_TYPE_REMOTE),
                                            IconResource.NETWORK_SERVER.getUrl());
        JButton remoteButton = new JButton(remoteButtonHtml);
        container.add(remoteButton, c);

        c.gridy++;
        String clusterButtonHtml = buildHtml(localize(LocaleResources.STARTUP_MODE_SELECTION_TYPE_CLUSTER),
                                             IconResource.NETWORK_GROUP.getUrl());
        JButton clusterButton = new JButton(clusterButtonHtml);
        container.add(clusterButton, c);

        localButton.addActionListener(new SetStartupModeListener(this, ConnectionType.LOCAL));
        remoteButton.addActionListener(new SetStartupModeListener(this, ConnectionType.REMOTE));
        clusterButton.addActionListener(new SetStartupModeListener(this, ConnectionType.CLUSTER));
        return container;
    }

    private String buildHtml(String text, String imageUrl) {
        /* build a table to vertically align image and text properly */
        // TODO does not deal correctly with right-to-left languages
        String html = "" +
                "<html>" +
                " <table>" +
                "  <tr> " +
                "   <td> " + "<img src='" + imageUrl + "'>" + "</td>" +
                "   <td>" + new HtmlTextBuilder().huge(text).toHtml() + "</td>" +
                "  </tr>" +
                " </table>" +
                "</html>";
        return html;
    }

    public Connection getModel() {
        return model;
    }

    public void setCancelled(boolean newValue) {
        cancelled = newValue;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private static class SetStartupModeListener implements ActionListener {
        private final ConnectionType mode;
        private final ConnectionSelectionDialog window;

        public SetStartupModeListener(ConnectionSelectionDialog frame, ConnectionType mode) {
            this.mode = mode;
            this.window = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            window.getModel().setType(mode);
            window.setVisible(false);
            window.dispose();
        }
    }

    private static class CancelListener extends WindowAdapter implements ActionListener {

        private final ConnectionSelectionDialog dialog;

        public CancelListener(ConnectionSelectionDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }

        @Override
        public void windowClosing(WindowEvent e) {
            cancel();
        }

        private void cancel() {
            dialog.setCancelled(true);
            dialog.setVisible(false);
            dialog.dispose();
        }

    }
}
