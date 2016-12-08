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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import org.junit.Test;

import com.redhat.thermostat.agent.utils.linux.ProcDataSource;

public class ProcIoDataReaderTest {

    @Test
    public void verifyIsParsedCorrectly() throws Exception {
        final int SOME_PID = 0;
        String fileContents = "" +
                "rchar: 19961133\n" +
                "wchar: 2451715\n" +
                "syscr: 17880\n" +
                "syscw: 13870\n" +
                "read_bytes: 21004288\n" +
                "write_bytes: 811008\n" +
                "cancelled_write_bytes: 16384\n";
        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getIoReader(SOME_PID)).thenReturn(new StringReader(fileContents));

        ProcIoData parsedData = new ProcIoDataReader(dataSource).read(SOME_PID);

        assertEquals(19961133, parsedData.rchar);
        assertEquals(2451715, parsedData.wchar);
        assertEquals(17880, parsedData.syscr);
        assertEquals(13870, parsedData.syscw);
        assertEquals(21004288, parsedData.read_bytes);
        assertEquals(811008, parsedData.write_bytes);
        assertEquals(16384, parsedData.cancelled_write_bytes);

    }
}
