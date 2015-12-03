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

package com.redhat.thermostat.client.swing;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MenuStatesTest {

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private MenuStates menuStates;

    @Before
    public void setup() {
        prefs = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editor);
        menuStates = new MenuStates(prefs);
    }

    @Test
    public void testSetMenuState() {
        menuStates.setMenuStates(Collections.singletonMap("someKey", true));
        verify(editor).set(contains("someKey"), eq(true));

        menuStates.setMenuStates(Collections.singletonMap("someKey", false));
        verify(editor).set(contains("someKey"), eq(false));
    }

    @Test
    public void testGetMenuState() {
        String falseKey = "falseKey";
        String trueKey = "trueKey";
        when(prefs.getBoolean(contains(falseKey), any(Boolean.TYPE))).thenReturn(false);
        when(prefs.getBoolean(contains(trueKey), any(Boolean.TYPE))).thenReturn(true);

        boolean resA = menuStates.getMenuState(falseKey);
        boolean resB = menuStates.getMenuState(trueKey);

        assertThat(resA, is(false));
        assertThat(resB, is(true));
    }

}
