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

package com.redhat.thermostat.client.filter.host.swing;

import static org.junit.Assert.assertEquals;
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

    private static final String THERMOSTAT_MAIN_CLASS = "com.redhat.thermostat.main.Thermostat";
    private static final String THERMOSTAT_COMMAND_CHANNEL_CLASS = "com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain";

    private ThermostatVmMainLabelDecorator decorator;
    private VmInfoDAO dao;
    private VmRef vmRef;
    private VmInfo info;

    @Before
    public void setUp() {
        dao = mock(VmInfoDAO.class);
        vmRef = mock(VmRef.class);        
        info = mock(VmInfo.class);

        when(dao.getVmInfo(vmRef)).thenReturn(info);
    }
    
    /**
     * Testing using a Thermostat vm. The getLabel method must return
     * the shorter version of the vm's command.
     */
    @Test
    public void getLabelTest1() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat service");
        
        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat service", result);
    }

    @Test
    public void verifyLabelForIncompleteCommand() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat", result);
    }


    @Test
    public void verifyLabelForCommandChannel() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_COMMAND_CHANNEL_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_COMMAND_CHANNEL_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain 127.0.0.1 1200");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat (command channel)", result);
    }

    @Test
    public void verifyLabelForCliCommand() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat kill-vm --vmId foo");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat kill-vm", result);
    }

    @Test
    public void verifyLabelForCliCommand2() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat agent -d http://127.0.0.1:8999/thermostat/storage");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat agent", result);
    }

    @Test
    public void verifyLabelWhenGlobalOptionIsUsed() {
        when(vmRef.getName()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getMainClass()).thenReturn(THERMOSTAT_MAIN_CLASS);
        when(info.getJavaCommandLine()).thenReturn("com.redhat.thermostat.main.Thermostat --print-osgi-info kill-vm");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("Thermostat kill-vm", result);
    }

    /**
     * Testing using a non Thermostat vm. The getLabel method must return
     * the vm's main class.
     */
    @Test
    public void getLabelTest2() {
        String JAR = "/opt/eclipse//plugin/org.eclipse.equinox.laucher.jar";
        when(vmRef.getName()).thenReturn(JAR);
        when(info.getMainClass()).thenReturn(JAR);
        when(info.getJavaCommandLine()).thenReturn("-os linux");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("originalLabel", result);
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
        String result = decorator.getLabel("localhost.localdomain", hostRef);
        assertEquals("localhost.localdomain", result);
    }

    @Test
    public void verifySomeDirectoryIsNotDetectedAsThermostat() {
        // Just because "thermostat" appears in the path doesn't make it thermostat
        String JAR = "/home/thermostat/ant/ant.jar";
        when(vmRef.getName()).thenReturn(JAR);
        when(info.getMainClass()).thenReturn(JAR);
        when(info.getJavaCommandLine()).thenReturn("-f bulid.xml");

        decorator = new ThermostatVmMainLabelDecorator(dao);
        String result = decorator.getLabel("originalLabel", vmRef);
        assertEquals("originalLabel", result);
    }
}
