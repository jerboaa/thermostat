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


package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.mongodb.internal.MongoPojoConverter;

public class MongoPojoConverterTest {

    @Entity
    public static class SimplePojo implements Pojo {
        
        private String test;
        private String ignored;


        @Persist
        public String getTest() {
            return test;
        }

        @Persist
        public void setTest(String test) {
            this.test = test;
        }

        public String getIgnored() {
            return ignored;
        }

        public void setIgnored(String ignored) {
            this.ignored = ignored;
        }
        
    }

    @Entity
    public static class NestedPojo extends SimplePojo {

        private SimplePojo nested;

        @Persist
        public SimplePojo getNested() {
            return nested;
        }

        @Persist
        public void setNested(SimplePojo nested) {
            this.nested = nested;
        }
    }

    @Entity
    public static class IndexedPojo extends SimplePojo {

        private SimplePojo[] indexed;

        @Persist
        public SimplePojo[] getIndexed() {
            return indexed;
        }

        @Persist
        public void setIndexed(SimplePojo[] indexed) {
            this.indexed = indexed;
        }
    }

    @Entity
    public static class PrimitiveIndexedPojo extends SimplePojo {

        private int[] indexed;

        @Persist
        public int[] getIndexed() {
            return indexed;
        }

        @Persist
        public void setIndexed(int[] indexed) {
            this.indexed = indexed;
        }
    }

    public static class BrokenPojo1 extends SimplePojo {
        private int broken;

        @Persist
        public void setBroken(int broken) {
            this.broken = broken;
        }
    }

    public static class BrokenPojo2 extends SimplePojo {
        private int broken;

        @Persist
        public void setBroken(int broken) {
            this.broken = broken;
        }

        public int getBroken() {
            return broken;
        }
    }

