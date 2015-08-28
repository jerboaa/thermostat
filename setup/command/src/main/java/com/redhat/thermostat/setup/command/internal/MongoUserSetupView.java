/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.setup.command.internal;

import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.net.URL;

public class MongoUserSetupView extends JPanel implements SetupView {

    private JButton backBtn;
    private JButton nextBtn;
    private JButton cancelBtn;

    private JPanel toolbar;
    private JPanel midPanel;
    private CredentialPanel credentialPanel;

    private static final String THERMOSTAT_LOGO = "thermostat.png";
    private static final String PROGRESS = "Step 2 of 3";
    private static final String DEFAULT_STORAGE_USER = "mongodevuser";
    private static final char[] DEFAULT_STORAGE_PASSWORD = new char[] {'m', 'o', 'n', 'g', 'o', 'd', 'e', 'v', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    public MongoUserSetupView(LayoutManager layout) {
        super(layout);

        createMidPanel();
        createToolbarPanel();

        this.add(midPanel, BorderLayout.CENTER);
        this.add(toolbar, BorderLayout.SOUTH);
    }

    @Override
    public void setTitleAndProgress(JLabel title, JLabel progress) {
        title.setText(translator.localize(LocaleResources.MONGO_SETUP_TITLE).getContents());
        progress.setText(PROGRESS);
    }

    private void createMidPanel() {
        credentialPanel = new CredentialPanel(
            translator.localize(LocaleResources.MONGO_CRED_TITLE).getContents(),
            translator.localize(LocaleResources.STORAGE_HELP_INFO).getContents(),
            DEFAULT_STORAGE_USER,
            DEFAULT_STORAGE_PASSWORD);
        DocumentListener inputValidator = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateInput();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateInput();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateInput();
            }
        };
        credentialPanel.getUsernameField().getDocument().addDocumentListener(inputValidator);
        credentialPanel.getPasswordField1().getDocument().addDocumentListener(inputValidator);
        credentialPanel.getPasswordField2().getDocument().addDocumentListener(inputValidator);

        midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.PAGE_AXIS));
        midPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        midPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        midPanel.add(credentialPanel);
        midPanel.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private void createToolbarPanel() {
        URL logoURL = SetupWindow.class.getClassLoader().getResource(THERMOSTAT_LOGO);
        JLabel thermostatLogo = new JLabel(new ImageIcon(logoURL));

        backBtn = new JButton(translator.localize(LocaleResources.BACK).getContents());
        backBtn.setPreferredSize(new Dimension(70, 30));
        nextBtn = new JButton(translator.localize(LocaleResources.NEXT).getContents());
        nextBtn.setPreferredSize(new Dimension(70, 30));
        nextBtn.setEnabled(false);
        cancelBtn = new JButton(translator.localize(LocaleResources.CANCEL).getContents());
        cancelBtn.setPreferredSize(new Dimension(70, 30));

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        toolbar.add(thermostatLogo);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(backBtn);
        toolbar.add(nextBtn);
        toolbar.add(cancelBtn);
    }

    private void validateInput() {
        if (credentialPanel.isInputValid()) {
            nextBtn.setEnabled(true);
        } else {
            nextBtn.setEnabled(false);
        }
    }

    public void enableButtons() {
        backBtn.setEnabled(true);
        nextBtn.setEnabled(true);
        credentialPanel.setEnabled(true);
    }

    public void disableButtons() {
        backBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        credentialPanel.setEnabled(false);
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    public JButton getBackBtn() {
        return backBtn;
    }

    public JButton getNextBtn() {
        return nextBtn;
    }

    public JButton getCancelBtn() {
        return cancelBtn;
    }

    public String getUsername() {
        return credentialPanel.getUsername();
    }

    public char[] getPassword() {
        return credentialPanel.getPassword();
    }
}
