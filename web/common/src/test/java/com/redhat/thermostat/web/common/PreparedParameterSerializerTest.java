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

package com.redhat.thermostat.web.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.PreparedParameter;

public class PreparedParameterSerializerTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder().registerTypeAdapter(
                PreparedParameter.class,
                new PreparedParameterSerializer()).create();
    }
    
    @Test
    public void canDeserializeBasic() {
        // String
        String jsonStr = "{ \"type\": \"java.lang.String\" , \"value\": \"testing\"}";
        PreparedParameter param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String.class, param.getType());
        assertEquals("testing", param.getValue());
        // Integer
        jsonStr = "{ \"type\": \"java.lang.Integer\" , \"value\": -1}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Integer.class, param.getType());
        assertTrue(param.getValue() instanceof Integer);
        assertEquals(-1, param.getValue());
        // Long
        jsonStr = "{ \"type\": \"java.lang.Long\" , \"value\": -10}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Long.class, param.getType());
        assertTrue(param.getValue() instanceof Long);
        assertEquals(-10L, param.getValue());
        jsonStr = "{ \"type\": \"java.lang.Long\" , \"value\": 30000000003}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Long.class, param.getType());
        assertTrue(param.getValue() instanceof Long);
        assertEquals(30000000003L, param.getValue());
        // Boolean
        jsonStr = "{ \"type\": \"java.lang.Boolean\" , \"value\": true}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Boolean.class, param.getType());
        assertTrue(param.getValue() instanceof Boolean);
        assertEquals(true, param.getValue());
        // String[]
        String strArrayVal = "[ \"testing1\", \"testing2\", \"3\" ]";
        jsonStr = "{ \"type\": \"java.lang.String[]\" , \"value\": " + strArrayVal + "}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String[].class, param.getType());
        assertTrue(param.getValue() instanceof String[]);
        String[] vals = (String[])param.getValue();
        assertEquals(3, vals.length);
        assertEquals("testing1", vals[0]);
        assertEquals("testing2", vals[1]);
        assertEquals("3", vals[2]);
    }
    
    @Test
    public void failsDeserializationWrongTypeClass() {
        String jsonStr = "{ \"type\": \"java.io.File\" , \"value\": true}";
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            fail("should have failed to serialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertTrue(cause.getMessage().contains("Illegal type of parameter"));
        }
    }
    
    @Test
    public void canSerializeBasic() {
        // String
        String expected = "{\"value\":\"testing\",\"type\":\"java.lang.String\"}";
        PreparedParameter param = new PreparedParameter();
        param.setType(String.class);
        param.setValue("testing");
        String actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Integer
        expected = "{\"value\":-1,\"type\":\"java.lang.Integer\"}";
        param.setType(Integer.class);
        param.setValue(-1);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Long
        expected = "{\"value\":30000000003,\"type\":\"java.lang.Long\"}";
        param.setType(Long.class);
        param.setValue(30000000003L);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // boolean
        expected = "{\"value\":true,\"type\":\"java.lang.Boolean\"}";
        param.setType(Boolean.class);
        param.setValue(true);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // String[]
        String strArrayVal = "[\"testing1\",\"testing2\",\"3\"]";
        expected = "{\"value\":" + strArrayVal + ",\"type\":\"java.lang.String[]\"}";
        param.setType(String[].class);
        String[] array = new String[] {
                "testing1", "testing2", "3"
        };
        param.setValue(array);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
    }
    
    @Test
    public void canSerializeDeserializeInteger() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Integer.class);
        expected.setValue(3);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }

    @Test
    public void canSerializeDeserializeLong() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Long.class);
        expected.setValue(30000000003L);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeString() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(String.class);
        expected.setValue("testing");
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeBoolean() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Boolean.class);
        expected.setValue(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
        
        expected = new PreparedParameter();
        expected.setType(Boolean.class);
        expected.setValue(true);
        jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }

    private void assertParameterEquals(PreparedParameter expected,
            String jsonStr) {
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getValue(), actual.getValue());
    }
    
    @Test
    public void canSerializeDeserializeStringArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(String[].class);
        String[] expectedArray = new String[] {
          "one", "two", "three"      
        };
        expected.setValue(expectedArray);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(expected.getType(), actual.getType());
        String[] actualArray = (String[])actual.getValue();
        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], actualArray[i]);
        }
    }
    
}
