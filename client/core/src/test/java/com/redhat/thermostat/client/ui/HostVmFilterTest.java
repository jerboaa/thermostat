/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.ui.HostVmFilter;
import com.redhat.thermostat.common.dao.VmRef;

public class HostVmFilterTest {

    private HostVmFilter filter;
    
    @Before
    public void setUp() throws Exception {
        filter = new HostVmFilter();
    }

    @After
    public void tearDown() throws Exception {
        filter = null;
    }

    @Test
    public void vmStringIDfilterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getStringID()).thenReturn("operation1");
        when(ref.getName()).thenReturn("noMatch");
        filter.setFilter("op");
        assertTrue(filter.matches(ref));
    }
    
    @Test
    public void vmNamefilterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("operation1");
        when(ref.getStringID()).thenReturn("noMatch");
        filter.setFilter("op");
        assertTrue(filter.matches(ref));
    }
    
    @Test
    public void filterDoesntMatch() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("test1");
        when(ref.getStringID()).thenReturn("test3");
        filter.setFilter("op");
        assertFalse(filter.matches(ref));
    }
    
    @Test
    public void filterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("test1");
        when(ref.getStringID()).thenReturn("test1");
        filter.setFilter("test1");
        assertTrue(filter.matches(ref));
    }

}
