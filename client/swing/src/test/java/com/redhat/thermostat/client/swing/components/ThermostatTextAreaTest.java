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

package com.redhat.thermostat.client.swing.components;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class ThermostatTextAreaTest {

    @Test
    public void testComponentPopupMenuIsSet() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu(), is(not(equalTo(null))));
        assertThat(textField.getContextMenu(), is(textField.getComponentPopupMenu()));
    }

    @Test
    public void testMenuEnabledWhenParentEnabled() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isEnabled(), is(true));
        textField.setEnabled(false);
        assertThat(textField.getContextMenu().isEnabled(), is(false));
        textField.setEnabled(true);
        assertThat(textField.getContextMenu().isEnabled(), is(true));
    }

    @Test
    public void testCutDisabledWhenParentDisabled() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
        textField.setEnabled(false);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        textField.setEnabled(true);
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
    }

    @Test
    public void testCopyDisabledWhenParentDisabled() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
        textField.setEnabled(false);
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        textField.setEnabled(true);
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
    }

    @Test
    public void testPasteDisabledWhenParentDisabled() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
        textField.setEnabled(false);
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
        textField.setEnabled(true);
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
    }

    @Test
    public void testCutDisabledWhenParentNotEditable() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
        textField.setEditable(false);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        textField.setEditable(true);
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
    }

    @Test
    public void testPasteDisabledWhenParentNotEditable() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
        textField.setEditable(false);
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
        textField.setEditable(true);
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
    }

    @Test
    public void testCopyNotDisabledWhenParentNotEditable() {
        ThermostatTextArea textField = new ThermostatTextArea();
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
        textField.setEditable(false);
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
        textField.setEditable(true);
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
    }

    @Test
    public void testEnableDoesNotOverrideEditable() {
        ThermostatTextArea textField = new ThermostatTextArea();
        textField.setEnabled(false);
        textField.setEditable(false);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
        textField.setEnabled(true);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
    }

    @Test
    public void testEditableDoesNotOverrideEnabled() {
        ThermostatTextArea textField = new ThermostatTextArea();
        textField.setEnabled(false);
        textField.setEditable(false);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
        textField.setEditable(true);
        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
    }
    
}
