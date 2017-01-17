/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.byteman.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.BytemanMetricTypeAdapterFactory;
import com.redhat.thermostat.vm.byteman.common.JsonHelper;

public class BytemanMetricTypeAdapterTest {
    
    private static final double DELTA = 0.001;
    
    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new BytemanMetricTypeAdapterFactory())
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
    }

    @Test
    public void canDeserializeArrayOfMetrics() {
        String json = JsonHelper.buildJsonArray(10);
        BytemanMetric[] metrics = gson.fromJson(json, BytemanMetric[].class);
        assertEquals(10, metrics.length);
        for (int i = 0; i < 10; i++) {
            assertEquals("baz" + i, metrics[i].getMarker());
            assertEquals(1234567890 + i, metrics[i].getTimeStamp());
            Map<String, Object> dataAsMap = metrics[i].getDataAsMap();
            assertFalse((Boolean)dataAsMap.get("foo4"));
            assertEquals("[]", (String)dataAsMap.get("foo5"));
            double foo3Val = (double)dataAsMap.get("foo3");
            String foo3Str = String.format("42.%d", i);
            double expectedFoo3Val = Double.parseDouble(foo3Str); 
            assertEquals(expectedFoo3Val, foo3Val, DELTA);
            double foo2Val = (double)dataAsMap.get("foo2");
            String foo2Str = String.format("42%d.0", i);
            double expectedFoo2Val = Double.parseDouble(foo2Str);
            assertEquals(expectedFoo2Val, foo2Val, DELTA);
            assertEquals("ba\"r1", (String)dataAsMap.get("foo1"));
            assertEquals("Expected 7 key value pairs", 7, dataAsMap.keySet().size());
            assertTrue(dataAsMap.containsKey("key"));
            assertNull("value for \"key\" is null", dataAsMap.get("key"));
            Double rawLong = (Double)dataAsMap.get("long");
            long longVal = rawLong.longValue();
            assertEquals(10_000_000_001L, longVal);
        }
    }
    
    /*
     * This is a test which makes sure that html characters don't get
     * escaped. We were seeing instances where the following happened which this
     * test is trying to catch:
     * 
     * before JSON deserialization: {"key": "value = 'foo', 'bar', 'baz'"}
     * after JSON deserialization: {"key"; "value \u003d \u0027foo\u0027, \u0027bar\u0027, \u0027baz\u0027"}
     */
    @Test
    public void canDeserializeMetricWithSpecialChar() {
        String json = "{\n" +
                "    \"marker\": \"marker\",\n" +
                "    \"timestamp\":\"30\",\n" +
                "    \"data\": {\n" +
                "        \"key\": \"value = 'foo', 'bar', 'baz'\"\n" +
                "    }\n" +
                "}";
        BytemanMetric metric = gson.fromJson(json, BytemanMetric.class);
        assertEquals("{\"key\":\"value = 'foo', 'bar', 'baz'\"}", metric.getData());
        Map<String, Object> dataAsMap = metric.getDataAsMap();
        assertEquals("value = 'foo', 'bar', 'baz'", dataAsMap.get("key"));
    }

}
