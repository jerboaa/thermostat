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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.testutils.StubBundleContext;

public class HeapDumperTest {
    private static final String TEST_HOST_ID = "1111111";
    private static final String TEST_VM_ID = "vmId";
    private static final Integer TEST_VM_PID = 2222222;

    private HeapDumper dumper;

    private StubBundleContext bundleContext;
    private Launcher launcher;
    
    @Before
    public void setUp() throws Exception {
        bundleContext = new StubBundleContext();
        
        launcher = mock(Launcher.class);
        bundleContext.registerService(Launcher.class, launcher, null);
        
        HostRef hostRef = new HostRef(TEST_HOST_ID, "myHost");
        VmRef ref = new VmRef(hostRef, TEST_VM_ID, TEST_VM_PID, "myVM");
        dumper = new HeapDumper(ref, bundleContext);
    }

    @Test
    public void testDump() throws CommandException {
        dumper.dump();

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(launcher).run(captor.capture(), eq(true));
        verifyNoMoreInteractions(launcher);

        String[] args = captor.getValue();
        assertArrayEquals(new String[] { "dump-heap", "--hostId", TEST_HOST_ID, "--vmId", String.valueOf(TEST_VM_ID) }, args);
    }

}

