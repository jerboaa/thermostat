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

package com.redhat.thermostat.vm.io.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoStatPrintDelegateTest {

    private static Locale defaultLocale;

    private VmIoStatDAO vmIoStatDAO;
    private VmIoStatPrintDelegate delegate;
    private VmRef vm;
    private List<VmIoStat> cpuStats;

    @BeforeClass
    public static void setUpBeforeClass() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Locale.setDefault(defaultLocale);
    }

    @Before
    public void setUp() {
        setupDAOs();
        delegate = new VmIoStatPrintDelegate(vmIoStatDAO);
    }

    @After
    public void tearDown() {
        vmIoStatDAO = null;
    }

    private void setupDAOs() {
        vmIoStatDAO = mock(VmIoStatDAO.class);
        String vmId = "vmId";
        HostRef host = new HostRef("123", "dummy");
        vm = new VmRef(host, vmId, 234, "dummy");
        VmIoStat ioStat1 = new VmIoStat("foo-agent", vmId, 2, 65, 123, 123, 123);
        VmIoStat ioStat2 = new VmIoStat("foo-agent", vmId, 3, 70, 78, 89, 90);
        cpuStats = Arrays.asList(ioStat1, ioStat2);
        when(vmIoStatDAO.getLatestVmIoStats(vm, Long.MIN_VALUE)).thenReturn(cpuStats);
    }

    @Test
    public void testGetLatestStats() {
        List<? extends TimeStampedPojo> stats = delegate.getLatestStats(vm, Long.MIN_VALUE);
        assertEquals(cpuStats, stats);
    }

    @Test
    public void testGetHeaders() {
        List<String> headers = delegate.getHeaders(cpuStats.get(0));
        assertEquals(Arrays.asList("characters read", "characters written"), headers);
    }

    @Test
    public void testGetStatRow() throws CommandException {
        final List<String> row1 = Arrays.asList("65", "123");
        final List<String> row2 = Arrays.asList("70", "78");
        assertEquals(row1, delegate.getStatRow(cpuStats.get(0)));
        assertEquals(row2, delegate.getStatRow(cpuStats.get(1)));
    }

}

