/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ChunkConverterTest {

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);

    private static final Key<String> key1_key1 = new Key<>("key1.key1", false);
    private static final Key<String> key1_key2 = new Key<>("key1.key2", false);
    private static final Key<String> key1_key2_key1 = new Key<>("key1.key2.key1", false);
    private static final Key<String> key1_key2_key2 = new Key<>("key1.key2.key2", false);
    private static final Key<String> key1_key2_key3 = new Key<>("key1.key2.key3", false);
    private static final Key<String> key2_key1 = new Key<>("key2.key1", false);
    private static final Key<String> key2_key2 = new Key<>("key2.key2", false);
    private static final Key<String> key2_key3 = new Key<>("key2.key3", false);

    private static final Key<List<Integer>> listKey = new Key<>("list", false);

    private static final String mongoId = "_id";
    private static final Key<String> invalidMongoIdKey = new Key<>(mongoId, false);

    private static final Category testCategory = new Category("ChunkConverterTest", key1, key2, key3, key4, key5,
                                                             key1_key1, key1_key2, key2_key1, key2_key2, key2_key3,
                                                             key1_key2_key1, key1_key2_key2, key1_key2_key3);
    private static final Category smallerCategory = new Category("SmallerTest", key1, key2);

    private static final Category listCategory = new Category("something-with-lists", listKey);

    @Test
    public void verifyBasicChunkToDBObject() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, "test1");

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObject = converter.chunkToDBObject(chunk);

        assertEquals(1, dbObject.keySet().size());
        assertTrue(dbObject.keySet().contains("key1"));
        assertEquals("test1", dbObject.get("key1"));
    }

    @Test
    public void verifyChunkToDBObjectInOrder() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key5, "test1");
        chunk.put(key4, "test2");
        chunk.put(key3, "test3");
        chunk.put(key2, "test4");
        chunk.put(key1, "test5");

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObject = converter.chunkToDBObject(chunk);

        assertEquals(5, dbObject.keySet().size());
        assertArrayEquals(new String[]{"key5", "key4", "key3", "key2", "key1"}, dbObject.keySet().toArray());
    }

    @Test
    public void verifyChunkToDBObjectWithLists() {
        Chunk chunk = new Chunk(listCategory, false);
        chunk.put(listKey, Arrays.asList(1, 2, 3, 4));

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObj = converter.chunkToDBObject(chunk);

        assertEquals(Arrays.asList(1,2,3,4), dbObj.get("list"));

    }

    @Test
    public void verifyBasicDBObjectToChunk() {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("key1", "test1");

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObject, testCategory);

        assertSame(testCategory, chunk.getCategory());
        assertEquals(1, chunk.getKeys().size());
        assertTrue(chunk.getKeys().contains(key1));
        assertEquals("test1", chunk.get(key1));

    }

    @Test
    public void verifyDBObjectToChunkInOrder() {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("key5", "test1");
        dbObject.put("key4", "test2");
        dbObject.put("key3", "test3");
        dbObject.put("key2", "test4");
        dbObject.put("key1", "test5");

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObject, testCategory);

        assertSame(testCategory, chunk.getCategory());
        assertEquals(5, chunk.getKeys().size());
        assertArrayEquals(new Key<?>[]{ key5, key4, key3, key2, key1 }, chunk.getKeys().toArray());
        assertEquals("test5", chunk.get(key1));
        assertEquals("test4", chunk.get(key2));
        assertEquals("test3", chunk.get(key3));
        assertEquals("test2", chunk.get(key4));
        assertEquals("test1", chunk.get(key5));

    }

    @Test
    public void verifyDBObjectToChunkWithLists() {
        BasicDBObject dbObj = new BasicDBObject();
        dbObj.put("list", Arrays.asList(1, 2, 3, 4));

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObj, listCategory);

        List<Integer> data = chunk.get(listKey);
        assertEquals(Arrays.asList(1, 2, 3, 4), data);
    }

    @Test
    public void verifySimpleNestedChunkToObject() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1_key1, "test1");

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObject = converter.chunkToDBObject(chunk);

        assertEquals(1, dbObject.keySet().size());
        assertTrue(dbObject.keySet().contains("key1"));
        DBObject nested = (DBObject) dbObject.get("key1");
        assertEquals(1, nested.keySet().size());
        assertTrue(nested.keySet().contains("key1"));
        assertEquals("test1", nested.get("key1"));
    }

    @Test
    public void verifyComplexNestedChunkToObject() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1_key1, "test1");
        chunk.put(key1_key2, "test2");
        chunk.put(key2_key1, "test3");
        chunk.put(key2_key2, "test4");
        chunk.put(key2_key3, "test5");
        chunk.put(key3, "test6");

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObject = converter.chunkToDBObject(chunk);

        assertEquals(3, dbObject.keySet().size());
        assertTrue(dbObject.keySet().contains("key1"));
        assertTrue(dbObject.keySet().contains("key2"));
        assertTrue(dbObject.keySet().contains("key3"));
        assertEquals("test6", dbObject.get("key3"));

        DBObject nested1 = (DBObject) dbObject.get("key1");
        assertEquals(2, nested1.keySet().size());
        assertTrue(nested1.keySet().contains("key1"));
        assertTrue(nested1.keySet().contains("key2"));
        assertEquals("test1", nested1.get("key1"));
        assertEquals("test2", nested1.get("key2"));

        DBObject nested2 = (DBObject) dbObject.get("key2");
        assertEquals(3, nested2.keySet().size());
        assertTrue(nested2.keySet().contains("key1"));
        assertTrue(nested2.keySet().contains("key2"));
        assertTrue(nested2.keySet().contains("key3"));
        assertEquals("test3", nested2.get("key1"));
        assertEquals("test4", nested2.get("key2"));
        assertEquals("test5", nested2.get("key3"));
    }

    @Test
    public void verifyComplex3LevelChunkToObject() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1_key1, "test1");
        chunk.put(key1_key2_key1, "test3");
        chunk.put(key1_key2_key2, "test4");
        chunk.put(key1_key2_key3, "test5");
        chunk.put(key3, "test6");

        ChunkConverter converter = new ChunkConverter();
        DBObject dbObject = converter.chunkToDBObject(chunk);

        assertEquals(2, dbObject.keySet().size());
        assertTrue(dbObject.keySet().contains("key1"));
        assertTrue(dbObject.keySet().contains("key3"));
        assertEquals("test6", dbObject.get("key3"));

        DBObject nested1 = (DBObject) dbObject.get("key1");
        assertEquals(2, nested1.keySet().size());
        assertTrue(nested1.keySet().contains("key1"));
        assertTrue(nested1.keySet().contains("key2"));
        assertEquals("test1", nested1.get("key1"));

        DBObject nested2 = (DBObject) nested1.get("key2");
        assertEquals(3, nested2.keySet().size());
        assertTrue(nested2.keySet().contains("key1"));
        assertTrue(nested2.keySet().contains("key2"));
        assertTrue(nested2.keySet().contains("key3"));
        assertEquals("test3", nested2.get("key1"));
        assertEquals("test4", nested2.get("key2"));
        assertEquals("test5", nested2.get("key3"));
    }

    @Test
    public void verifySimpleNestedObjectToChunk() {
        BasicDBObject nested = new BasicDBObject();
        nested.put("key1", "test1");
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("key1", nested);

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObject, testCategory);

        assertSame(testCategory, chunk.getCategory());
        assertEquals(1, chunk.getKeys().size());
        assertTrue(chunk.getKeys().contains(key1_key1));
        assertEquals("test1", chunk.get(key1_key1));
    }

    @Test
    public void verifyComplexNestedObjectToChunk() {
        BasicDBObject nested1 = new BasicDBObject();
        nested1.put("key1", "test1");
        nested1.put("key2", "test2");

        BasicDBObject nested2 = new BasicDBObject();
        nested2.put("key1", "test3");
        nested2.put("key2", "test4");
        nested2.put("key3", "test5");

        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("key1", nested1);
        dbObject.put("key2", nested2);
        dbObject.put("key3", "test6");

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObject, testCategory);

        assertSame(testCategory, chunk.getCategory());

        assertEquals(6, chunk.getKeys().size());
        assertTrue(chunk.getKeys().contains(key1_key1));
        assertTrue(chunk.getKeys().contains(key1_key2));
        assertTrue(chunk.getKeys().contains(key2_key1));
        assertTrue(chunk.getKeys().contains(key2_key2));
        assertTrue(chunk.getKeys().contains(key2_key3));
        assertTrue(chunk.getKeys().contains(key3));
        assertEquals("test1", chunk.get(key1_key1));
        assertEquals("test2", chunk.get(key1_key2));
        assertEquals("test3", chunk.get(key2_key1));
        assertEquals("test4", chunk.get(key2_key2));
        assertEquals("test5", chunk.get(key2_key3));
        assertEquals("test6", chunk.get(key3));
    }

    @Test
    public void verifyComplex3LevelObjectToChunk() {

        BasicDBObject nested2 = new BasicDBObject();
        nested2.put("key1", "test3");
        nested2.put("key2", "test4");
        nested2.put("key3", "test5");

        BasicDBObject nested1 = new BasicDBObject();
        nested1.put("key1", "test1");
        nested1.put("key2", nested2);

        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("key1", nested1);
        dbObject.put("key3", "test6");

        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(dbObject, testCategory);

        assertSame(testCategory, chunk.getCategory());

        assertEquals(5, chunk.getKeys().size());
        assertTrue(chunk.getKeys().contains(key1_key1));
        assertTrue(chunk.getKeys().contains(key1_key2_key1));
        assertTrue(chunk.getKeys().contains(key1_key2_key2));
        assertTrue(chunk.getKeys().contains(key1_key2_key3));
        assertTrue(chunk.getKeys().contains(key3));

        assertEquals("test1", chunk.get(key1_key1));
        assertEquals("test3", chunk.get(key1_key2_key1));
        assertEquals("test4", chunk.get(key1_key2_key2));
        assertEquals("test5", chunk.get(key1_key2_key3));
        assertEquals("test6", chunk.get(key3));
    }

    @Test
    public void verifyDBObjectToChunkIgnoresMongoID() {
        DBObject obj = new BasicDBObject(mongoId, "mongo_private_info");
        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(obj, new Category("invalidCategory", invalidMongoIdKey));

        assertEquals(1, chunk.getKeys().size());
        assertEquals("mongo_private_info", chunk.get(Key.ID));
    }

    @Test
    public void verifyDBObjectToChunkAvoidsNonExistentKeys() {
        DBObject obj = new BasicDBObject("key1", "data1");
        obj.put("key2", "data2");
        obj.put("key3", "data3"); // This one is not a part of smallerCategory
        ChunkConverter converter = new ChunkConverter();
        Chunk chunk = converter.dbObjectToChunk(obj, smallerCategory);

        assertEquals(2, chunk.getKeys().size());
        assertFalse(chunk.getKeys().contains(key3));
        assertTrue(chunk.getKeys().contains(key1));
        assertTrue(chunk.getKeys().contains(key2));
        assertEquals("data1", chunk.get(key1));
        assertEquals("data2", chunk.get(key2));
    }
}
