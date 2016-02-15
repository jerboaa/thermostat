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

package com.redhat.thermostat.vm.profiler.agent.jvm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.profiler.agent.jvm.InstrumentationControl.ResultsFile;
import com.redhat.thermostat.vm.profiler.agent.jvm.InstrumentationControl.ResultsFileCreator;

public class InstrumentationControlTest {

    private Instrumentation instrumentation;
    private ProfilerInstrumentor instrumentor;
    private ProfileRecorder recorder;
    private ResultsFileCreator resultsFileCreator;
    private ResultsFile resultsFile;

    private InstrumentationControl control;
    private StringWriter dataWriter;

    @Before
    public void setUp() throws Exception {
        instrumentation = mock(Instrumentation.class);
        when(instrumentation.isModifiableClass(Object.class)).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[] { Object.class });

        instrumentor = mock(ProfilerInstrumentor.class);
        when(instrumentor.shouldInstrument(Object.class)).thenReturn(true);

        recorder = mock(ProfileRecorder.class);

        dataWriter = new StringWriter();
        resultsFile = mock(ResultsFile.class);
        when(resultsFile.getWriter()).thenReturn(new BufferedWriter(dataWriter));

        resultsFileCreator = mock(ResultsFileCreator.class);
        when(resultsFileCreator.get()).thenReturn(resultsFile);

        control = new InstrumentationControl(instrumentation, instrumentor, recorder, resultsFileCreator);
    }

    @Test
    public void isProfilingRepresentsCurrentProfilingState() throws Exception {
        control.startProfiling();

        assertTrue(control.isProfiling());

        control.stopProfiling();

        assertFalse(control.isProfiling());
    }

    @Test (expected=IllegalStateException.class)
    public void startingProfilingTwiceThrowsException() throws Exception {
        control.startProfiling();
        control.startProfiling();
    }

    @Test (expected=IllegalStateException.class)
    public void stoppingProfilingWhenNotRunningThrowException() throws Exception {
        control.stopProfiling();
    }

    @Test
    public void startProfilingInstrumentAllCode() throws Exception {
        control.startProfiling();

        verify(instrumentation).addTransformer(instrumentor, true);
        verify(instrumentation).retransformClasses(new Class[] { Object.class });
    }

    @Test
    public void stopProfilingInstrumentsAllCode() throws Exception {
        control.startProfiling();

        control.stopProfiling();

        verify(instrumentation).removeTransformer(instrumentor);
        verify(instrumentation, times(2)).retransformClasses(new Class[] { Object.class });
    }

    @Test
    public void stopProfilingSavesProfilingResultsToDisk() throws Exception {
        final String DATA_LOCATION = "foobar";

        Map<String, AtomicLong> profileData = new HashMap<String, AtomicLong>();
        profileData.put("foo", new AtomicLong(1));
        when(recorder.getData()).thenReturn(profileData);

        when(resultsFile.getPath()).thenReturn(DATA_LOCATION);
        control.startProfiling();

        control.stopProfiling();

        verify(resultsFile).getWriter();
        assertEquals("1\tfoo\n", dataWriter.toString());
        assertEquals(DATA_LOCATION, control.getProfilingDataFile());
    }

    @Test
    public void stopProfilingClearsProfilingData() throws Exception {
        control.startProfiling();
        control.stopProfiling();

        verify(recorder).clearData();
    }

    @Test
    public void vmShutdownSaveDataToDisk() throws Exception {
        final String DATA_LOCATION = "foobar";

        Map<String, AtomicLong> profileData = new HashMap<String, AtomicLong>();
        profileData.put("foo", new AtomicLong(1));
        when(recorder.getData()).thenReturn(profileData);

        when(resultsFile.getPath()).thenReturn(DATA_LOCATION);

        control.startProfiling();

        // simulate vm shutdown:
        control.onVmShutdown();

        verify(resultsFile).getWriter();
        assertEquals("1\tfoo\n", dataWriter.toString());
        assertEquals(DATA_LOCATION, control.getProfilingDataFile());
    }
}
