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

package com.redhat.thermostat.backend.system.internal;

import com.redhat.thermostat.common.portability.PortableHost;
import com.redhat.thermostat.backend.system.internal.models.HostInfoBuilder;
import com.redhat.thermostat.shared.config.OS;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.model.HostInfo;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HostInfoBuilderTest {

    private WriterID writerId;
    private PortableHost helper;

    @Before
    public void setup() {
        writerId = mock(WriterID.class);
        helper = mock(PortableHost.class);
        when(helper.getHostName()).thenReturn("testhost");
        when(helper.getOSName()).thenReturn("testos");
        when(helper.getOSVersion()).thenReturn("testversion");
        when(helper.getCPUModel()).thenReturn("testcpu");
        when(helper.getCPUCount()).thenReturn(4567);
        when(helper.getTotalMemory()).thenReturn(9876L);
    }

    // TODO - This test currently fails on Windows because the helper DLL isn't on the execution path
    @Test
    @Ignore
    public void testSimpleBuild() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        HostInfo info = new HostInfoBuilderImpl(writerId).build();
        assertNotNull(info);
    }

    @Test
    public void testGetInfo() {
        final HostInfoBuilder ib = new HostInfoBuilderImpl(writerId, helper);
        final HostInfo hi = ib.build();
        assertEquals("testhost",hi.getHostname());
        assertEquals("testos", hi.getOsName());
        assertEquals("testcpu", hi.getCpuModel());
        assertEquals("testversion", hi.getOsKernel());
        assertEquals(4567, hi.getCpuCount());
        assertEquals(9876L, hi.getTotalMemory());
    }
}

