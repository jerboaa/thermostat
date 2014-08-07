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

package com.redhat.thermostat.storage.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.redhat.thermostat.storage.internal.TimeStampedPrintWriter.DateSource;

public class TimeStampedPrintWriterTest {

    @Test
    public void printsWithTimestamp() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DateSource calendar = mock(DateSource.class);
        Date d = new Date();
        when(calendar.getDate()).thenReturn(d);
        TimeStampedPrintWriter pw = new TimeStampedPrintWriter(out, calendar);
        String text = "foo";
        pw.println(text);
        pw.close();
        
        SimpleDateFormat format = new SimpleDateFormat(TimeStampedPrintWriter.ISO_FORMAT_WITH_MILLIS);
        String timestamp = format.format(d);
        String expected = timestamp + "|" + text + "\n";
        String actual = out.toString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void multipleCallsProducesFreshTimestamp() throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TimeStampedPrintWriter pw = new TimeStampedPrintWriter(out);
        String text = "foo";
        pw.println(text);
        pw.close();
        
        // be sure to be 3 milis later
        Thread.sleep(3);
        
        String first = out.toString();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        pw = new TimeStampedPrintWriter(bout);
        pw.println(text);
        pw.close();
        
        String second = bout.toString();
        assertFalse(first + " " + second, first.equals(second));
    }
}
