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

package com.redhat.thermostat.client.filter.host.swing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.PlatformIcon;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;

public class DeadHostIconDecoratorTest {

    private DeadHostIconDecorator decorator;

    @Before
    public void setUp() {
        final int SOME_SIZE = 10;

        UIDefaults uiDefaults = mock(UIDefaults.class);
        when(uiDefaults.getIconDecorationSize()).thenReturn(SOME_SIZE);
        when(uiDefaults.getReferenceFieldDefaultIconSize()).thenReturn(SOME_SIZE);

        HostInfoDAO dao = mock(HostInfoDAO.class);

        HostIconDecorator parent = HostIconDecorator.createInstance(uiDefaults);

        decorator = DeadHostIconDecorator.createInstance(dao, uiDefaults, parent);
    }

    @Test
    public void verifyGetIconWithNullProducesValidIcon() {
        PlatformIcon icon = decorator.getIcon(null, mock(HostRef.class));
        assertNotNull(icon);
    }

    @Test
    public void verifyGetIconWithNotAHostRefProducesOriginalIcon() {
        PlatformIcon icon = decorator.getIcon(null, mock(VmRef.class));
        assertNull(icon);
    }

    @Test
    public void verifyGetSelectedIconWithNullProducesValidIcon() {
        PlatformIcon icon = decorator.getSelectedIcon(null, mock(HostRef.class));
        assertNotNull(icon);
    }

    @Test
    public void verifyGetSelectedIconWithNotAHostRefProducesOriginalIcon() {
        PlatformIcon icon = decorator.getSelectedIcon(null, mock(VmRef.class));
        assertNull(icon);
    }

}
