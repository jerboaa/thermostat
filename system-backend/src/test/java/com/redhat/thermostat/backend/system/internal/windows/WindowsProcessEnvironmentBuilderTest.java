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

package com.redhat.thermostat.backend.system.internal.windows;

import com.redhat.thermostat.agent.utils.windows.WindowsHelperImpl;
import com.redhat.thermostat.backend.system.internal.models.ProcessEnvironmentBuilder;
import com.redhat.thermostat.shared.config.OS;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test windows process env builder
 */
public class WindowsProcessEnvironmentBuilderTest {

    private WindowsHelperImpl whelp;

    private static final int FAKE_PID = 4567;
    private static final String PATH_KEY = "PATH";
    private static final String FAKE_PATH = "testpath";

    private static final Map<String,String> goodMap = new HashMap<>();

    @Before
    public void setup() {
        whelp = mock(WindowsHelperImpl.class);
        goodMap.put(PATH_KEY, FAKE_PATH);
        when(whelp.getEnvironment(anyInt())).thenReturn(null);
        when(whelp.getEnvironment(eq(FAKE_PID))).thenReturn(goodMap);
    }

    // TODO - This test currently fails on Windows because the helper DLL isn't on the execution path
    @Test
    @Ignore
    public void testSimpleBuild() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        final Map<String,String> info = new WindowsProcessEnvironmentBuilderImpl().build(FAKE_PID);
        assertNotNull(info);
    }

    @Test
    public void testGetInfoFromGoodPid() {
        final ProcessEnvironmentBuilder ib = new WindowsProcessEnvironmentBuilderImpl(whelp);
        final Map<String,String> info  = ib.build(FAKE_PID);
        assertFalse(info.isEmpty());
        assertTrue(info.containsKey(PATH_KEY));
        assertEquals(FAKE_PATH, info.get(PATH_KEY));
    }

    @Test
    public void testGetInfoFromBadPid() {
        final ProcessEnvironmentBuilder ib = new WindowsProcessEnvironmentBuilderImpl(whelp);
        final Map<String,String> info  = ib.build(FAKE_PID+1);
        assertTrue(info == null || info.isEmpty());
    }
}
