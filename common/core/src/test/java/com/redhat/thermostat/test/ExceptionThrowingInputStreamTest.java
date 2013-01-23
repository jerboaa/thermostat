/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExceptionThrowingInputStreamTest {

    private ExceptionThrowingInputStream in;
    
    private PipedOutputStream out;

    @Before
    public void setUp() throws IOException {
        out = new PipedOutputStream();
        in = new ExceptionThrowingInputStream(out);
    }

    @After
    public void tearDown() {
        in = null;
        out = null;
    }

    @Test
    public void testRead() throws IOException {
        out.write(2);
        out.flush();

        int read = in.read();

        assertEquals(2, read);
    }

    @Test
    public void testReadException() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.read();
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testReadBytes() throws IOException {
        
        String test = "test";
        out.write(test.getBytes());
        out.flush();

        byte[] read = new byte[4];
        int numRead = in.read(read);

        assertEquals(4, numRead);
        assertEquals("test", new String(read));
    }

    @Test
    public void testReadBytesException() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.read(new byte[4]);
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testReadBytes2() throws IOException {
        
        String test = "test";
        out.write(test.getBytes());
        out.flush();

        byte[] read = new byte[10];
        int numRead = in.read(read, 3, 4);

        assertEquals(4, numRead);
        assertEquals("test", new String(read, 3, 4));
    }

    @Test
    public void testReadBytesException2() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.read(new byte[10], 3, 4);
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testSkip() throws IOException {
        
        String test = "test";
        out.write(test.getBytes());
        out.flush();

        byte[] read = new byte[4];
        in.skip(1);
        int numRead = in.read(read);

        assertEquals(3, numRead);
        assertEquals("est", new String(read, 0, 3));
    }

    @Test
    public void testSkipException() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.skip(1);
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testAvailable() throws IOException {
        
        String test = "test";
        out.write(test.getBytes());
        out.flush();

        int available = in.available();

        assertEquals(4, available);
    }

    @Test
    public void testAvailableException() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.available();
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testClose() throws IOException {
        
        String test = "test";
        out.write(test.getBytes());
        out.flush();

        in.close();

        try {
            in.read();
        } catch (IOException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testCloseException() throws IOException {
        IOException ex = new IOException();
        in.setException(ex);
        try {
            in.close();
        } catch (IOException io) {
            assertSame(ex, io);
        }
    }

    @Test
    public void testMarkSupported() {
        assertFalse(in.markSupported());
    }

    @Test(expected=IOException.class)
    public void testReset() throws IOException {
        in.reset();
    }

    @Test
    public void testMark() throws IOException {
        // This method should do nothing. We simply check that no exception is thrown,
        // and otherwise the stream works as normal.
        in.mark(1);

        out.write(2);
        out.flush();

        int read = in.read();

        assertEquals(2, read);
    }
}

