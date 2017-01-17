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

package org.jboss.byteman.thermostat.helper;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.jboss.byteman.thermostat.helper.BytemanMetric;
import org.junit.Test;

public class BytemanMetricTest {
    
    @Test
    public void testToJsonMarkerNullDataNull() {
        BytemanMetric recEmptyData = new BytemanMetric(null, null);
        long timestamp = recEmptyData.getTimestamp();
        assertEquals("{" +
                        "\"marker\":null," +
                        "\"timestamp\":\"" + timestamp + "\"," +
                        "\"data\":null" +
                     "}", recEmptyData.toJson());
    }
    
    @Test
    public void testToJsonNull() {
        BytemanMetric recEmptyData = new BytemanMetric("baz", null);
        long timestamp = recEmptyData.getTimestamp();
        assertEquals("{" +
                        "\"marker\":\"baz\"," +
                        "\"timestamp\":\"" + timestamp + "\"," +
                        "\"data\":null" +
                     "}", recEmptyData.toJson());
    }
    
    @Test
    public void testToJsonMapDataNonNull() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("foo1", "ba\"r1");
        data.put("foo2", 42);
        data.put("foo3", 42.000);
        data.put("foo4", false);
        data.put("foo5", "[]");
        data.put("key", null);
        BytemanMetric recData = new BytemanMetric("baz", data);
        long timestamp = recData.getTimestamp();
        assertEquals(
                "{" +
                    "\"marker\":\"baz\"," +
                    "\"timestamp\":\"" + timestamp + "\"," +
                    "\"data\":{" +
                    "\"foo1\":\"ba\\\"r1\"," +
                    "\"foo2\":42," +
                    "\"foo3\":42.0," +
                    "\"foo4\":false," +
                    "\"foo5\":\"[]\"," +
                    "\"key\":null" +
                    "}" +
                "}", recData.toJson());
    }
    
    @Test
    public void testToJsonMarkerNullDataNonNull() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("foo1", "bar1");
        BytemanMetric recData = new BytemanMetric(null, data);
        long timestamp = recData.getTimestamp();
        // marker will be the default string
        assertEquals(
                "{" +
                    "\"marker\":null," +
                    "\"timestamp\":\"" + timestamp + "\"," +
                    "\"data\":{" +
                        "\"foo1\":\"bar1\"" +
                    "}" +
                "}", recData.toJson());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedObjectJson() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("foo1", new Object());
        BytemanMetric metric = new BytemanMetric("something", data);
        metric.toJson(); // throws UnsupportedOperationException
    }
    
    @Test
    public void markerContainsQuotes() {
        BytemanMetric metric = new BytemanMetric("something \"quoted\"", null);
        long timestamp = metric.getTimestamp();
        assertEquals("{" +
                    "\"marker\":\"something \\\"quoted\\\"\"," +
                    "\"timestamp\":\"" + timestamp + "\"," +
                    "\"data\":null" +
                "}", metric.toJson());
    }
    
    @Test
    public void testDataKeyContainsQuotes() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("foo\"1", "bar1");
        BytemanMetric recData = new BytemanMetric("baz", data);
        long timestamp = recData.getTimestamp();
        assertEquals(
                "{" +
                    "\"marker\":\"baz\"," +
                    "\"timestamp\":\"" + timestamp + "\"," +
                    "\"data\":{" +
                    "\"foo\\\"1\":\"bar1\"" +
                    "}" +
                "}", recData.toJson());
    }
}
