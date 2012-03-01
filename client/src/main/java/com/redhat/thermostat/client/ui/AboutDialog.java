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

import static com.redhat.thermostat.client.Translate.localize;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.redhat.thermostat.client.ApplicationInfo;

public class AboutDialog extends JDialog {

    private static final long serialVersionUID = -2459715525658426058L;

    private static final Border emptySpace = BorderFactory.createEmptyBorder(10, 10, 10, 10);

    private final ApplicationInfo appInfo;

    public AboutDialog(ApplicationInfo info) {
        this.appInfo = info;
        setupUi();
    }

    private void setupUi() {

        String name = appInfo.getName();
        String description = appInfo.getDescription();
        String version = appInfo.getVersion();
        Icon icon = IconResource.QUESTION.getIcon(); // TODO appInfo.getIcon();
        String releaseDate = appInfo.getReleaseDate();
        String copyright = appInfo.getCopyright();
        String license = appInfo.getLicenseSummary();
        String email = appInfo.getEmail();
        String website = appInfo.getWebsite();

        JPanel iconContainer = new JPanel(new BorderLayout());
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(emptySpace);
        iconContainer.add(iconLabel);

        add(iconContainer, BorderLayout.LINE_START);

        JPanel descriptionContainer = new JPanel();
        descriptionContainer.setLayout(new BoxLayout(descriptionContainer, BoxLayout.PAGE_AXIS));
        descriptionContainer.setBorder(emptySpace);

        descriptionContainer.add(Box.createGlue());
        descriptionContainer.add(new JLabel(new HtmlTextBuilder().larger(name).toHtml()));
        descriptionContainer.add(new JLabel(localize("ABOUT_DIALOG_VERSION_AND_RELEASE", version, releaseDate)));
        descriptionContainer.add(new JLabel(description));
        descriptionContainer.add(new JLabel(copyright));
        descriptionContainer.add(new JLabel(localize("ABOUT_DIALOG_LICENSE", license)));
        descriptionContainer.add(new JLabel(localize("ABOUT_DIALOG_EMAIL", email)));
        JLabel websiteLink = new JLabel(localize("ABOUT_DIALOG_WEBSITE", website));
        descriptionContainer.add(websiteLink);
        descriptionContainer.add(Box.createGlue());

        add(descriptionContainer, BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel();
        JButton closeButton = new JButton(localize("BUTTON_CLOSE"));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog.this.dispose();
            }
        });
        buttonContainer.add(closeButton);
        add(buttonContainer, BorderLayout.PAGE_END);
    }

}
