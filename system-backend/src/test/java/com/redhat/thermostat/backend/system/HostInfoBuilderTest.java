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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;

import org.junit.Test;

import com.redhat.thermostat.backend.system.HostInfoBuilder.HostCpuInfo;
import com.redhat.thermostat.backend.system.HostInfoBuilder.HostMemoryInfo;
import com.redhat.thermostat.backend.system.HostInfoBuilder.HostOsInfo;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.utils.ProcDataSource;

public class HostInfoBuilderTest {

    final int KILOBYTES_TO_BYTES = 1024;

    @Test
    public void testSimpleBuild() {
        HostInfo info = new HostInfoBuilder(new ProcDataSource()).build();
        assertNotNull(info);
    }

    @Test
    public void testCpuInfo() throws IOException {
        String cpuInfoString =
                "processor: 1\n" +
                "model name: Test Model\n" +
                "processor: 0\n" +
                "model name: Test Model\n";

        StringReader cpuInfoReader = new StringReader(cpuInfoString);

        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getCpuInfoReader()).thenReturn(cpuInfoReader);

        HostCpuInfo cpuInfo = new HostInfoBuilder(dataSource).getCpuInfo();
        assertEquals(2, cpuInfo.count);
        assertEquals("Test Model", cpuInfo.model);
        verify(dataSource).getCpuInfoReader();
    }

    @Test
    public void testMemoryInfo() throws IOException {
        String memoryInfoString =
                "MemTotal: 12345 kB";

        StringReader memoryInfoReader = new StringReader(memoryInfoString);
        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getMemInfoReader()).thenReturn(memoryInfoReader);

        HostMemoryInfo memoryInfo = new HostInfoBuilder(dataSource).getMemoryInfo();
        assertNotNull(memoryInfo);
        assertEquals(Size.bytes(12345 * KILOBYTES_TO_BYTES), memoryInfo.totalMemory);
        verify(dataSource).getMemInfoReader();

    }

    @Test
    public void testOsInfo() {
        DistributionInformation distroInfo = new DistributionInformation("distro-name", "distro-version");
        ProcDataSource dataSource = mock(ProcDataSource.class);
        HostOsInfo osInfo = new HostInfoBuilder(dataSource).getOsInfo(distroInfo);
        assertEquals("distro-name distro-version", osInfo.distribution);
        assertEquals(System.getProperty("os.name") + " " + System.getProperty("os.version"), osInfo.kernel);
    }

    @Test
    public void testHostname() {

        InetAddress address = mock(InetAddress.class);
        when(address.getCanonicalHostName()).thenReturn("test-hostname");

        ProcDataSource dataSource = mock(ProcDataSource.class);

        String name = new HostInfoBuilder(dataSource).getHostName(address);
        assertEquals("test-hostname", name);
    }

}
