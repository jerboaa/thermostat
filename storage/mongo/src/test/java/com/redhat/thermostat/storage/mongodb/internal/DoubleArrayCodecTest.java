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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.junit.Test;

public class DoubleArrayCodecTest {
    
    private final double DELTA = 0.0001;

    @Test
    public void canEncodeDoubleArray() {
        double[] values = new double[] {
           Math.PI, 123.232, -2.32
        };
        DoubleArrayCodec codec = new DoubleArrayCodec();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BsonWriter writer = new JsonWriter(new PrintWriter(baos));
        EncoderContext ctxt = EncoderContext.builder().build();
        
        writer.writeStartDocument();
        writer.writeName("foo");
        codec.encode(writer, values, ctxt);
        writer.writeEndDocument();
        writer.flush();
        String json = new String(baos.toByteArray());
        assertTrue(json.contains("\"foo\""));
        assertTrue(json.contains("[" + values[0] + ", " + values[1] + ", " + values[2] + "]"));
    }
    
    @Test
    public void canDecodeDoubleArray() {
        String json = "{ \"foo\" : [3.141592653589793, 123.232, -2.32] }";
        BsonReader reader = new JsonReader(json);
        reader.readStartDocument();
        String name = reader.readName();
        assertEquals("foo", name);
        DoubleArrayCodec codec = new DoubleArrayCodec();
        double[] decoded = codec.decode(reader, DecoderContext.builder().build());
        reader.readEndDocument();
        assertEquals(Math.PI, decoded[0], DELTA);
        assertEquals(123.232, decoded[1], DELTA);
        assertEquals(-2.32, decoded[2], DELTA);
    }
}
