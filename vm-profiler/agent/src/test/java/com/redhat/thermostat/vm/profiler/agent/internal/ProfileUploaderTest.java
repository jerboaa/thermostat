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

package com.redhat.thermostat.vm.profiler.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;

public class ProfileUploaderTest {

    private ProfileDAO dao;

    private final String AGENT_ID = "agent-id";
    private final String VM_ID = "vm-id";
    private final int PID = -1;
    private final long TIME = 1_000_000_000;

    private ProfileUploader uploader;

    @Before
    public void setUp() {
        dao = mock(ProfileDAO.class);

        uploader = new ProfileUploader(dao, AGENT_ID, VM_ID, PID);
    }

    @Test
    public void uploadFile() throws Exception {
        byte[] data = "Test Profile Data".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(data);

        ArgumentCaptor<ProfileInfo> profileInfoCaptor = ArgumentCaptor.forClass(ProfileInfo.class);

        uploader.upload(TIME, input);

        verify(dao).saveProfileData(profileInfoCaptor.capture(), eq(input));
        ProfileInfo profileInfo = profileInfoCaptor.getValue();
        assertEquals(AGENT_ID, profileInfo.getAgentId());
        assertEquals(VM_ID, profileInfo.getVmId());
        assertEquals(TIME, profileInfo.getTimeStamp());
        assertNotNull(profileInfo.getProfileId());
    }
}
