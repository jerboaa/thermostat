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

package com.redhat.thermostat.client.swing.internal.views;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

@SuppressWarnings("serial")
class ClientConfigurationPanel extends JPanel {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    final JTextField storageUrl = new JTextField();
    final JTextField userName = new JTextField();
    final JPasswordField password = new JPasswordField();
    
    final JCheckBox saveEntitlements;
    
    public ClientConfigurationPanel() {
        setBorder(new TitledBorder(null,
                  translator.localize(LocaleResources.CLIENT_PREFS_CONNECTION).getContents(),
                  TitledBorder.LEFT, TitledBorder.TOP, null, null));

        JLabel storageURLText = new JLabel(translator.localize(LocaleResources.CLIENT_PREFS_STORAGE_URL).getContents());
        storageURLText.setName("");
        
        storageUrl.setColumns(10);
        storageUrl.setName("connectionUrl");
        
        JLabel userNameText = new JLabel(translator.localize(LocaleResources.CLIENT_PREFS_STORAGE_USERNAME).getContents());
        userNameText.setName("userNameText");
        
        userName.setName("username");
        userName.setColumns(10);
        
        JLabel passowrdText = new JLabel("Password");
        passowrdText.setName("passwordText");
        
        password.setName("password");
        password.setColumns(10);
        
        saveEntitlements = new JCheckBox(translator.localize(LocaleResources.CLIENT_PREFS_STORAGE_SAVE_ENTITLEMENTS).getContents());
        saveEntitlements.setName("saveEntitlements");
        saveEntitlements.setSelected(false);

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(saveEntitlements)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(storageURLText, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                    .addComponent(passowrdText, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(userNameText, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(userName, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                                .addComponent(storageUrl, GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                                .addComponent(password, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE))))
                    .addGap(24))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(storageURLText)
                        .addComponent(storageUrl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(8)
                            .addComponent(userNameText))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(userName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(8)
                            .addComponent(passowrdText))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(password, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(saveEntitlements)
                    .addContainerGap(15, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
    }
}

