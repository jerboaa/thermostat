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
import com.redhat.thermostat.backend.system.internal.models.ProcessUserInfo;
import com.redhat.thermostat.backend.system.internal.models.ProcessUserInfoBuilder;
import com.redhat.thermostat.shared.config.OS;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test windows user info builder
 */
public class WindowsProcessUserInfoBuilderTest {

    private WindowsHelperImpl whelp;

    private static final String FAKE_USERNAME = "testuser";
    private static final int FAKE_PID = 4567;
    private static final int FAKE_UID = 7652;

    @Before
    public void setup() {
        whelp = mock(WindowsHelperImpl.class);
        when(whelp.getUserName(anyInt())).thenReturn("badname");
        when(whelp.getUserName(eq(FAKE_PID))).thenReturn(FAKE_USERNAME);
        when(whelp.getUid(anyInt())).thenReturn(11);
        when(whelp.getUid(eq(FAKE_PID))).thenReturn(FAKE_UID);
    }

    // TODO - This test currently fails on Windows because the helper DLL isn't on the execution path
    @Test
    @Ignore
    public void testSimpleBuild() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        ProcessUserInfo info = new WindowsUserInfoBuilderImpl().build(FAKE_PID);
        assertNotNull(info);
    }

    @Test
    public void testGetInfoFromGoodPid() {
        final ProcessUserInfoBuilder ib = new WindowsUserInfoBuilderImpl(whelp);
        final ProcessUserInfo hi = ib.build(FAKE_PID);
        assertEquals(FAKE_USERNAME, hi.getUsername());
        assertEquals(FAKE_UID, hi.getUid());
    }

    @Test
    public void testGetInfoFromBadPid() {
        final ProcessUserInfoBuilder ib = new WindowsUserInfoBuilderImpl(whelp);
        final ProcessUserInfo hi = ib.build(FAKE_PID+1);
        assertNotSame(FAKE_USERNAME,hi.getUsername());
        assertNotSame(FAKE_UID, hi.getUid());
    }
}
