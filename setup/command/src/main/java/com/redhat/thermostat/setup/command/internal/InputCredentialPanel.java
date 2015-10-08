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

import com.redhat.thermostat.client.swing.components.ThermostatPasswordField;
import com.redhat.thermostat.setup.command.internal.model.CredentialGenerator;
import com.redhat.thermostat.setup.command.internal.model.UserCredsValidator;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class InputCredentialPanel extends CredentialPanel {

    private JLabel passwordConfirmText;
    private ThermostatPasswordField passwordConfirm;
    private JPanel messagePanel;
    private JLabel errorMessage;
    private JCheckBox showPasswordCheckbox;
    private JButton useDefaultsBtn;
    private CredentialGenerator credentialGenerator;

    private static final UserCredsValidator validator = new UserCredsValidator();
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    public InputCredentialPanel(String titleText, String helpMessage, String defaultUsernamePrefix) {
        super(titleText, helpMessage);

        credentialGenerator = new CredentialGenerator(defaultUsernamePrefix);

        initComponents();
    }

    private void initComponents() {
        showPasswordCheckbox = new JCheckBox(translator.localize(LocaleResources.SHOW_PASSWORDS).getContents());
        showPasswordCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showPassword(showPasswordCheckbox.isSelected());
            }
        });
        showPasswordCheckbox.setSelected(false);

        useDefaultsBtn = new JButton(translator.localize(LocaleResources.USE_DEFAULTS).getContents());
        useDefaultsBtn.setPreferredSize(new Dimension(140, 30));
        useDefaultsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String defaultUsername = credentialGenerator.generateRandomUsername();
                char[] defaultPassword = credentialGenerator.generateRandomPassword();

                username.setText(defaultUsername);
                password.setText(String.valueOf(defaultPassword));
                passwordConfirm.setText(String.valueOf(defaultPassword));
                removeErrorMessage();
            }
        });

        passwordConfirmText = new JLabel(translator.localize(LocaleResources.VERIFY_PASSWORD).getContents());
        passwordConfirm = new ThermostatPasswordField();
        passwordConfirm.setCutCopyEnabled(true);

        errorMessage = new JLabel();
        errorMessage.setForeground(Color.RED);
        errorMessage.setHorizontalAlignment(SwingConstants.CENTER);

        messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(errorMessage, BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(25, 25, 25)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(passwordText)
                                .addComponent(usernameText)
                                .addComponent(passwordConfirmText))
                            .addGap(65, 65, 65)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(showPasswordCheckbox)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                                    .addComponent(useDefaultsBtn))
                                .addComponent(passwordConfirm, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(password)
                                .addComponent(username))))
                    .addContainerGap())
                .addComponent(messagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(usernameText)
                        .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(passwordText)
                        .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(passwordConfirmText)
                        .addComponent(passwordConfirm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(showPasswordCheckbox)
                        .addComponent(useDefaultsBtn))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(messagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0))
        );
    }

    public JTextField getUsernameField() {
        return username;
    }

    public String getUsername() {
        return username.getText();
    }

    public JTextField getPasswordField() {
        return password;
    }

    public JTextField getPasswordConfirmField() {
        return passwordConfirm;
    }

    public char[] getPassword() {
        return password.getPassword();
    }

    private void showPassword(boolean isVisible) {
        if (isVisible) {
            password.setEchoChar((char) 0);
            passwordConfirm.setEchoChar((char) 0);
        } else {
            password.setEchoChar('*');
            passwordConfirm.setEchoChar('*');
        }
    }

    public void removeErrorMessage() {
        errorMessage.setText(null);
        this.revalidate();
        this.repaint();
    }

    public void showPasswordMismatch() {
        errorMessage.setText(translator.localize(LocaleResources.PASSWORD_MISMATCH).getContents());
        this.revalidate();
        this.repaint();
    }

    public void showDetailsMissing() {
        errorMessage.setText(translator.localize(LocaleResources.DETAILS_MISSING).getContents());
        this.revalidate();
        this.repaint();
    }

    public void setErrorMessage(String message) {
        errorMessage.setText(message);
        this.revalidate();
        this.repaint();
    }

    public boolean isInputValid() {
        //ensure credentials are not empty
        try {
            validator.validateUsername(username.getText());
            validator.validatePassword(password.getPassword());
            validator.validatePassword(passwordConfirm.getPassword());
        } catch (IllegalArgumentException e) {
            showDetailsMissing();
            return false;
        }

        //ensure passwords match
        if (!Arrays.equals(password.getPassword(), passwordConfirm.getPassword())) {
            showPasswordMismatch();
            return false;
        }

        removeErrorMessage();
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        usernameText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        passwordConfirmText.setEnabled(enabled);
        username.setEnabled(enabled);
        password.setEnabled(enabled);
        passwordConfirm.setEnabled(enabled);
        showPasswordCheckbox.setEnabled(enabled);
        useDefaultsBtn.setEnabled(enabled);
    }
}
