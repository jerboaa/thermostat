/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;
import com.redhat.thermostat.shared.locale.LocalizedString;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 */
class TabUI extends JComponent {

    private UIDefaults defaults = UIDefaultsImpl.getInstance();
    private LocalizedString name;
    protected TabModel model;
    private ShadowLabel label;

    TabUI(LocalizedString name) {
        this.name = name;
        model = new TabModel(this);

        setBorder(new EmptyBorder(5, 5, 5, 5));

        setLayout(new BorderLayout());
        label = new ShadowLabel(name);
        label.setFont(defaults.getDefaultFont());
        label.setForeground((Color) defaults.getSelectedComponentBGColor());
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label);
    }

    public LocalizedString getTabName() {
        return name;
    }

    TabModel getModel() {
        return model;
    }

    ShadowLabel getTitle() {
        return label;
    }

    @Override
    protected void paintComponent(Graphics g) {

        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);

        if (getModel().isSelected()) {
            graphics.setPaint(defaults.getSelectedComponentBGColor());
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);

        } else if (getModel().isHover()) {
            graphics.setPaint(defaults.getDecorationIconColor());
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
        }

        graphics.dispose();
    }
}
