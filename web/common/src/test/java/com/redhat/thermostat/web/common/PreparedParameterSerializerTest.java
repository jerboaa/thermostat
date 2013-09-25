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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.model.VmInfo.KeyValuePair;

public class PreparedParameterSerializerTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Pojo.class, new ThermostatGSONConverter())
            .registerTypeAdapter(
                PreparedParameter.class,
                new PreparedParameterSerializer()).create();
    }
    
    @Test
    public void canDeserializeBasic() {
        // String
        String jsonStr = "{ \"type\": \"java.lang.String\" , \"value\": \"testing\" , \"isArray\": false}";
        PreparedParameter param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String.class, param.getType());
        assertEquals("testing", param.getValue());
        assertFalse(param.isArrayType());
        // Integer
        jsonStr = "{ \"type\": \"java.lang.Integer\" , \"value\": -1 , \"isArray\": false}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Integer.class, param.getType());
        assertTrue(param.getValue() instanceof Integer);
        assertEquals(-1, param.getValue());
        assertFalse(param.isArrayType());
        // Long
        jsonStr = "{ \"type\": \"java.lang.Long\" , \"value\": -10 , \"isArray\": false}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Long.class, param.getType());
        assertTrue(param.getValue() instanceof Long);
        assertEquals(-10L, param.getValue());
        assertFalse(param.isArrayType());
        jsonStr = "{ \"type\": \"java.lang.Long\" , \"value\": 30000000003 , \"isArray\": false}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Long.class, param.getType());
        assertTrue(param.getValue() instanceof Long);
        assertEquals(30000000003L, param.getValue());
        assertFalse(param.isArrayType());
        // Boolean
        jsonStr = "{ \"type\": \"java.lang.Boolean\" , \"value\": true , \"isArray\": false}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(Boolean.class, param.getType());
        assertTrue(param.getValue() instanceof Boolean);
        assertEquals(true, param.getValue());
        assertFalse(param.isArrayType());
        // String[]
        String strArrayVal = "[ \"testing1\", \"testing2\", \"3\" ]";
        jsonStr = "{ \"type\": \"java.lang.String\" , \"value\": " + strArrayVal + " , \"isArray\": true}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String.class, param.getType());
        assertTrue(param.isArrayType());
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
        String expected = "{\"value\":\"testing\",\"type\":\"java.lang.String\",\"isArray\":false}";
        PreparedParameter param = new PreparedParameter();
        param.setType(String.class);
        param.setValue("testing");
        param.setArrayType(false);
        String actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Integer
        expected = "{\"value\":-1,\"type\":\"java.lang.Integer\",\"isArray\":false}";
        param.setType(Integer.class);
        param.setValue(-1);
        param.setArrayType(false);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Long
        expected = "{\"value\":30000000003,\"type\":\"java.lang.Long\",\"isArray\":false}";
        param.setType(Long.class);
        param.setValue(30000000003L);
        param.setArrayType(false);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // boolean
        expected = "{\"value\":true,\"type\":\"java.lang.Boolean\",\"isArray\":false}";
        param.setType(Boolean.class);
        param.setValue(true);
        param.setArrayType(false);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // String[]
        String strArrayVal = "[\"testing1\",\"testing2\",\"3\"]";
        expected = "{\"value\":" + strArrayVal + ",\"type\":\"java.lang.String\",\"isArray\":true}";
        param.setType(String.class);
        param.setArrayType(true);
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
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeIntegerArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Integer.class);
        // it's important for the expected type to be of primitive array type,
        // rather than Integer[]. we want the serializer to deserialize to
        // primitive types if possible. asserted in method assertParameterEquals()
        // Note that model classes use primitive array types as well.
        expected.setValue(new int[] { 0, 3, 20 });
        expected.setArrayType(true);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeDouble() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Double.class);
        expected.setValue(Math.E);
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeDoubleArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Double.class);
        // it's important for the expected type to be of primitive array type,
        // rather than Double[]. we want the serializer to deserialize to
        // primitive types if possible. asserted in method assertParameterEquals()
        // Note that model classes use primitive array types as well.
        expected.setValue(new double[] { 3.3, 1.0, Math.PI });
        expected.setArrayType(true);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeLong() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Long.class);
        expected.setValue(30000000003L);
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeLongArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Long.class);
        // it's important for the expected type to be of primitive array type,
        // rather than Long[]. we want the serializer to deserialize to
        // primitive types if possible. asserted in method assertParameterEquals()
        // Note that model classes use primitive array types as well.
        expected.setValue(new long[] { 3000000000L, 3, 20 });
        expected.setArrayType(true);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeString() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(String.class);
        expected.setValue("testing");
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeStringArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(String.class);
        expected.setArrayType(true);
        String[] expectedArray = new String[] {
                "one", "two", "three"      
        };
        expected.setValue(expectedArray);
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
    
    @Test
    public void canSerializeDeserializeBooleanArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(Boolean.class);
        // it's important for the expected type to be of primitive array type,
        // rather than Boolean[]. we want the serializer to deserialize to
        // primitive types if possible. asserted in method assertParameterEquals()
        // Note that model classes use primitive array types as well.
        expected.setValue(new boolean[] { true, false, false, true });
        expected.setArrayType(true);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    
    @Test
    public void canSerializeDeserializePojos() {
        PreparedParameter expected = new PreparedParameter();
        AgentInformation info = new AgentInformation("foo-writer");
        expected.setType(info.getClass());
        expected.setValue(info);
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
        
        info = new AgentInformation("some-writer");
        info.setAlive(true);
        info.setConfigListenAddress("127.0.0.1:12000");
        info.setStartTime(System.currentTimeMillis());
        info.setStopTime(System.currentTimeMillis());
        expected = new PreparedParameter();
        expected.setType(info.getClass());
        expected.setValue(info);
        expected.setArrayType(false);
        jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeInnerClassPojoTypes() {
        PreparedParameter expected = new PreparedParameter();
        KeyValuePair pair = new KeyValuePair();
        pair.setKey("foo");
        pair.setValue("bar");
        expected.setType(pair.getClass());
        expected.setValue(pair);
        expected.setArrayType(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        
        assertEquals(expected.getType(), actual.getType());
        assertTrue(expected.isArrayType() == actual.isArrayType());
        assertTrue(actual.getValue() instanceof KeyValuePair);
        KeyValuePair actualPair = (KeyValuePair)actual.getValue();
        assertEquals(pair.getKey(), actualPair.getKey());
        assertEquals(pair.getValue(), actualPair.getValue());
    }
    
    @Test
    public void canSerializeDeserializePojoLists() {
        AgentInformation info1 = new AgentInformation("foo-writer");
        AgentInformation info2 = new AgentInformation("some-writer");
        info2.setAlive(true);
        info2.setConfigListenAddress("127.0.0.1:12000");
        info2.setStartTime(System.currentTimeMillis());
        info2.setStopTime(System.currentTimeMillis());
        AgentInformation[] infos = new AgentInformation[] {
                info1, info2
        };
        PreparedParameter param = new PreparedParameter();
        param.setArrayType(true);
        param.setType(AgentInformation.class);
        param.setValue(infos);
        String jsonStr = gson.toJson(param, PreparedParameter.class);
        assertParameterEquals(param, jsonStr);
    }
    
    private void assertParameterEquals(PreparedParameter expected,
            String jsonStr) {
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.isArrayType(), actual.isArrayType());
        if (actual.isArrayType()) {
            // compare element by element
            Object values = actual.getValue();
            Object expectedVals = expected.getValue();
            Class<?> expectedType = expectedVals.getClass();
            int expectedLength = Array.getLength(expectedVals);
            int actualLength = Array.getLength(values);
            assertEquals(expectedLength, actualLength);
            // Make sure the deserialized array is of the correct expected type
            assertTrue(values.getClass() == expectedType);
            for (int i = 0; i < expectedLength; i++) {
                Object exp = Array.get(expectedVals, i);
                Object act = Array.get(values, i);
                assertEquals(exp, act);
            }
        } else {
            assertEquals(expected.getValue(), actual.getValue());
        }
    }
}

