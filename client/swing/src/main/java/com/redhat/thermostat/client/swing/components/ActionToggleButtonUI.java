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

package com.redhat.thermostat.client.swing.components;

import com.redhat.thermostat.client.core.ToggleActionState;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.GrayFilter;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * An ActionButtonUI which is intended specifically for "togglebuttons", which
 * are defined to have exactly 4 possible states: conceptually, they are
 * STARTING, STARTED, STOPPING, and STOPPED. The initial state is STOPPED.
 * Each state is represented by a pairing of "button enabled" and
 * "button selected" states:
 *
 * STOPPED: enabled, not selected
 * STARTING: disabled, selected
 * STARTED: enabled, selected
 * STOPPING: disabled, not selected
 *
 * @see ActionToggleButton
 * @see ToggleActionState
 */
public class ActionToggleButtonUI extends ActionButtonUI {

    protected BufferedImage selectedIcon;
    protected Image rolledOverSelectedIcon;
    protected Image disabledSelectedIcon;
    private ToggleActionState toggleActionState;

    ActionToggleButtonUI(ToggleActionState initialState) {
        this.toggleActionState = Objects.requireNonNull(initialState);
    }

    @Override
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();

        javax.swing.Icon icon = button.getIcon();
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        if (sourceIcon == null) {
            sourceIcon = new BufferedImage(w + 1, h + 1,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics imageGraphics = sourceIcon.getGraphics();
            icon.paintIcon(null, imageGraphics, 0, 0);
        }

        if (rollOverIcon == null) {
            rollOverIcon = getBrighterImage(sourceIcon);
        }

        if (disabledIcon == null) {
            disabledIcon = GrayFilter.createDisabledImage(sourceIcon);
        }

        if (selectedIcon == null) {
            selectedIcon = new BufferedImage(w + 1, h + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics imageGraphics = selectedIcon.getGraphics();
            button.getSelectedIcon().paintIcon(null, imageGraphics, 0, 0);
        }

        if (rolledOverSelectedIcon == null) {
            rolledOverSelectedIcon = getBrighterImage(selectedIcon);
        }

        if (disabledSelectedIcon == null) {
            disabledSelectedIcon = GrayFilter.createDisabledImage(selectedIcon);
        }

        int x = 3;
        int y = button.getHeight() / 2 - h / 2;

        String text = button.getText();
        if (text == null || text.equals("")) {
            x = button.getWidth() / 2 - w / 2;
        }

        boolean transitionState = toggleActionState.isTransitionState();
        boolean actionEnabled = toggleActionState.isActionEnabled();
        boolean buttonEnabled = toggleActionState.isButtonEnabled();

        boolean stopped = !transitionState && !actionEnabled;
        boolean starting = transitionState && actionEnabled;
        boolean started = !transitionState && actionEnabled;
        boolean stopping = transitionState && !actionEnabled;

        if (!buttonEnabled) {
            g.drawImage(disabledIcon, x, y, null);
        } else if (stopped) {
            if (model.isRollover()) {
                g.drawImage(rollOverIcon, x, y, null);
            } else {
                g.drawImage(sourceIcon, x, y, null);
            }
        } else if (starting) {
            g.drawImage(disabledIcon, x, y, null);
        } else if (started) {
            if (model.isRollover()) {
                g.drawImage(rolledOverSelectedIcon, x, y, null);
            } else {
                g.drawImage(selectedIcon, x, y, null);
            }
        } else if (stopping) {
            g.drawImage(disabledSelectedIcon, x, y, null);
        }
    }

    void setState(ToggleActionState toggleActionState) {
        this.toggleActionState = Objects.requireNonNull(toggleActionState);
    }

}
