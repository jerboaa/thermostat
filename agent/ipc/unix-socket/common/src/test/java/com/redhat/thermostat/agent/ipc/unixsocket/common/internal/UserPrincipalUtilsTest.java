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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UserPrincipalUtils.SystemHelper;

public class UserPrincipalUtilsTest {
    
    private static final String PROP_USER_NAME = "test";
    
    private SystemHelper helper;
    private UserPrincipal principal;
    private Path tempFilePath;

    private UserPrincipalUtils userUtils;
    
    @Before
    public void setup() throws Exception {
        helper = mock(SystemHelper.class);
        tempFilePath = mock(Path.class);
        principal = mock(UserPrincipal.class);
        
        when(helper.getSystemProperty("user.name")).thenReturn(PROP_USER_NAME);
        when(helper.createTempFile("thermostat_uds", PROP_USER_NAME)).thenReturn(tempFilePath);
        when(helper.getOwner(tempFilePath)).thenReturn(principal);
        userUtils = new UserPrincipalUtils(helper);
    }
    
    @Test
    public void testGetCurrentUser() throws Exception {
        UserPrincipal result = userUtils.getCurrentUser();
        
        assertEquals(principal, result);
        verify(helper).deleteFile(tempFilePath);
    }
    
    @Test
    public void testGetCurrentUserCreateFileException() throws Exception {
        when(helper.createTempFile(anyString(), anyString())).thenThrow(new IOException("TEST"));
        try {
            userUtils.getCurrentUser();
            fail("Expected IOException");
        } catch (IOException expected) {
            // No file to delete
            verify(helper, never()).deleteFile(any(Path.class));
        }
    }
    
    @Test
    public void testGetCurrentUserGetOwnerException() throws Exception {
        when(helper.getOwner(tempFilePath)).thenThrow(new IOException("TEST"));
        try {
            userUtils.getCurrentUser();
            fail("Expected IOException");
        } catch (IOException expected) {
            verify(helper).deleteFile(any(Path.class));
        }
    }
    
    @Test
    public void testGetCurrentUserGetOwnerNull() throws Exception {
        when(helper.getOwner(tempFilePath)).thenReturn(null);
        try {
            userUtils.getCurrentUser();
            fail("Expected IOException");
        } catch (IOException expected) {
            verify(helper).deleteFile(any(Path.class));
        }
    }

}
