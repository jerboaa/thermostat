/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import com.redhat.thermostat.backend.system.ProcessUserInfoBuilder.ProcessUserInfo;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.utils.ProcDataSource;
import com.redhat.thermostat.utils.username.UserNameUtil;

public class ProcessUserInfoBuilderTest {
    
    @Test
    public void testBuild() throws IOException, ApplicationException {
        StringReader reader = new StringReader("Uid:   2000  2000  2000  2000");
        ProcDataSource source = mock(ProcDataSource.class);
        UserNameUtil util = mock(UserNameUtil.class);
        when(util.getUserName(2000)).thenReturn("myUser");
        when(source.getStatusReader(anyInt())).thenReturn(reader);
        ProcessUserInfoBuilder builder = new ProcessUserInfoBuilder(source, util);
        ProcessUserInfo info = builder.build(0);
        
        assertEquals(2000, info.getUid());
        assertEquals("myUser", info.getUsername());
    }
    
    @Test
    public void testBuildErrorUid() throws IOException, ApplicationException {
        StringReader reader = new StringReader("");
        ProcDataSource source = mock(ProcDataSource.class);
        UserNameUtil util = mock(UserNameUtil.class);
        when(source.getStatusReader(anyInt())).thenReturn(reader);
        ProcessUserInfoBuilder builder = new ProcessUserInfoBuilder(source, util);
        ProcessUserInfo info = builder.build(0);
        
        assertEquals(-1, info.getUid());
        assertEquals(null, info.getUsername());
    }
    
    @Test
    public void testBuildErrorUsername() throws IOException, ApplicationException {
        StringReader reader = new StringReader("Uid:   2000  2000  2000  2000");
        ProcDataSource source = mock(ProcDataSource.class);
        UserNameUtil util = mock(UserNameUtil.class);
        when(util.getUserName(2000)).thenReturn(null);
        when(source.getStatusReader(anyInt())).thenReturn(reader);
        ProcessUserInfoBuilder builder = new ProcessUserInfoBuilder(source, util);
        ProcessUserInfo info = builder.build(0);
        
        assertEquals(2000, info.getUid());
        assertEquals(null, info.getUsername());
    }

}
