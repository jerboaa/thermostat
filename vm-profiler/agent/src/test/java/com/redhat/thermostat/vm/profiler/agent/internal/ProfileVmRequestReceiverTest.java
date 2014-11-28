/*
 * Copyright 2012-2014 Red Hat, Inc.
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.vm.profiler.agent.internal.ProfileVmRequestReceiver.ProfileUploaderCreator;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileVmRequestReceiverTest {

    private static final String AGENT_ID = "agent-id";

    private static final String VM_ID = "foo";
    private static final int VM_PID = 1;
    private static final long TIMESTAMP = 99;

    private ProfileVmRequestReceiver requestReceiver;

    private Clock clock;
    private VmProfiler profiler;
    private ProfileDAO dao;
    private ProfileUploader uploader;

    @Before
    public void setUp() {
        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(TIMESTAMP);

        profiler = mock(VmProfiler.class);
        dao = mock(ProfileDAO.class);
        uploader = mock(ProfileUploader.class);
        ProfileUploaderCreator uploaderCreator = mock(ProfileUploaderCreator.class);
        when(uploaderCreator.create(dao, AGENT_ID, VM_ID, VM_PID)).thenReturn(uploader);

        requestReceiver = new ProfileVmRequestReceiver(AGENT_ID, clock, profiler, dao, uploaderCreator);
    }

    @Test
    public void doesNotProfileUnknownVm() {
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);
        assertEquals(ResponseType.NOK, result.getType());
    }

    @Test
    public void profilesKnownVms() throws ProfilerException {
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);

        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        Response result = requestReceiver.receive(request);

        assertEquals(ResponseType.OK, result.getType());
        verify(profiler).startProfiling(VM_PID);

        verify(dao).addStatus(new ProfileStatusChange(AGENT_ID, VM_ID, TIMESTAMP, true));
    }

    @Test
    public void startAndStopProfiling() throws ProfilerException {
        Request request;

        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);

        assertEquals(ResponseType.OK, result.getType());
        verify(profiler).startProfiling(VM_PID);

        request = ProfileRequest.create(null, VM_ID, ProfileRequest.STOP_PROFILING);
        result = requestReceiver.receive(request);

        assertEquals(ResponseType.OK, result.getType());
        verify(profiler).stopProfiling(eq(VM_PID), isA(ProfileUploader.class));
        verify(dao, times(2)).addStatus(isA(ProfileStatusChange.class));
    }

    @Test
    public void doesNotProfileDeadVms() {
        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        requestReceiver.vmStatusChanged(Status.VM_STOPPED, VM_ID, VM_PID);

        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);
        assertEquals(ResponseType.NOK, result.getType());
        verify(dao, never()).addStatus(isA(ProfileStatusChange.class));
    }

    @Test
    public void readsProfilingResultsOnVmExit() throws Exception {
        Request request;

        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        requestReceiver.receive(request);

        // simulate target vm exiting
        File profilingResults = new File(System.getProperty("java.io.tmpdir"), "thermostat-" + VM_PID + "-foobar.perfdata");
        try (BufferedWriter writer = Files.newBufferedWriter(profilingResults.toPath(), StandardCharsets.UTF_8)) {
            writer.append("test file, please ignore");
        }

        requestReceiver.vmStatusChanged(Status.VM_STOPPED, VM_ID, VM_PID);

        verify(profiler, never()).stopProfiling(anyInt(), isA(ProfileUploader.class));
        verify(uploader).upload(TIMESTAMP, profilingResults);
        verify(dao, times(2)).addStatus(isA(ProfileStatusChange.class));

        profilingResults.delete();
    }
}
