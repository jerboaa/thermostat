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

package com.redhat.thermostat.vm.io.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.io.common.VmIoStat;

public class VmIoStatBuilderTest {

    private WriterID writerID;

    @Before
    public void setup() {
        writerID = mock(WriterID.class);
    }

    @Test
    public void testBuilderBuildsNullForUnknownPid() {
        Clock clock = mock(Clock.class);
        ProcIoDataReader procDataReader = mock(ProcIoDataReader.class);
        VmIoStatBuilder builder = new VmIoStatBuilder(clock, procDataReader, writerID);
        VmIoStat result = builder.build("vmId", 0);
        assertNull(result);
    }

    @Test
    public void testBuildWithSufficientInformation() {
        final int PID = 0;

        int rchar = 1;
        int wchar = 2;
        int syscr = 3;
        int syscw = 4;
        int read_bytes = 5;
        int write_bytes = 6;
        int cancelled_write_bytes = 7;

        final ProcIoData data = new ProcIoData(rchar, wchar, syscr, syscw,
                read_bytes, write_bytes, cancelled_write_bytes);

        ProcIoDataReader ioReader = mock(ProcIoDataReader.class);
        when(ioReader.read(PID)).thenReturn(data);
        Clock clock = mock(Clock.class);

        VmIoStatBuilder builder = new VmIoStatBuilder(clock, ioReader, writerID);

        VmIoStat ioData = builder.build("vmId", PID);
        assertNotNull(ioData);
        assertEquals(rchar, ioData.getCharactersRead());
        assertEquals(wchar, ioData.getCharactersWritten());
        assertEquals(syscr, ioData.getReadSyscalls());
        assertEquals(syscw, ioData.getWriteSyscalls());
    }

}

