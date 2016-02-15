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

package com.redhat.thermostat.web.common.typeadapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo.KeyValuePair;

public class PreparedParameterTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                .create();
    }
    
    @Test
    public void canDeserializeBasic() {
        // String
        String jsonStr = "{ \"type\": \"java.lang.String\" , \"value\": \"testing\" }";
        PreparedParameter param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String.class, param.getType());
        assertEquals("testing", param.getValue());
        // Integer
        jsonStr = "{ \"type\": \"int\" , \"value\": -1}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(int.class, param.getType());
        assertEquals(-1, param.getValue());
        // Long
        jsonStr = "{ \"type\": \"long\" , \"value\": -10}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(long.class, param.getType());
        assertEquals(-10L, param.getValue());
        jsonStr = "{ \"type\": \"long\" , \"value\": 30000000003}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(long.class, param.getType());
        assertEquals(30000000003L, param.getValue());
        // Boolean
        jsonStr = "{ \"type\": \"boolean\" , \"value\": true}";
        param = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(boolean.class, param.getType());
        assertEquals(true, param.getValue());
        // String[]
        String strArrayVal = "[ \"testing1\", \"testing2\", \"3\" ]";
        jsonStr = "{ \"type\": \"[Ljava.lang.String;\" , \"value\": " + strArrayVal + "}";
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
    public void allowsBooleanListNullDeserialization() {
        String jsonStr = "{ \"type\": \"[Z\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(boolean[].class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void allowsIntListNullDeserialization() {
        String jsonStr = "{ \"type\": \"[I\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(int[].class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void allowsLongListNullDeserialization() {
        String jsonStr = "{ \"type\": \"[J\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(long[].class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void allowsDoubleListNullDeserialization() {
        String jsonStr = "{ \"type\": \"[D\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(double[].class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void allowsStringNullDeserialization() {
        String jsonStr = "{ \"type\": \"java.lang.String\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String.class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void allowsStringListNullDeserialization() {
        String jsonStr = "{ \"type\": \"[Ljava.lang.String;\" , \"value\": null}";
        PreparedParameter p = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(String[].class, p.getType());
        assertEquals(null, p.getValue());
    }
    
    @Test
    public void rejectNullForBooleanPrimitive() {
        String jsonStr = "{ \"type\": \"boolean\" , \"value\": null}";
        doPrimitiveNullTest(jsonStr, "boolean");
    }
    
    @Test
    public void rejectNullForIntPrimitive() {
        String jsonStr = "{ \"type\": \"int\" , \"value\": null}";
        doPrimitiveNullTest(jsonStr, "int");
    }
    
    @Test
    public void rejectNullForDoublePrimitive() {
        String jsonStr = "{ \"type\": \"double\" , \"value\": null}";
        doPrimitiveNullTest(jsonStr, "double");
    }
    
    @Test
    public void rejectNullForLongPrimitive() {
        String jsonStr = "{ \"type\": \"long\" , \"value\": null}";
        doPrimitiveNullTest(jsonStr, "long");
    }
    
    private void doPrimitiveNullTest(String jsonStr, String typeName) {
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            // The Java language spec does not permit this
            fail(typeName + " null primitive should not deserialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertEquals(typeName + " primitive does not accept a null value!", cause.getMessage());
        }
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
    public void failsDeserializationIfStringForInt() {
        String jsonStr = "{ \"type\": \"int\" , \"value\":\"testing\"}";
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            fail("should have failed to serialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertTrue(cause instanceof NumberFormatException);
        }
    }
    
    @Test
    public void failsDeserializationIfBooleanForInt() {
        String jsonStr = "{ \"type\": \"int\" , \"value\": true}";
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            fail("should have failed to serialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException);
        }
    }
    
    @Test
    public void failsDeserializationIfIntForIntList() {
        String jsonStr = "{ \"type\": \"[I\" , \"value\": -1}";
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            fail("should have failed to serialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException);
        }
    }
    
    @Test
    public void failsDeserializationIfDoubleForInt() {
        String jsonStr = "{ \"type\": \"int\" , \"value\": -1.3}";
        try {
            gson.fromJson(jsonStr, PreparedParameter.class);
            fail("should have failed to serialize");
        } catch (Exception e) {
            // pass
            Throwable cause = e.getCause();
            assertTrue(cause instanceof NumberFormatException);
        }
    }
    
    @Test
    public void canSerializeBasic() {
        // String
        String expected = "{\"type\":\"java.lang.String\",\"value\":\"testing\"}";
        PreparedParameter param = new PreparedParameter();
        param.setType(String.class);
        param.setValue("testing");
        String actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Integer
        expected = "{\"type\":\"int\",\"value\":-1}";
        param.setType(int.class);
        param.setValue(-1);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // Long
        expected = "{\"type\":\"long\",\"value\":30000000003}";
        param.setType(long.class);
        param.setValue(30000000003L);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // boolean
        expected = "{\"type\":\"boolean\",\"value\":true}";
        param.setType(boolean.class);
        param.setValue(true);
        actual = gson.toJson(param);
        assertEquals(expected, actual);
        // String[]
        String strArrayVal = "[\"testing1\",\"testing2\",\"3\"]";
        expected = "{\"type\":\"[Ljava.lang.String;\",\"value\":" + strArrayVal + "}";
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
        expected.setType(int.class);
        expected.setValue(3);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeIntegerArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(int[].class);
        expected.setValue(new int[] { 0, 3, 20 });
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeDouble() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(double.class);
        expected.setValue(Math.E);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeDoubleArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(double[].class);
        expected.setValue(new double[] { 3.3, 1.0, Math.PI });
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeLong() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(long.class);
        expected.setValue(30000000003L);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeLongArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(long[].class);
        expected.setValue(new long[] { 3000000000L, 3, 20 });
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
    public void canSerializeDeserializeStringArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(String[].class);
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
        expected.setType(boolean.class);
        expected.setValue(false);
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
        
        expected = new PreparedParameter();
        expected.setType(boolean.class);
        expected.setValue(true);
        jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    @Test
    public void canSerializeDeserializeBooleanArray() {
        PreparedParameter expected = new PreparedParameter();
        expected.setType(boolean[].class);
        expected.setValue(new boolean[] { true, false, false, true });
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
    }
    
    
    @Test
    public void canSerializeDeserializePojos() {
        PreparedParameter expected = new PreparedParameter();
        AgentInformation info = new AgentInformation("foo-writer");
        expected.setType(info.getClass());
        expected.setValue(info);
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
        jsonStr = gson.toJson(expected, PreparedParameter.class);
        assertParameterEquals(expected, jsonStr);
        
        // null pojo
        expected = new PreparedParameter();
        expected.setType(AgentInformation.class);
        expected.setValue(null);
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
        String jsonStr = gson.toJson(expected, PreparedParameter.class);
        
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        
        assertEquals(expected.getType(), actual.getType());
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
        param.setType(AgentInformation.class);
        param.setValue(infos);
        String jsonStr = gson.toJson(param, PreparedParameter.class);
        assertParameterEquals(param, jsonStr);
    }
    
    private void assertParameterEquals(PreparedParameter expected,
            String jsonStr) {
        PreparedParameter actual = gson.fromJson(jsonStr, PreparedParameter.class);
        assertEquals(expected.getType(), actual.getType());
        if (actual.getValue() != null && actual.getValue().getClass().isArray()) {
            // compare element by element
            Object values = actual.getValue();
            Object expectedVals = expected.getValue();
            int expectedLength = Array.getLength(expectedVals);
            int actualLength = Array.getLength(values);
            assertEquals(expectedLength, actualLength);
            // Make sure the deserialized array is of the correct expected type
            assertEquals(expectedVals.getClass(), values.getClass());
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

