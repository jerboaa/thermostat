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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class ThermostatVmMainLabelDecoratorTest {

    private ThermostatVmMainLabelDecorator decorator;
    private VmInfoDAO dao;
    private VmRef vmRef;
    private VmInfo info;


    @Before
    public void setUp() {
        dao = mock(VmInfoDAO.class);
        vmRef = mock(VmRef.class);        
        info = mock(VmInfo.class);
    }
    
    /**
     * Testing using a Thermostat vm. The getLabel method must return
     * the shorter version of the vm's command.
     */
    @Test
    public void getLabelTest1() {
        
        when(dao.getVmInfo(vmRef)).thenReturn(info);
        when(info.getMainClass()).thenReturn("com.redhat.thermostat.main.Thermostat");
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat service");
        
        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertTrue(result.equals("Thermostat service"));
    }
    
    /**
     * Testing using a non Thermostat vm. The getLabel method must return
     * the vm's main class.
     */
    @Test
    public void getLabelTest2() {
        
        when(dao.getVmInfo(vmRef)).thenReturn(info);
        when(info.getMainClass()).thenReturn("/opt/eclipse//plugin/org.eclipse.equinox.laucher.jar");
        when(info.getJavaCommandLine()).thenReturn("-os linux");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertTrue(result.equals("/opt/eclipse//plugin/org.eclipse.equinox.laucher.jar"));
    }
    
    
    /**
     * Testing using a non VmRef object. The getLabel method must return
     * the host's reference name.
     */
    @Test
    public void getLabelTest3() {
        
        Ref hostRef = mock(HostRef.class);
        when(hostRef.getName()).thenReturn("localhost.localdomain");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", hostRef);
        assertTrue(result.equals("localhost.localdomain"));
    }

}
