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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;

public class ProfileVmRequestReceiverTest {

    private static final String AGENT_ID = "agent-id";

    private static final String VM_ID = "foo";
    private static final int VM_PID = 1;

    private ProfileVmRequestReceiver requestReceiver;

    private VmProfiler profiler;
    private ProfileDAO dao;
    private ProfileUploader uploader;

    @Before
    public void setUp() {
        profiler = mock(VmProfiler.class);
        dao = mock(ProfileDAO.class);

        requestReceiver = new ProfileVmRequestReceiver(AGENT_ID, profiler, dao);
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
    }

    @Test
    public void startAndStopProfiling() throws ProfilerException {
        Request request;
        request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);

        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        Response result = requestReceiver.receive(request);

        assertEquals(ResponseType.OK, result.getType());
        verify(profiler).startProfiling(VM_PID);

        request = ProfileRequest.create(null, VM_ID, ProfileRequest.STOP_PROFILING);
        result = requestReceiver.receive(request);

        assertEquals(ResponseType.OK, result.getType());
        verify(profiler).stopProfiling(eq(VM_PID), isA(ProfileUploader.class));
    }

    @Test
    public void doesNotProfileDeadVms() {
        requestReceiver.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        requestReceiver.vmStatusChanged(Status.VM_STOPPED, VM_ID, VM_PID);

        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);
        assertEquals(ResponseType.NOK, result.getType());
    }
}