    @Test
    public void testConvertSimplePojoToMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        SimplePojo obj = new SimplePojo();
        obj.setTest("fluff");
        DBObject dbObject = conv.convertPojoToMongo(obj);
        assertEquals(1, dbObject.keySet().size());
        assertEquals("fluff", dbObject.get("test"));
    }

    @Test
    public void testConvertSimplePojoFromMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject dbObj = new BasicDBObject();
        dbObj.put("test", "fluff");
        SimplePojo obj = conv.convertMongoToPojo(dbObj, SimplePojo.class);
        assertEquals("fluff", obj.getTest());
        assertNull(obj.getIgnored());
    }

    @Test(expected=StorageException.class)
    public void testConvertSimplePojoFromMongoExtraProperty() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject dbObj = new BasicDBObject();
        dbObj.put("test", "fluff");
        dbObj.put("foo", "bar");
        conv.convertMongoToPojo(dbObj, SimplePojo.class);
    }

    @Test(expected=StorageException.class)
    public void testConvertSimplePojoFromMongoBrokenPojo1() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject dbObj = new BasicDBObject();
        dbObj.put("broken", "foo");
        conv.convertMongoToPojo(dbObj, BrokenPojo1.class);
    }


    @Test(expected=StorageException.class)
    public void testConvertSimplePojoFromMongoBrokenPojo2() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject dbObj = new BasicDBObject();
        dbObj.put("broken", "foo");
        conv.convertMongoToPojo(dbObj, BrokenPojo2.class);
    }

    @Test
    public void testConvertNestedPojoToMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        NestedPojo obj = new NestedPojo();
        obj.setTest("foo");
        SimplePojo nested = new SimplePojo();
        nested.setTest("bar");
        obj.setNested(nested);
        DBObject dbObject = conv.convertPojoToMongo(obj);
        assertEquals(2, dbObject.keySet().size());
        assertEquals("foo", dbObject.get("test"));
        DBObject nestedDbObj = (DBObject) dbObject.get("nested");
        assertEquals(1, nestedDbObj.keySet().size());
        assertEquals("bar", nestedDbObj.get("test"));
    }

    @Test
    public void testConvertNestedPojoFromMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject nested = new BasicDBObject();
        nested.put("test", "bar");
        DBObject dbObj = new BasicDBObject();
        dbObj.put("test", "foo");
        dbObj.put("nested", nested);
        NestedPojo obj = conv.convertMongoToPojo(dbObj, NestedPojo.class);
        assertEquals("foo", obj.getTest());
        assertNull(obj.getIgnored());
        assertNotNull(obj.getNested());
        assertEquals("bar", obj.getNested().getTest());
        assertEquals(null, obj.getNested().getIgnored());
    }

    @Test
    public void testConvertIndexedPojoToMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        IndexedPojo obj = new IndexedPojo();
        obj.setTest("test");
        SimplePojo obj1 = new SimplePojo();
        obj1.setTest("test1");
        SimplePojo obj2 = new SimplePojo();
        obj2.setTest("test2");
        SimplePojo obj3 = new SimplePojo();
        obj3.setTest("test3");
        obj.setIndexed(new SimplePojo[] { obj1, obj2, obj3 });

        DBObject dbObject = conv.convertPojoToMongo(obj);
        assertEquals(2, dbObject.keySet().size());
        assertEquals("test", dbObject.get("test"));
        List<?> indexedDbObj = (List<?>) dbObject.get("indexed");
        assertEquals(3, indexedDbObj.size());

        DBObject dbObj1 = (DBObject) indexedDbObj.get(0);
        assertEquals(1, dbObj1.keySet().size());
        assertEquals("test1", dbObj1.get("test"));

        DBObject dbObj2 = (DBObject) indexedDbObj.get(1);
        assertEquals(1, dbObj2.keySet().size());
        assertEquals("test2", dbObj2.get("test"));

        DBObject dbObj3 = (DBObject) indexedDbObj.get(2);
        assertEquals(1, dbObj3.keySet().size());
        assertEquals("test3", dbObj3.get("test"));
    }

    @Test
    public void testConvertPrimitiveIndexedPojoToMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        PrimitiveIndexedPojo obj = new PrimitiveIndexedPojo();
        obj.setTest("test");
        obj.setIndexed(new int[] { 1, 2, 3 });

        DBObject dbObject = conv.convertPojoToMongo(obj);
        assertEquals(2, dbObject.keySet().size());
        assertEquals("test", dbObject.get("test"));
        List<?> indexedDbObj = (List<?>) dbObject.get("indexed");
        assertEquals(3, indexedDbObj.size());

        assertEquals(1, indexedDbObj.get(0));
        assertEquals(2, indexedDbObj.get(1));
        assertEquals(3, indexedDbObj.get(2));

    }

    @Test
    public void testConvertIndexedPojoFromMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject indexed = new BasicDBObject();
        indexed.put("test", "test");
        DBObject dbObj1 = new BasicDBObject();
        dbObj1.put("test", "test1");
        DBObject dbObj2 = new BasicDBObject();
        dbObj2.put("test", "test2");
        DBObject dbObj3 = new BasicDBObject();
        dbObj3.put("test", "test3");
        indexed.put("indexed", Arrays.asList(dbObj1, dbObj2, dbObj3));

        IndexedPojo obj = conv.convertMongoToPojo(indexed, IndexedPojo.class);
        assertEquals("test", obj.getTest());
        assertNull(obj.getIgnored());
        assertNotNull(obj.getIndexed());
        SimplePojo[] indexedObj = obj.getIndexed();
        assertEquals("test1", indexedObj[0].getTest());
        assertNull(indexedObj[0].getIgnored());
        assertEquals("test2", indexedObj[1].getTest());
        assertNull(indexedObj[1].getIgnored());
        assertEquals("test3", indexedObj[2].getTest());
        assertNull(indexedObj[2].getIgnored());
    }

    @Test
    public void testConvertPrimitiveIndexedPojoFromMongo() {
        MongoPojoConverter conv = new MongoPojoConverter();
        DBObject indexed = new BasicDBObject();
        indexed.put("test", "test");
        indexed.put("indexed", Arrays.asList(1, 2, 3));

        PrimitiveIndexedPojo obj = conv.convertMongoToPojo(indexed, PrimitiveIndexedPojo.class);
        assertEquals("test", obj.getTest());
        assertNull(obj.getIgnored());
        assertNotNull(obj.getIndexed());
        int[] indexedObj = obj.getIndexed();
        assertEquals(1, indexedObj[0]);
        assertEquals(2, indexedObj[1]);
        assertEquals(3, indexedObj[2]);
    }
}

