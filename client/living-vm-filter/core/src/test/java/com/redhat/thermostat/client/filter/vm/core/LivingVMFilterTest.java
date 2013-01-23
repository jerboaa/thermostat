/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.filter.vm.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.filter.vm.core.LivingVMFilter;
import com.redhat.thermostat.client.filter.vm.core.LivingVMFilterMenuAction;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.storage.model.VmInfo;

public class LivingVMFilterTest {

    private VmInfoDAO dao;
    private VmRef vmRef1;
    private VmRef vmRef2;
    
    private VmInfo vmInfo1;
    private VmInfo vmInfo2;
    
    @Before
    public void setUp() {
        dao = mock(VmInfoDAO.class);
        
        vmRef1 = mock(VmRef.class);
        vmRef2 = mock(VmRef.class);
        
        vmInfo1 = mock(VmInfo.class);
        vmInfo2 = mock(VmInfo.class);
        
        when(dao.getVmInfo(vmRef1)).thenReturn(vmInfo1);
        when(dao.getVmInfo(vmRef2)).thenReturn(vmInfo2);
        
        when(vmInfo1.isAlive()).thenReturn(true);
        when(vmInfo2.isAlive()).thenReturn(false);
    }
    
    @Test
    public void testFilter() {
        LivingVMFilter filter = new LivingVMFilter(dao);
        LivingVMFilterMenuAction action = new LivingVMFilterMenuAction(filter);
        
        assertTrue(filter.matches(vmRef1));
        assertFalse(filter.matches(vmRef2));
        
        action.execute();
        
        assertTrue(filter.matches(vmRef1));
        assertTrue(filter.matches(vmRef2));
    }

}

