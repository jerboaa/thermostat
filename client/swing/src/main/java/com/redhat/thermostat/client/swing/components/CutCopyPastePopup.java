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

import com.redhat.thermostat.client.swing.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import javax.swing.JMenuItem;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class CutCopyPastePopup extends ThermostatPopupMenu {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();
    private JMenuItem cut;
    private JMenuItem copy;
    private JMenuItem paste;

    public CutCopyPastePopup(JTextComponent parent) {
        this();
        boolean enableCopy = parent.isEnabled();
        boolean enableMutators = parent.isEnabled() && parent.isEditable();
        setCutEnabled(enableMutators);
        setCopyEnabled(enableCopy);
        setPasteEnabled(enableMutators);
    }

    public CutCopyPastePopup() {
        cut = new JMenuItem(new DefaultEditorKit.CutAction());
        copy = new JMenuItem(new DefaultEditorKit.CopyAction());
        paste = new JMenuItem(new DefaultEditorKit.PasteAction());

        cut.setText(translate.localize(LocaleResources.CUT).getContents());
        copy.setText(translate.localize(LocaleResources.COPY).getContents());
        paste.setText(translate.localize(LocaleResources.PASTE).getContents());

        this.add(cut);
        this.add(copy);
        this.add(paste);

        setCutEnabled(true);
        setCopyEnabled(true);
        setPasteEnabled(true);
    }

    public static CutCopyPastePopup getCopyOnlyMenu() {
        CutCopyPastePopup menu = new CutCopyPastePopup();
        menu.setCutEnabled(false);
        menu.setCopyEnabled(true);
        menu.setPasteEnabled(false);
        return menu;
    }

    public static CutCopyPastePopup getPasteOnlyMenu() {
        CutCopyPastePopup menu = new CutCopyPastePopup();
        menu.setCutEnabled(false);
        menu.setCopyEnabled(false);
        menu.setPasteEnabled(true);
        return menu;
    }

    public static CutCopyPastePopup getDisabledMenu() {
        CutCopyPastePopup menu = new CutCopyPastePopup();
        menu.setCutEnabled(false);
        menu.setCopyEnabled(false);
        menu.setPasteEnabled(false);
        menu.setEnabled(false);
        return menu;
    }

    public void setCutEnabled(boolean enabled) {
        cut.setEnabled(enabled);
    }

    public void setCopyEnabled(boolean enabled) {
        copy.setEnabled(enabled);
    }

    public void setPasteEnabled(boolean enabled) {
        paste.setEnabled(enabled);
    }

    public boolean isCutEnabled() {
        return cut.isEnabled();
    }

    public boolean isCopyEnabled() {
        return copy.isEnabled();
    }

    public boolean isPasteEnabled() {
        return paste.isEnabled();
    }

}
