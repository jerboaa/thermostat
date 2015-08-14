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

package com.redhat.thermostat.vm.gc.command.internal;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.params.GcParam;
import com.redhat.thermostat.vm.gc.common.params.GcParamsMapper;
import com.redhat.thermostat.vm.gc.common.params.JavaVersionRange;

public class ShowGcNameCommandTest {
    
    private static final String VM_ID = "foo-id";
    private Arguments args, listTunablesArgs;
    
    @Before
    public void setup() {
        args = mock(Arguments.class);
        listTunablesArgs = mock(Arguments.class);
        when(listTunablesArgs.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn(VM_ID);
        when(listTunablesArgs.hasArgument("with-tunables")).thenReturn(true);
    }
    
    @Test
    public void commandFailsWithVmInfoDAOUnavailable() {
        ShowGcNameCommand command = new ShowGcNameCommand();
        try {
            command.run(new TestCommandContextFactory().createContext(args));
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("VmInfoDAO is unavailable"));
        }
    }
    
    @Test
    public void commandFailsWithVmGcStatDAOUnavailable() {
        ShowGcNameCommand command = new ShowGcNameCommand();
        command.setVmInfo(mock(VmInfoDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("VmGcStatDAO is unavailable"));
        }
    }
    
    @Test
    public void commandFailsWhenVmIdNotFound() {
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn(VM_ID);
        ShowGcNameCommand command = new ShowGcNameCommand();
        VmInfoDAO dao = mock(VmInfoDAO.class);
        when(dao.getVmInfo(new VmId(VM_ID))).thenReturn(null);
        command.setVmInfo(dao);
        command.setVmGcStat(mock(VmGcStatDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
        } catch (CommandException e) {
            assertTrue(e.getMessage().startsWith("VM with ID:"));
            assertTrue(e.getMessage().endsWith("not found"));
        }
    }

    @Test
    public void commandFailsWhenVmIdIsNull() {
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn(null);
        ShowGcNameCommand command = new ShowGcNameCommand();
        command.setVmInfo(mock(VmInfoDAO.class));
        command.setVmGcStat(mock(VmGcStatDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
        } catch (CommandException e) {
            assertTrue(e.getMessage().equals("A vmId is required"));
        }
    }
    
    @Test
    public void commandSuccessWithKnownMapping() throws CommandException {
        // This set should map to the parallel collector
        Set<String> collectorSet = Sets.newSet("PSParallelCompact", "PSScavenge");
        String expectedCollectorName = "Parallel Collector";
        doSuccessTest(collectorSet, expectedCollectorName);
    }

    @Test
    public void commandSuccessWithUnknownMapping() throws CommandException {
        // This set should *not* map to anything known.
        Set<String> collectorSet = Sets.newSet("Foo", "Bar");
        String expectedCollectorName = "Unknown Collector";
        doSuccessTest(collectorSet, expectedCollectorName);
    }

    @Test
    public void commandSuccessWithShowTunablesFlag() throws CommandException {
        Set<String> collectorSet = Sets.newSet("PSParallelCompact", "PSScavenge");
        String expectedCollectorName = "Parallel Collector";
        ShowGcNameCommand command = new ShowGcNameCommand();
        VmInfoDAO dao = mock(VmInfoDAO.class);
        VmInfo fooVmInfo = mock(VmInfo.class);
        when(fooVmInfo.getVmId()).thenReturn(VM_ID);
        String mainClass = "com.example.app.Main";
        when(fooVmInfo.getMainClass()).thenReturn(mainClass);
        String javaVersion = "1.8.0_45";
        when(fooVmInfo.getJavaVersion()).thenReturn(javaVersion);
        when(dao.getVmInfo(any(VmId.class))).thenReturn(fooVmInfo);
        command.setVmInfo(dao);
        VmGcStatDAO gcStat = mock(VmGcStatDAO.class);
        when(gcStat.getDistinctCollectorNames(any(VmId.class))).thenReturn(collectorSet);
        command.setVmGcStat(gcStat);
        TestCommandContextFactory testContextFactory = new TestCommandContextFactory();
        command.run(testContextFactory.createContext(listTunablesArgs));
        String output = testContextFactory.getOutput();
        assertThat(output, containsString(expectedCollectorName));
        assertThat(output, containsString(mainClass));
        assertThat(output, containsString(VM_ID));
        for (GcParam param : GcParamsMapper.getInstance().getParams(new GcCommonNameMapper().mapToCommonName(collectorSet), JavaVersionRange.fromString(javaVersion))) {
            assertThat(output, containsString(param.getFlag()));
        }
        assertThat(output, not(containsString("Unable to show GC tunables")));
    }
    
    private void doSuccessTest(Set<String> collectorMapping, String expectedCollectorName) throws CommandException {
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn(VM_ID);
        ShowGcNameCommand command = new ShowGcNameCommand();
        VmInfoDAO dao = mock(VmInfoDAO.class);
        VmInfo fooVmInfo = mock(VmInfo.class);
        when(fooVmInfo.getVmId()).thenReturn(VM_ID);
        String mainClass = "com.example.app.Main";
        when(fooVmInfo.getMainClass()).thenReturn(mainClass);
        when(dao.getVmInfo(new VmId(VM_ID))).thenReturn(fooVmInfo);
        command.setVmInfo(dao);
        VmGcStatDAO gcStat = mock(VmGcStatDAO.class);
        when(gcStat.getDistinctCollectorNames(new VmId(VM_ID))).thenReturn(collectorMapping);
        command.setVmGcStat(gcStat);
        TestCommandContextFactory testContextFactory = new TestCommandContextFactory();
        command.run(testContextFactory.createContext(args));
        String output = testContextFactory.getOutput();
        assertTrue(output, output.contains(expectedCollectorName));
        assertTrue(output, output.contains(mainClass));
        assertTrue(output, output.contains(VM_ID));
    }
}
