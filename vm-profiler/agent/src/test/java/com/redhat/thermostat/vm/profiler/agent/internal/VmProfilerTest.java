/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.vm.profiler.agent.internal.VmProfiler.ProfileUploaderCreator;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class VmProfilerTest {

    private static final String AGENT_ID = "some-agent";
    private static final String VM_ID = "some-vm";
    private static final int PID = 0;

    private static final String AGENT_JAR = "foo";
    private static final String ASM_JAR = "bar";
    private static final String AGENT_OPTIONS = AGENT_JAR;
    private static final long TIMESTAMP = 1_000_000_000;

    private VmProfiler profiler;

    private RemoteProfilerCommunicator remote;
    private Clock clock;
    private ProfileDAO dao;

    private ProfileUploader uploader;
    private ProfileUploaderCreator profileUploaderCreator;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("AGENT_JAR", AGENT_JAR);
        props.setProperty("ASM_JAR", ASM_JAR);

        dao = mock(ProfileDAO.class);

        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(TIMESTAMP);

        uploader = mock(ProfileUploader.class);
        profileUploaderCreator = mock(ProfileUploaderCreator.class);
        when(profileUploaderCreator.create(dao, AGENT_ID, VM_ID, PID)).thenReturn(uploader);

        remote = mock(RemoteProfilerCommunicator.class);

        profiler = new VmProfiler(AGENT_ID, props, dao, clock, profileUploaderCreator, remote);
    }

    @Test (expected=ProfilerException.class)
    public void doesNotProfileNotStartedVms() throws Exception {
        profiler.startProfiling(VM_ID);
    }

    @Test (expected=ProfilerException.class)
    public void doesNotProfileDeadVms() throws Exception {
        profiler.vmStarted(VM_ID, PID);
        profiler.vmStopped(VM_ID, PID);
        profiler.startProfiling(VM_ID);
    }

    @Test (expected=ProfilerException.class)
    public void doesNotStartingProfilingTwice() throws Exception {
        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.startProfiling(VM_ID);
    }

    @Test
    public void startingProfilingLoadsJvmAgentAndMakesAnRmiCall() throws Exception {
        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);

        verify(remote).loadAgentIntoPid(PID, AGENT_JAR, AGENT_OPTIONS);
        verify(remote).startProfiling(PID);
        verify(dao).addStatus(new ProfileStatusChange(AGENT_ID, VM_ID, TIMESTAMP, true));
        verifyNoMoreInteractions(remote);
    }

    @Test
    public void onlyLoadsAgentOnceForRepeatedProfiling() throws Exception {
        final String FILE = "foobar";
        when(remote.getProfilingDataFile(PID)).thenReturn(FILE);

        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.stopProfiling(VM_ID);
        profiler.startProfiling(VM_ID);

        verify(remote, times(1)).loadAgentIntoPid(PID, AGENT_JAR, AGENT_OPTIONS);
        verify(remote, times(2)).startProfiling(PID);
    }

    @Test
    public void loadsAgentMultipleTimesForNewProcesses() throws Exception {
        final String FILE = "foobar";
        when(remote.getProfilingDataFile(PID)).thenReturn(FILE);

        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.stopProfiling(VM_ID);
        profiler.vmStopped(VM_ID, PID);
        // it's not likely that the vmId and the pid will be reused, but just to be sure
        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);

        verify(remote, times(2)).loadAgentIntoPid(PID, AGENT_JAR, AGENT_OPTIONS);
        verify(remote, times(2)).startProfiling(PID);
    }

    @Test (expected=ProfilerException.class)
    public void doesNotStopProfilingAnUnknownVm() throws Exception {
        profiler.stopProfiling(VM_ID);
    }

    @Test (expected=ProfilerException.class)
    public void doesNotStopProfilingNonProfiledVm() throws Exception {
        profiler.vmStarted(VM_ID, PID);
        profiler.stopProfiling(VM_ID);
    }

    @Test (expected=ProfilerException.class)
    public void errorOnStoppingTwice() throws Exception {
        final String FILE = "foobar";
        when(remote.getProfilingDataFile(PID)).thenReturn(FILE);

        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.stopProfiling(VM_ID);
        profiler.stopProfiling(VM_ID);
    }

    @Test
    public void stoppingProfilingMakesInvokesStopUsingRmiAndUploadsData() throws Exception {
        final String FILE = "foobar";
        when(remote.getProfilingDataFile(PID)).thenReturn(FILE);

        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.stopProfiling(VM_ID);

        verify(dao).addStatus(new ProfileStatusChange(AGENT_ID, VM_ID, TIMESTAMP, false));

        verify(remote).stopProfiling(PID);
        verify(uploader).upload(eq(TIMESTAMP), eq(TIMESTAMP), eq(new File(FILE)), isA(Runnable.class));
        verifyNoMoreInteractions(uploader);
    }

    @Test
    public void gathersAndUploadsProfileDataOnVmExit() throws Exception {
        // this is written on the target vm exit
        File profilingResults = new File(System.getProperty("java.io.tmpdir"), "thermostat-" + PID + "-foobar.perfdata");
        try (BufferedWriter writer = Files.newBufferedWriter(profilingResults.toPath(), StandardCharsets.UTF_8)) {
            writer.append("test file, please ignore");
        }

        profiler.vmStarted(VM_ID, PID);
        profiler.startProfiling(VM_ID);
        profiler.vmStopped(VM_ID, PID);

        verify(remote, never()).stopProfiling(PID);
        ArgumentCaptor<Runnable> cleanupCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(uploader).upload(eq(TIMESTAMP), eq(TIMESTAMP), eq(profilingResults), cleanupCaptor.capture());
        verify(dao).addStatus(new ProfileStatusChange(AGENT_ID, VM_ID, TIMESTAMP, false));

        cleanupCaptor.getValue().run();
        assertFalse(profilingResults.exists());
    }
}
