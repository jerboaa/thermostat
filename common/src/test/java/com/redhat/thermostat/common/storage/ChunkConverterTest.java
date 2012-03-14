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

import static org.junit.Assert.*;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ChunkConverterTest {

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);

    private static final Category testCategory = new Category("ChunkConverterTest", key1, key2, key3, key4, key5);

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
    
}
