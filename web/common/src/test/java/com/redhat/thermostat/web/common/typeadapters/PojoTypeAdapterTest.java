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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.Pojo;

public class PojoTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .create();
    }
    
    @Test
    public void canSerializePojoLists() {
        String expectedJson = "[{\"AInt\":200,\"fooString\":\"bar\"}," +
                               "{\"AInt\":5,\"fooString\":\"baz\"}," +
                               "{\"AInt\":200,\"fooString\":\"bar\"}]";
        
        FooPojo pojo1 = new FooPojo();
        pojo1.setAInt(200);
        pojo1.setFooString("bar");
        
        FooPojo pojo2 = new FooPojo();
        pojo2.setAInt(5);
        pojo2.setFooString("baz");
        
        FooPojo[] list = new FooPojo[] {
                pojo1, pojo2, pojo1
        };
        String json = gson.toJson(list);
        assertEquals(expectedJson, json);
    }
    
    @Test
    public void canDeserializePojoLists() {
        String json = "[{\"AInt\":200,\"fooString\":\"bar\"}," +
                               "{\"AInt\":5,\"fooString\":\"baz\"}," +
                               "{\"AInt\":200,\"fooString\":\"bar\"}]";
        
        FooPojo[] actual = gson.fromJson(json, FooPojo[].class);
        assertEquals(3, actual.length);
        assertEquals(200, actual[0].getAInt());
        assertEquals(5, actual[1].getAInt());
        assertEquals(200, actual[2].getAInt());
        assertEquals("bar", actual[0].getFooString());
        assertEquals("bar", actual[2].getFooString());
        assertEquals("baz", actual[1].getFooString());
    }
    
    @Test
    public void canSerializeNullPojo() {
        FooPojo foo = null;
        String jsonStr = gson.toJson(foo);
        assertEquals("null", jsonStr);
    }
    
    // Null values won't show up in the JSON string.
    @Test
    public void canSerializeNullMember() {
        String expectedJson = "{\"AInt\":200,\"fooString\":\"bar\"}";
        
        FooPojo foo = new FooPojo();
        foo.setAInt(200);
        foo.setFooString("bar");
        String jsonStr = gson.toJson(foo);
        assertEquals(expectedJson, jsonStr);
    }
    
    @Test
    public void canDeserializeWithMembersUnset() {
        String json = "{\"AInt\":200,\"boolArray\":null,\"fooString\":\"bar\"}";

        FooPojo pojo = gson.fromJson(json, FooPojo.class);
        assertNotNull(pojo);
        assertEquals(200, pojo.getAInt());
        assertEquals("bar", pojo.getFooString());
        assertNull(pojo.getBoolArray());
        
        // Do it again with unset values omitted.
        json = "{\"fooString\":\"bar\"}";
        pojo = gson.fromJson(json, FooPojo.class);
        assertNotNull(pojo);
        assertEquals("bar", pojo.getFooString());
        assertNull(pojo.getBoolArray());
        assertEquals("0 is default int val", 0, pojo.getAInt());
    }
    
    @Test
    public void canDeserializeBasic() {
        String json = "{\"AInt\":1223,\"boolArray\":[false,true,false],\"fooString\":\"fooStringValue\"}";
        
        FooPojo pojo = gson.fromJson(json, FooPojo.class);
        assertEquals(1223, pojo.getAInt());
        assertEquals("fooStringValue", pojo.getFooString());
        assertEquals(3, pojo.getBoolArray().length);
        assertFalse(pojo.getBoolArray()[0]);
        assertTrue(pojo.getBoolArray()[1]);
        assertFalse(pojo.getBoolArray()[2]);
    }
    
    @Test
    public void canDeserializeNullPojo() {
        String nullStr = "null";
        FooPojo pojo = gson.fromJson(nullStr, FooPojo.class);
        assertNull(pojo);
    }
    
    @Test
    public void canSerializeBasic() {
        String expectedJson = "{\"AInt\":1223,\"boolArray\":[false,true,false],\"fooString\":\"fooStringValue\"}";
        
        FooPojo fooPojo = new FooPojo();
        fooPojo.setBoolArray(new boolean[] { false, true, false });
        fooPojo.setAInt(1223);
        fooPojo.setFooString("fooStringValue");
        String jsonStr = gson.toJson(fooPojo);
        assertEquals(expectedJson, jsonStr);
    }
    
    @Test
    public void canSerializeRecursivePojo() {
        FooPojo fooPojo = new FooPojo();
        fooPojo.setBoolArray(new boolean[] { false, true, false });
        fooPojo.setAInt(1223);
        fooPojo.setFooString("fooStringValue");
        String fooPojoJson = gson.toJson(fooPojo);
        
        String expectedJson = "{\"myLong\":10000000000,\"pojo\":" + fooPojoJson + "}";
        RecPojo recursive = new RecPojo();
        recursive.setPojo(fooPojo);
        recursive.setMyLong(10000000000L);
        
        String jsonStr = gson.toJson(recursive);
        assertEquals(expectedJson, jsonStr);
    }
    
    @Test
    public void canDeserializeRecursivePojo() {
        String json = "{\"myLong\":10000000000,\"pojo\":" +
                                         "{\"AInt\":1223,\"boolArray\":[false,true,false],\"fooString\":\"fooStringValue\"}}";
        
        RecPojo rec = gson.fromJson(json, RecPojo.class);
        
        assertEquals(10000000000L, rec.getMyLong());
        assertNotNull(rec.getPojo());
        
        FooPojo pojo = rec.getPojo();
        assertEquals(1223, pojo.getAInt());
        assertEquals("fooStringValue", pojo.getFooString());
        assertEquals(3, pojo.getBoolArray().length);
        assertFalse(pojo.getBoolArray()[0]);
        assertTrue(pojo.getBoolArray()[1]);
        assertFalse(pojo.getBoolArray()[2]);
    }
    
    @Test
    public void canSerializeDeserializeBasic() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation("testing");
        agentInfo.setAlive(true);
        
        String jsonStr = gson.toJson(agentInfo);
        
        AgentInformation actual = gson.fromJson(jsonStr, AgentInformation.class);
        
        assertEquals("testing", actual.getAgentId());
        assertEquals(true, actual.isAlive());
    }
    
    @Test
    public void canSerializeDeserializeArray() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation("testing");
        agentInfo.setAlive(true);
        AgentInformation[] agentInfos = new AgentInformation[] {
                agentInfo
        };
        
        String jsonStr = gson.toJson(agentInfos);
        
        AgentInformation[] actual = gson.fromJson(jsonStr, AgentInformation[].class);
        
        assertEquals("testing", actual[0].getAgentId());
        assertEquals(true, actual[0].isAlive());
    }
    
    @Test
    public void canSerializeDeserializeAggregateCount() {
        long expectedCount = 3333000333L;
        AggregateCount count = new AggregateCount();
        count.setCount(expectedCount);
        String jsonStr = gson.toJson(count);
        // now do the reverse
        AggregateCount c2 = gson.fromJson(jsonStr, AggregateCount.class);
        assertEquals(expectedCount, c2.getCount());
    }
    
    @Entity
    public static class FooPojo implements Pojo {
        
        private String fooString;
        private int fooInt;
        private boolean[] boolArray;
        
        @Persist
        public String getFooString() {
            return fooString;
        }
        
        @Persist
        public void setFooString(String fooString) {
            this.fooString = fooString;
        }
        
        @Persist
        public int getAInt() {
            return fooInt;
        }
        
        @Persist
        public void setAInt(int fooInt) {
            this.fooInt = fooInt;
        }
        
        @Persist
        public boolean[] getBoolArray() {
            return boolArray;
        }
        
        @Persist
        public void setBoolArray(boolean[] boolArray) {
            this.boolArray = boolArray;
        }
        
        
    }
    
    @Entity
    public static class RecPojo implements Pojo {
        
        private long myLong;
        private FooPojo pojo;
        
        @Persist
        public long getMyLong() {
            return myLong;
        }
        
        @Persist
        public void setMyLong(long myLong) {
            this.myLong = myLong;
        }
        
        @Persist
        public FooPojo getPojo() {
            return pojo;
        }
        
        @Persist
        public void setPojo(FooPojo pojo) {
            this.pojo = pojo;
        }
        
    }
}
