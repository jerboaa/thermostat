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

import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatPasswordField;
import com.redhat.thermostat.client.swing.components.ThermostatTextField;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public abstract class CredentialPanel extends JPanel {
    
    private String titleText;
    private String helpMessage;
    private ComponentTitledBorder titledBorder;
    private JLabel title;

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Icon infoIcon = new FontAwesomeIcon('\uf05a', 15);

    protected JLabel usernameText;
    protected JLabel passwordText;
    protected ThermostatTextField username;
    protected ThermostatPasswordField password;

    public CredentialPanel(String titleText, String helpMessage) {
        this.titleText = titleText;
        this.helpMessage = helpMessage;

        //enable tooltips
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(0);

        initComponents();
    }

    private void initComponents() {
        usernameText = new JLabel(translator.localize(LocaleResources.USERNAME).getContents());
        passwordText = new JLabel(translator.localize(LocaleResources.PASSWORD).getContents());
        username = new ThermostatTextField();
        password = new ThermostatPasswordField();
        password.setCutCopyEnabled(true);

        title = new JLabel(titleText, infoIcon, JLabel.CENTER);
        title.setHorizontalTextPosition(JLabel.LEFT);
        title.setIconTextGap(20);
        title.setOpaque(true);

        titledBorder = new ComponentTitledBorder(title, BorderFactory.createEtchedBorder());
        setBorder(titledBorder);
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

    public char[] getPassword() {
        return password.getPassword();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (titledBorder.getTitleRect().contains(e.getPoint())) {
            return helpMessage;
        } else {
            return null;
        }
    }

    public class ComponentTitledBorder implements Border {
        private static final int OFFSET = 5;
        private final Component comp;
        private Rectangle rect;
        private Border border;

        public ComponentTitledBorder(Component comp, Border border) {
            this.comp = comp;
            this.border = border;
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Insets borderInsets = border.getBorderInsets(c);
            Insets insets = getBorderInsets(c);
            int temp = (insets.top - borderInsets.top) / 2;
            border.paintBorder(c, g, x, y + temp, width, height - temp);
            Dimension size = comp.getPreferredSize();
            rect = new Rectangle(OFFSET, 0, size.width, size.height);
            SwingUtilities.paintComponent(g, comp, (Container) c, rect);
        }

        public Insets getBorderInsets(Component c) {
            Dimension size = comp.getPreferredSize();
            Insets insets = border.getBorderInsets(c);
            insets.top = Math.max(insets.top, size.height);
            return insets;
        }

        public Rectangle getTitleRect() {
            return rect;
        }
    }
}
