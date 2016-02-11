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

package com.redhat.thermostat.vm.profiler.common.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.SaveFileListener.EventType;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileDAOImplTest {

    private static final String AGENT_ID = "some-agent";
    private static final String VM_ID = "some-vm";
    private static final long TIMESTAMP = 123;
    private static final String PROFILE_ID = "some-profile";
    private static final boolean STARTED = false;

    private Storage storage;
    private PreparedStatement statement;
    private ProfileInfo profileInfo;
    private ProfileStatusChange profileStatusChange;
    private ProfileDAOImpl profileDAO;
    private AgentId agentId;
    private VmId vmId;

    @Before
    public void setUp() throws DescriptorParsingException {
        agentId = new AgentId(AGENT_ID);
        vmId = new VmId(VM_ID);
        statement = mock(PreparedStatement.class);

        storage = mock(Storage.class);
        profileDAO = new ProfileDAOImpl(storage);
        when(storage.prepareStatement(isA(StatementDescriptor.class))).thenReturn(statement);

        profileInfo = new ProfileInfo(AGENT_ID, VM_ID, TIMESTAMP, TIMESTAMP, PROFILE_ID);
        profileStatusChange = new ProfileStatusChange(AGENT_ID, VM_ID, TIMESTAMP, STARTED);
    }

    @Test
    public void registersCategories() throws Exception {
        verify(storage).registerCategory(ProfileDAOImpl.PROFILE_INFO_CATEGORY);
        verify(storage).registerCategory(ProfileDAOImpl.PROFILE_STATUS_CATEGORY);
    }

    @Test
    public void testRunnableIsInvokedOnSaveFile() {
        InputStream data = new ByteArrayInputStream(new byte[0]);
        Runnable cleanup = mock(Runnable.class);
        ArgumentCaptor<SaveFileListener> listenerCaptor = ArgumentCaptor.forClass(SaveFileListener.class);

        profileDAO.saveProfileData(profileInfo, data, cleanup);

        verify(storage).saveFile(eq(profileInfo.getProfileId()), same(data), listenerCaptor.capture());

        SaveFileListener listener = listenerCaptor.getValue();
        listener.notify(EventType.SAVE_COMPLETE, null);

        verify(cleanup).run();
    }

    @Test
    public void testLoadLatestProfileData() throws StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<ProfileInfo> cursor = new Cursor<ProfileInfo>() {
            int number = 0;

            @Override
            public void setBatchSize(final int n) throws IllegalArgumentException {

            }

            @Override
            public int getBatchSize() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return (number == 0);
            }

            @Override
            public ProfileInfo next() {
                if (number == 0) {
                    number++;
                    return profileInfo;
                } else {
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
        when(statement.executeQuery()).thenReturn(cursor);

        InputStream expected = mock(InputStream.class);
        when(storage.loadFile(PROFILE_ID)).thenReturn(expected);

        InputStream result = profileDAO.loadLatestProfileData(agentId, vmId);

        assertEquals(expected, result);
    }

    @Test
    public void testGetLatestStatus() throws StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<ProfileStatusChange> cursor = new Cursor<ProfileStatusChange>() {
            int number = 0;

            @Override
            public void setBatchSize(final int n) throws IllegalArgumentException {

            }

            @Override
            public int getBatchSize() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return (number == 0);
            }

            @Override
            public ProfileStatusChange next() {
                if (number == 0) {
                    number++;
                    return profileStatusChange;
                } else {
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
        when(statement.executeQuery()).thenReturn(cursor);

        ProfileStatusChange result = profileDAO.getLatestStatus(agentId, vmId);
        assertEquals(agentId.get(), result.getAgentId());
        assertEquals(vmId.get(), result.getVmId());
    }

}
