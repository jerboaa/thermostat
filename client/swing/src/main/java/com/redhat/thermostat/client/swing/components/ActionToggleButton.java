/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import com.redhat.thermostat.client.core.ToggleActionState;
import com.redhat.thermostat.shared.locale.LocalizedString;

import java.util.Objects;

@SuppressWarnings("serial")
public class ActionToggleButton extends JToggleButton implements ToolbarButton {

    private static final ToggleActionState DEFAULT_TOGGLE_ACTION_STATE = new DefaultToggleActionState();
        
    private String lastText;
    private boolean showText;
    private ActionToggleButtonUI buttonUI;

    public ActionToggleButton(final Icon icon) {
        this(icon, LocalizedString.EMPTY_STRING);
    }
    
    public ActionToggleButton(final Icon icon, LocalizedString text) {
        this(icon, icon, text);
    }

    public ActionToggleButton(final Icon defaultStateIcon, final Icon selectedStateIcon, LocalizedString text) {
        super();

        setIcon(defaultStateIcon);
        setSelectedIcon(selectedStateIcon);

        showText = true;
        setText(text.getContents());

        buttonUI = new ActionToggleButtonUI(DEFAULT_TOGGLE_ACTION_STATE);
        setUI(buttonUI);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(new ToolbarButtonBorder(this));
        setToggleActionState(DEFAULT_TOGGLE_ACTION_STATE);
    }
    
    @Override
    public AbstractButton getToolbarButton() {
        return this;
    }
    
    @Override
    public void setText(String text) {
        lastText = text;
        if (showText) {
            super.setText(text);
        }
    }
    
    private void setText_noClient(String text) {
        super.setText(text);
    }
    
    @Override
    public void toggleText(boolean showText) {
        this.showText = showText;
        if (showText) {
            setText_noClient(lastText);
        } else {
            setText_noClient("");
        }
    }

    public void setToggleActionState(ToggleActionState toggleActionState) {
        Objects.requireNonNull(toggleActionState);
        setEnabled(!toggleActionState.isTransitionState() && toggleActionState.isButtonEnabled());
        setSelected(toggleActionState.isActionEnabled());
        buttonUI.setState(toggleActionState);
        repaint();
    }

    private static class DefaultToggleActionState implements ToggleActionState {
        @Override
        public boolean isTransitionState() {
            return false;
        }

        @Override
        public boolean isActionEnabled() {
            return false;
        }

        @Override
        public boolean isButtonEnabled() {
            return true;
        }
    }

}

