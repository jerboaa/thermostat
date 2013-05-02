/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.UIResources;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class AboutDialog extends JDialog {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final long serialVersionUID = -7611616871710076514L;

    private static final Logger logger = LoggingUtils.getLogger(AboutDialog.class);

    private String description;
    private String version;
    private Icon icon;
    private String copyright;
    private String license;
    private String website;
    private String email;
    
    /**
     * Create the dialog.
     * @param applicationInfo 
     */
    public AboutDialog(ApplicationInfo appInfo) {
       
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        description = appInfo.getDescription();
        version = appInfo.getVersion().getVersionNumber();
        copyright = appInfo.getCopyright();
        license = appInfo.getLicenseSummary();
        website = appInfo.getWebsite();
        email = appInfo.getEmail();
        
        initComponents();
    }

    private void initComponents() {
        setBounds(100, 100, 450, 338);
        
        UIResources res = UIResources.getInstance();
        icon = new com.redhat.thermostat.client.swing.components.Icon(res.getLogo());
        
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(""));
        
        JButton closeButton = new JButton(translator.localize(LocaleResources.BUTTON_CLOSE).getContents());
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AboutDialog.this.setVisible(false);
                AboutDialog.this.dispose();  
            }
        });
        
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(closeButton, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE)
                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panel, GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                    .addGap(18)
                    .addComponent(closeButton)
                    .addGap(9))
        );
        
        JLabel iconLabel = new JLabel("");
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        iconLabel.setIcon(icon);
        
        JLabel versionLabel = new JLabel(version);
        versionLabel.setFont(res.footerFont());
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descriptionLabel.setFont(res.standardFont());
        
        JLabel homePageLabel = new JLabel(website);
        homePageLabel.setForeground(res.hyperlinkColor());
        homePageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        homePageLabel.setFont(res.footerFont());        
        homePageLabel.addMouseListener(new Browse(homePageLabel));

        JLabel copyrightLabel = new JLabel(copyright);
        copyrightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        copyrightLabel.setFont(res.footerFont());
        
        JLabel licenseString = new JLabel(license);
        licenseString.setHorizontalAlignment(SwingConstants.CENTER);
        licenseString.setFont(res.footerFont());
        
        JLabel emailLabel = new JLabel(email);
        emailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emailLabel.setForeground(res.hyperlinkColor());
        emailLabel.setFont(res.footerFont());
        emailLabel.addMouseListener(new Mailer(emailLabel));
        
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
            gl_panel.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addComponent(iconLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(versionLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(descriptionLabel, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(copyrightLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(licenseString, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(emailLabel, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                        .addComponent(homePageLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
                    .addContainerGap())
        );
        gl_panel.setVerticalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(iconLabel)
                    .addGap(4)
                    .addComponent(versionLabel, GroupLayout.DEFAULT_SIZE, 13, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(descriptionLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(12)
                    .addComponent(homePageLabel, GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                    .addGap(3)
                    .addComponent(emailLabel, GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(copyrightLabel, GroupLayout.DEFAULT_SIZE, 13, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(licenseString, GroupLayout.DEFAULT_SIZE, 13, Short.MAX_VALUE)
                    .addContainerGap())
        );
        panel.setLayout(gl_panel);
        getContentPane().setLayout(groupLayout);
    }
    
    private abstract class HyperLinkAction extends MouseAdapter {
        
        private JLabel hyperLinkLabel;
        public HyperLinkAction(JLabel hyperLinkLabel) {
            this.hyperLinkLabel = hyperLinkLabel;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            hyperLinkLabel.setForeground(UIResources.getInstance().hyperlinkActiveColor());
            Cursor cursor = new Cursor(Cursor.HAND_CURSOR);
            setCursor(cursor);
        }
        @Override
        public void mouseExited(MouseEvent e) {
            hyperLinkLabel.setForeground(UIResources.getInstance().hyperlinkColor());
            Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);
            setCursor(cursor);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (Desktop.isDesktopSupported()) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        doAction();
                        return null;
                    }
                    @Override
                    protected void done() {
                        hyperLinkLabel.setForeground(UIResources.getInstance().hyperlinkColor());
                    }
                }.execute();
            }
        }
        
        protected abstract void doAction();
    }
    
    private class Mailer extends HyperLinkAction {
        public Mailer(JLabel hyperLinkLabel) {
            super(hyperLinkLabel);
        }

        @Override
        protected void doAction() {
            try {
                Desktop.getDesktop().mail(new URI("mailto:" + email));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Cannot send mail to Thermosat mail", ex);
            }
        }
    }
    
    private class Browse extends HyperLinkAction {
        public Browse(JLabel hyperLinkLabel) {
            super(hyperLinkLabel);
        }
        
        @Override
        protected void doAction() {
            try {
                Desktop.getDesktop().browse(new URI(website));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Cannot open Thermostat website URL", ex);
            }
        }
    }
}

