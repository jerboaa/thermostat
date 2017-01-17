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

package com.redhat.thermostat.client.filter.internal.host.swing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.PlatformIcon;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

@Category(CacioTest.class)
public class HostIconDecoratorTest {

    private HostIconDecorator decorator;

    @Before
    public void setUp() {
        final int SOME_SIZE = 10;
        final Color SOME_COLOR = Color.BLACK;
        UIDefaults ui = mock(UIDefaults.class);
        when(ui.getReferenceFieldDefaultIconSize()).thenReturn(SOME_SIZE);
        when(ui.getReferenceFieldIconColor()).thenReturn(SOME_COLOR);

        decorator = HostIconDecorator.createInstance(ui);
    }

    @Test
    public void verifyGetIconProducesAnIconForHosts() {
        HostRef hostRef = mock(HostRef.class);
        PlatformIcon icon = decorator.getIcon(null, hostRef);
        assertNotNull(icon);
    }

    @Test
    public void testGetIconIgnoresVmRef() {
        VmRef vmRef = mock(VmRef.class);
        PlatformIcon icon = decorator.getIcon(null, vmRef);
        assertNull(icon);
    }

    @Test
    public void verifyGetSelectedIconProducesAnIconForHosts() {
        HostRef hostRef = mock(HostRef.class);
        PlatformIcon icon = decorator.getSelectedIcon(null, hostRef);
        assertNotNull(icon);
    }

    @Test
    public void testGetSelectedIconIgnoresVmRef() {
        VmRef vmRef = mock(VmRef.class);
        PlatformIcon icon = decorator.getSelectedIcon(null, vmRef);
        assertNull(icon);
    }

}
