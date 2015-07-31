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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Arrays;

public class MongoUserSetupView extends JPanel implements SetupView {

    private JButton backBtn;
    private JButton defaultSetupBtn;
    private JButton nextBtn;
    private JButton cancelBtn;

    private JTextField usernameField;
    private JPasswordField passwordField1;
    private JPasswordField passwordField2;

    private JPanel toolbar;
    private JPanel midPanel;
    private JPanel detailsPanel;
    private JPanel messagePanel;

    private JLabel usernameLabel;
    private JLabel passwordLabel1;
    private JLabel passwordLabel2;
    private JLabel passwordMismatchLabel;
    private JLabel detailsMissingLabel;
    private JCheckBox showPasswordCheckbox;

    private static final String THERMOSTAT_LOGO = "thermostat.png";
    private static final String PROGRESS = "Step 2 of 3";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    public MongoUserSetupView(LayoutManager layout) {
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
        usernameField.getDocument().addDocumentListener(inputValidator);
        passwordField1.getDocument().addDocumentListener(inputValidator);
        passwordField2.getDocument().addDocumentListener(inputValidator);
    }

    @Override
    public void setTitleAndProgress(JLabel title, JLabel progress) {
        title.setText(translator.localize(LocaleResources.MONGO_SETUP_TITLE).getContents());
        progress.setText(PROGRESS);
    }

    private void createMidPanel() {
        usernameLabel = new JLabel("Username: ");
        usernameField = new JTextField(30);
        passwordLabel1 = new JLabel("Password:");
        passwordField1 = new JPasswordField(30);
        passwordLabel2 = new JLabel("Verify Password:");
        passwordField2 = new JPasswordField(30);
        showPasswordCheckbox = new JCheckBox("Show passwords");
        showPasswordCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                togglePasswords(showPasswordCheckbox.isSelected());
            }
        });

        defaultSetupBtn = new JButton("Use Defaults");
        defaultSetupBtn.setPreferredSize(new Dimension(140, 30));
        passwordMismatchLabel = new JLabel("Passwords don't match!");
        passwordMismatchLabel.setForeground(Color.RED);
        detailsMissingLabel = new JLabel("Please fill in ALL fields");
        detailsMissingLabel.setForeground(Color.RED);
        detailsPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.insets.top = 40;
        detailsPanel.add(usernameLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        detailsPanel.add(usernameField, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.insets.top = 10;
        detailsPanel.add(passwordLabel1, c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        detailsPanel.add(passwordField1, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        detailsPanel.add(passwordLabel2, c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        detailsPanel.add(passwordField2, c);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        detailsPanel.add(showPasswordCheckbox, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        detailsPanel.add(defaultSetupBtn, c);

        messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        midPanel = new JPanel(new BorderLayout());
        midPanel.add(detailsPanel, BorderLayout.NORTH);
        midPanel.add(messagePanel, BorderLayout.SOUTH);
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

    private void togglePasswords(boolean isVisible) {
        if (isVisible) {
            passwordField1.setEchoChar((char) 0);
            passwordField2.setEchoChar((char) 0);
        } else {
            passwordField1.setEchoChar('*');
            passwordField2.setEchoChar('*');
        }
    }

    private void validateInput() {
        if (usernameField.getText().isEmpty() || passwordField1.getPassword().length == 0 || passwordField2.getPassword().length == 0) {
            showDetailsMissing();
            nextBtn.setEnabled(false);
        } else if (!(Arrays.equals(passwordField1.getPassword(), passwordField2.getPassword()))) {
            showPasswordMismatch();
            nextBtn.setEnabled(false);
        } else {
            removeErrorMessages();
            nextBtn.setEnabled(true);
        }
    }

    public void enableButtons() {
        backBtn.setEnabled(true);
        defaultSetupBtn.setEnabled(true);
        nextBtn.setEnabled(true);
    }

    public void disableButtons() {
        backBtn.setEnabled(false);
        defaultSetupBtn.setEnabled(false);
        nextBtn.setEnabled(false);
    }

    private void showPasswordMismatch() {
        messagePanel.removeAll();
        messagePanel.add(passwordMismatchLabel);
        midPanel.revalidate();
        midPanel.repaint();
    }

    private void showDetailsMissing() {
        messagePanel.removeAll();
        messagePanel.add(detailsMissingLabel);
        midPanel.revalidate();
        midPanel.repaint();
    }

    private void removeErrorMessages() {
        messagePanel.removeAll();
        midPanel.revalidate();
        midPanel.repaint();
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    public JButton getBackBtn() {
        return backBtn;
    }

    public JButton getDefaultSetupBtn() {
        return defaultSetupBtn;
    }

    public JButton getNextBtn() {
        return nextBtn;
    }

    public JButton getCancelBtn() {
        return cancelBtn;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public void setUsername(String username) {
        this.usernameField.setText(username);
    }

    public char[] getPassword() {
        return passwordField1.getPassword();
    }

    public void setPassword(String password) {
        this.passwordField1.setText(password);
        this.passwordField2.setText(password);
    }
}
