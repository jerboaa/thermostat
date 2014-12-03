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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;

public class ProfilerRequestReceiverTest {

    private static final String VM_ID = "foo";

    private ProfilerRequestReceiver requestReceiver;

    private VmProfiler profiler;

    @Before
    public void setUp() {
        profiler = mock(VmProfiler.class);

        requestReceiver = new ProfilerRequestReceiver(profiler);
    }

    @Test
    public void forwardsStartRequestToProfiler() throws Exception {
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);

        verify(profiler).startProfiling(VM_ID);

        assertEquals(ResponseType.OK, result.getType());
    }

    @Test
    public void exceptionThrownFromProfilerResultsInStarError() throws Exception {
        doThrow(ProfilerException.class).when(profiler).startProfiling(VM_ID);
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        Response result = requestReceiver.receive(request);

        assertEquals(ResponseType.NOK, result.getType());
    }

    @Test
    public void forwardsStopRequestToProfiler() throws Exception {
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.STOP_PROFILING);
        Response result = requestReceiver.receive(request);

        verify(profiler).stopProfiling(VM_ID);

        assertEquals(ResponseType.OK, result.getType());
    }

    @Test
    public void exceptionThrownFromProfilerResultsInStopError() throws Exception {
        doThrow(ProfilerException.class).when(profiler).stopProfiling(VM_ID);
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.STOP_PROFILING);
        Response result = requestReceiver.receive(request);

        assertEquals(ResponseType.NOK, result.getType());
    }

    @Test
    public void unknownRequestCausesError() {
        Request request = ProfileRequest.create(null, VM_ID, "bad-request");
        Response result = requestReceiver.receive(request);

        verifyNoMoreInteractions(profiler);

        assertEquals(ResponseType.NOK, result.getType());
    }

    @Test
    public void receiverIsValidTargetForRequest() throws Exception {
        Request request = ProfileRequest.create(null, VM_ID, ProfileRequest.START_PROFILING);
        assertEquals(ProfilerRequestReceiver.class.getName(), request.getReceiver());
    }
}
