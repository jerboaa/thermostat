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

package com.redhat.thermostat.common.utils;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.Test;

public class StreamUtilsTest {

    @Test
    public void testCopyStreams() throws IOException {
        String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum ipsum.";

        ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8")));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        StreamUtils.copyStream(new BufferedInputStream(bis), new BufferedOutputStream(bos));

        assertArrayEquals(text.getBytes(Charset.forName("UTF-8")), bos.toByteArray());
    }

    @Test
    public void testReadAll() throws IOException {
        Random r = new Random();
        final int ONE_MEGABYTE = 1024 * 1024;
        byte[] inputData = new byte[ONE_MEGABYTE];
        r.nextBytes(inputData);

        byte[] read = StreamUtils.readAll(new ByteArrayInputStream(inputData));

        assertArrayEquals(inputData, read);
    }
}

