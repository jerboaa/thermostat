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

import javax.swing.JPasswordField;
import javax.swing.text.Document;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ThermostatPasswordField extends JPasswordField implements ThermostatTextComponent {

    private CutCopyPastePopup contextMenu = CutCopyPastePopup.getPasteOnlyMenu();
    {
        this.setComponentPopupMenu(contextMenu);
        addPropertyChangeListener("JPasswordField.cutCopyAllowed", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                boolean cutCopyEnabled = (Boolean) propertyChangeEvent.getNewValue();
                contextMenu.setCutEnabled(cutCopyEnabled && isEnabled() && isEditable());
                contextMenu.setCopyEnabled(cutCopyEnabled && isEnabled());
            }
        });
    }

    public ThermostatPasswordField() {
        super();
    }

    public ThermostatPasswordField(String text) {
        super(text);
    }

    public ThermostatPasswordField(int columns) {
        super(columns);
    }

    public ThermostatPasswordField(String text, int columns) {
        super(text, columns);
    }

    public ThermostatPasswordField(Document doc, String txt, int columns) {
        super(doc, txt, columns);
    }

    @Override
    public CutCopyPastePopup getContextMenu() {
        return contextMenu;
    }

    @Override
    public void setEditable(boolean b) {
        super.setEditable(b);
        if (contextMenu != null) {
            contextMenu.setCutEnabled(b && isEnabled() && isCutCopyEnabled());
            contextMenu.setPasteEnabled(b && isEnabled());
        }
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (contextMenu != null) {
            contextMenu.setCutEnabled(b && isEditable() &&  isCutCopyEnabled());
            contextMenu.setCopyEnabled(b && isCutCopyEnabled());
            contextMenu.setPasteEnabled(b && isEditable());
            contextMenu.setEnabled(b);
        }
    }

    public void setCutCopyEnabled(boolean b) {
        this.putClientProperty("JPasswordField.cutCopyAllowed", b);
    }

    public boolean isCutCopyEnabled() {
        Boolean enabled = (Boolean) getClientProperty("JPasswordField.cutCopyAllowed");
        return enabled != null && enabled;
    }

}
