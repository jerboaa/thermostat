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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

public class ClientConfigurationFrame extends JFrame implements ClientConfigurationView {

    private static final long serialVersionUID = 6888957994092403516L;

    private final ConfigurationCompleteListener configurationCompleteListener;
    private final WindowClosingListener windowClosingListener;

    private final JTextField storageUrl;
    private final JButton btnOk;
    private final JButton btnCancel;

    private CopyOnWriteArrayList<ActionListener<Action>> listeners = new CopyOnWriteArrayList<>();

    public ClientConfigurationFrame() {
        configurationCompleteListener = new ConfigurationCompleteListener();
        windowClosingListener = new WindowClosingListener();

        setTitle(localize(LocaleResources.CLIENT_PREFS_WINDOW_TITLE));
        addWindowListener(windowClosingListener);

        btnOk = new JButton(localize(LocaleResources.BUTTON_OK));
        btnOk.addActionListener(configurationCompleteListener);
        btnOk.setName("ok");
        btnCancel = new JButton(localize(LocaleResources.BUTTON_CANCEL));
        btnCancel.addActionListener(configurationCompleteListener);
        btnCancel.setName("cancel");
        JLabel lblClientConfiguration = new JLabel(localize(LocaleResources.CLIENT_PREFS_GENERAL));

        JLabel lblStorageUrl = new JLabel(localize(LocaleResources.CLIENT_PREFS_STORAGE_URL));

        storageUrl = new JTextField();
        storageUrl.setColumns(10);
        storageUrl.setName("connectionUrl");

        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                    .addContainerGap(251, Short.MAX_VALUE)
                    .addComponent(btnCancel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(btnOk)
                    .addContainerGap())
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(lblStorageUrl)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(storageUrl, GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE))
                        .addComponent(lblClientConfiguration))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblClientConfiguration)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblStorageUrl)
                        .addComponent(storageUrl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED, 185, Short.MAX_VALUE)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnOk)
                        .addComponent(btnCancel))
                    .addContainerGap())
        );
        getContentPane().setLayout(groupLayout);
    }

    @Override
    public void showDialog() {
        assertInEDT();
        this.pack();
        this.setVisible(true);
    }

    @Override
    public void hideDialog() {
        assertInEDT();

        this.setVisible(false);
        this.dispose();
    }

    @Override
    public String getConnectionUrl() {
        assertInEDT();
        return storageUrl.getText();
    }

    @Override
    public void setConnectionUrl(final String url) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                storageUrl.setText(url);
            }
        });
    }

    @Override
    public void addListener(ActionListener<Action> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ActionListener<Action> listener) {
        listeners.remove(listener);
    }

    private void fireAction(ActionEvent<Action> actionEvent) {
        for (ActionListener<Action> listener: listeners) {
            listener.actionPerformed(actionEvent);
        }
    }

    private void assertInEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be invoked in the EDT");
        }
    }

    class ConfigurationCompleteListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (e.getSource() == btnOk) {
                fireAction(new ActionEvent<>(ClientConfigurationFrame.this, Action.CLOSE_ACCEPT));
            } else if (e.getSource() == btnCancel) {
                fireAction(new ActionEvent<>(ClientConfigurationFrame.this, Action.CLOSE_CANCEL));
            }
        }
    }

    class WindowClosingListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            fireAction(new ActionEvent<>(ClientConfigurationFrame.this, Action.CLOSE_CANCEL));
        }
    }
}
