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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.thread.client.swing.experimental.components.DataPane;
import com.redhat.thermostat.thread.client.swing.experimental.components.Separator;
import java.awt.Color;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 *
 */
class TimelineLabel extends DataPane {
    private LabelField nameLabel;
    private Icon infoOn;
    private Icon infoOff;

    TimelineLabel(UIDefaults defaults, String text) {
        super(Palette.WHITE, Palette.LIGHT_GRAY);

        setBorder(new Separator(defaults, Separator.Side.BOTTOM, Separator.Type.SOLID));

        nameLabel = new LabelField(LocalizedString.EMPTY_STRING);
        nameLabel.setFont(defaults.getDefaultFont().deriveFont(10.f));

        nameLabel.setText(text);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setVerticalAlignment(SwingConstants.CENTER);
        nameLabel.setForeground((Color) defaults.getSelectedComponentBGColor());

        // make same small space around the label, a bit higher left and right
        // to account for the missing icon
        nameLabel.setBorder(new EmptyBorder(2, 4, 2, 4));

        nameLabel.setHorizontalTextPosition(SwingConstants.LEFT);

        // FIXME && TODO: comment this out for now, the functionality this is
        // meant for is not yet implemented in the controller
//        infoOn = new FontAwesomeIcon('\uf05a', 12, Palette.DARK_GRAY.getColor());
//        infoOff = new FontAwesomeIcon('\uf05a', 12, Palette.PALE_GRAY.getColor());
//
//        nameLabel.setIcon(infoOff);

        add(nameLabel);
    }

    public void onMouseHover(boolean hover) {
//        nameLabel.setIcon(hover ? infoOn : infoOff);
        repaint();
    }
}
