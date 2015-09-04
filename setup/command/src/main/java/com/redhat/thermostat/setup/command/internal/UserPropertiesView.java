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

public class UserPropertiesView extends JPanel implements SetupView {

    private JButton finishBtn;
    private JButton backBtn;
    private JButton cancelBtn;
    private JPanel toolbar;
    private JPanel midPanel;
    private InputCredentialPanel clientInfoPanel;
    private InputCredentialPanel agentInfoPanel;

    private static final String THERMOSTAT_LOGO = "thermostat.png";
    private static final String PROGRESS = "Step 3 of 3";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    public UserPropertiesView(LayoutManager layout) {
        super(layout);

        createMidPanel();
        createToolbarPanel();

        this.add(midPanel, BorderLayout.CENTER);
        this.add(toolbar, BorderLayout.SOUTH);

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
        clientInfoPanel.getUsernameField().getDocument().addDocumentListener(inputValidator);
        clientInfoPanel.getPasswordField().getDocument().addDocumentListener(inputValidator);
        clientInfoPanel.getPasswordConfirmField().getDocument().addDocumentListener(inputValidator);
        agentInfoPanel.getUsernameField().getDocument().addDocumentListener(inputValidator);
        agentInfoPanel.getPasswordField().getDocument().addDocumentListener(inputValidator);
        agentInfoPanel.getPasswordConfirmField().getDocument().addDocumentListener(inputValidator);
    }

    @Override
    public void setTitle(JLabel title) {
        title.setText(translator.localize(LocaleResources.USERS_SETUP_TITLE).getContents());
    }

    @Override
    public void setProgress(JLabel progress) {
        progress.setText(PROGRESS);
    }

    public void createMidPanel() {
        clientInfoPanel = new InputCredentialPanel(
            translator.localize(LocaleResources.CLIENT_CRED_TITLE).getContents(),
            translator.localize(LocaleResources.CLIENT_HELP_INFO).getContents(),
            translator.localize(LocaleResources.CLIENT_USER_PREFIX).getContents());
        agentInfoPanel = new InputCredentialPanel(
            translator.localize(LocaleResources.AGENT_CRED_TITLE).getContents(),
            translator.localize(LocaleResources.AGENT_HELP_INFO).getContents(),
            translator.localize(LocaleResources.AGENT_USER_PREFIX).getContents());

        midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.PAGE_AXIS));
        midPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        midPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        midPanel.add(clientInfoPanel);
        midPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        midPanel.add(agentInfoPanel);
    }

    private void createToolbarPanel() {
        URL logoURL = SetupWindow.class.getClassLoader().getResource(THERMOSTAT_LOGO);
        JLabel thermostatLogo = new JLabel(new ImageIcon(logoURL));

        backBtn = new JButton(translator.localize(LocaleResources.BACK).getContents());
        backBtn.setPreferredSize(new Dimension(70, 30));
        finishBtn = new JButton(translator.localize(LocaleResources.FINISH).getContents());
        finishBtn.setPreferredSize(new Dimension(70, 30));
        finishBtn.setEnabled(false);
        cancelBtn = new JButton(translator.localize(LocaleResources.CANCEL).getContents());
        cancelBtn.setPreferredSize(new Dimension(70, 30));

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        toolbar.add(thermostatLogo);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(backBtn);
        toolbar.add(finishBtn);
        toolbar.add(cancelBtn);
    }

    private void validateInput() {
        if (clientInfoPanel.isInputValid() && agentInfoPanel.isInputValid()) {
            finishBtn.setEnabled(true);
        } else {
            finishBtn.setEnabled(false);
        }
    }

    public void enableButtons() {
        backBtn.setEnabled(true);
        finishBtn.setEnabled(true);
        agentInfoPanel.setEnabled(true);
        clientInfoPanel.setEnabled(true);
    }

    public void disableButtons() {
        backBtn.setEnabled(false);
        finishBtn.setEnabled(false);
        agentInfoPanel.setEnabled(false);
        clientInfoPanel.setEnabled(false);
    }

    @Override
    public void setDefaultButton() {
        getRootPane().setDefaultButton(finishBtn);
    }

    @Override
    public void focusInitialComponent() {
        clientInfoPanel.getUsernameField().requestFocusInWindow();
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    public JButton getBackBtn() {
        return backBtn;
    }

    public JButton getFinishBtn() {
        return finishBtn;
    }

    public JButton getCancelBtn() {
        return cancelBtn;
    }

    public String getAgentUsername() {
        return agentInfoPanel.getUsername();
    }

    public char[] getAgentPassword() {
        return agentInfoPanel.getPassword();
    }

    public String getClientUsername() {
        return clientInfoPanel.getUsername();
    }

    public char[] getClientPassword() {
        return clientInfoPanel.getPassword();
    }

}
