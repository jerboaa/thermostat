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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ChunkAdapterTest {

    @Entity
    public static class SomeData {

        private long[] arrayData;
        private List<String> listData;
        private long longData;
        private String stringData;
        private NestedData nestedData;

        @Persist
        public long[] getArrayData() {
            return arrayData;
        }

        @Persist
        public void setArrayData(long[] arrayData) {
            this.arrayData = arrayData;
        }

        public void setListData(List<String> data) {
            this.listData = data;
        }

        public List<String> getListData() {
            return listData;
        }

        public long getLongData() {
            return longData;
        }

        public void setLongData(long newValue) {
            longData = newValue;
        }

        public void setStringData(String newStringData) {
            stringData = newStringData;
        }

        public String getStringData() {
            return stringData;
        }

        public NestedData getNestedData() {
            return nestedData;
        }

        public void setNestedData(NestedData nestedData) {
            this.nestedData = nestedData;
        }
    }

    public static class NestedData {
        private String data;
        public void setData(String data) {
            this.data = data;
        }
        public String getData() {
            return data;
        }
    }

    // the expected keys for each 'property' in the SomeData bean
    private static final Key<long[]> arrayData = new Key<>("arrayData", false);
    private static final Key<List<String>> listData = new Key<>("listData", false);
    private static final Key<Long> longData = new Key<>("longData", false);
    private static final Key<String> stringData = new Key<>("stringData", false);
    private static final Key<String> nestedData = new Key<>("nestedData.data", false);

    @Test
    public void verifyAdapaterCanBeUsedInPlaceOfChunk() {
        SomeData testObject = new SomeData();
        ChunkAdapter adapter = new ChunkAdapter(testObject);

        assertTrue(adapter instanceof Chunk);
    }

    @Test
    public void verifyCategoryName() {
        SomeData testObject = new SomeData();
        ChunkAdapter adapter = new ChunkAdapter(testObject);
        Category category = adapter.getCategory();
        assertNotNull(category);
        assertEquals("SomeData", category.getName());
    }

    @Test
    public void verifyKeys() throws InterruptedException {
        SomeData testObject = new SomeData();
        Chunk chunk = new ChunkAdapter(testObject);
        testObject.setArrayData(new long[] { 0xADD });

        Set<Key<?>> recognizedKeys = chunk.getKeys();
        // only one method is annotated with @Persist
        assertEquals(1, recognizedKeys.size());
        // verify that single key can be read
        Key<?> onlyRecognizedKey = recognizedKeys.iterator().next();
        assertArrayEquals(new long[] { 0xADD }, (long[]) chunk.get(onlyRecognizedKey));
    }

    @Test
    public void verifyGetAndPutLongValue() {
        SomeData testObject = new SomeData();
        Chunk chunk = new ChunkAdapter(testObject);
        testObject.setLongData(1l);

        assertEquals((Long) 1l, chunk.get(longData));

        chunk.put(longData, 2l);

        assertEquals((Long) 2l, chunk.get(longData));
        assertEquals(2, testObject.longData);
    }

    @Test
    public void verifyGetAndPutStringValue() {
        SomeData testObject = new SomeData();
        Chunk chunk = new ChunkAdapter(testObject);
        testObject.setStringData("stringData");

        assertEquals("stringData", chunk.get(stringData));

        chunk.put(stringData, "some-new-data");

        assertEquals("some-new-data", chunk.get(stringData));
        assertEquals("some-new-data", testObject.stringData);
    }

    @Test
    public void verifyGetAndPutArrayValue() {
        SomeData testObject = new SomeData();
        Chunk chunk = new ChunkAdapter(testObject);
        testObject.setArrayData(new long[] { 0xADD } );

        assertArrayEquals(new long[] { 0xADD }, chunk.get(arrayData));

        chunk.put(arrayData, new long[] { 0xC0FFEE });

        assertArrayEquals(new long[] { 0xC0FFEE }, chunk.get(arrayData));
        assertArrayEquals(new long[] { 0xC0FFEE }, testObject.arrayData);
    }

    @Test
    public void verifyGetAndPutListValue() {
        SomeData testObject = new SomeData();
        Chunk chunk = new ChunkAdapter(testObject);
        List<String> newList = new ArrayList<>();
        testObject.setListData(newList);

        assertEquals(newList, chunk.get(listData));

        chunk.put(listData, Collections.<String>emptyList());

        assertSame(Collections.<String>emptyList(), chunk.get(listData));
        assertSame(Collections.<String>emptyList(), testObject.listData);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyNonEntityIsNotAcceptable() {
        // missing @Entity annotation
        class NonEntity {
            public void setInteger(int integer) { /* no op */ }
            public int getInteger() { return 42; }
        }

        NonEntity toAdapt = new NonEntity();
        new ChunkAdapter(toAdapt);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyMissingPersistAnnotationOnOneMethodThrowsException() {
        @Entity
        class DataWithMissingAnnotationOnOneMethod {
            @Persist public void setInteger(int integer) { /* no op */ }
            public int getInteger() { return 42; }
        }

        DataWithMissingAnnotationOnOneMethod toAdapt = new DataWithMissingAnnotationOnOneMethod();
        new ChunkAdapter(toAdapt);
    }

    @Test
    public void verifyCustomEntityNameIsCategoryName() {
        final String ENTITY_NAME = "custom-data-name";

        @Entity (name=ENTITY_NAME)
        class DataWithCustomName {
            public void setData(String data) { /* no-op */ }
            public String getData() { return "ignore this value" ; }
        }

        Chunk chunk = new ChunkAdapter(new DataWithCustomName());

        assertEquals(ENTITY_NAME, chunk.getCategory().getName());
    }

    @Test
    public void verifyCustomAttributeNameOnGetIsKeyName() {
        final String ATTRIBUTE_NAME = "custom-attribute";
        Key<?> expectedKey = new Key<>(ATTRIBUTE_NAME, false);

        @Entity
        class DataWithCustomAttributeNameOnGet {
            @Persist (name=ATTRIBUTE_NAME)
            public String getCustomAttribute() { return "ignore this value" ; }
            @Persist
            public void setCustomAttribute(String attribute) { /* no op */ }
        }

        Chunk chunk = new ChunkAdapter(new DataWithCustomAttributeNameOnGet());

        assertTrue(chunk.getKeys().contains(expectedKey));

        assertTrue(chunk.getCategory().getKeys().contains(expectedKey));
        assertNotNull(chunk.getCategory().getKey(ATTRIBUTE_NAME));
    }

    @Test
    public void verifyCustomAttributeNameOnSetIsKeyName() {
        final String ATTRIBUTE_NAME = "custom-attribute";
        Key<?> expectedKey = new Key<>(ATTRIBUTE_NAME, false);

        @Entity
        class DataWithCustomAttributeNameOnSet {
            @Persist
            public String getCustomAttribute() { return "ignore this value" ; }
            @Persist (name=ATTRIBUTE_NAME)
            public void setCustomAttribute(String attribute) { /* no op */ }
        }

        Chunk chunk = new ChunkAdapter(new DataWithCustomAttributeNameOnSet());

        assertTrue(chunk.getKeys().contains(expectedKey));

        assertTrue(chunk.getCategory().getKeys().contains(expectedKey));
        assertNotNull(chunk.getCategory().getKey(ATTRIBUTE_NAME));
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyExceptionOnCustomAttributeNameMismatch() {
        @Entity
        class DataWithCustomAttributeNameMismatch {
            @Persist (name="one-name")
            public String getCustomAttribute() { return "ignore this value" ; }
            @Persist (name="different-name")
            public void setCustomAttribute(String attribute) { /* no op */ }
        }

        new ChunkAdapter(new DataWithCustomAttributeNameMismatch());
    }

    @Test
    public void verifyGetAndPutNestedValue() {
        NestedData nested = new NestedData();
        nested.setData("stringData");
        SomeData testObject = new SomeData();
        testObject.setNestedData(nested);
        Chunk chunk = new ChunkAdapter(testObject);

        assertEquals("stringData", chunk.get(nestedData));

        chunk.put(nestedData, "some-new-data");

        assertEquals("some-new-data", chunk.get(nestedData));
        assertEquals("some-new-data", testObject.getNestedData().getData());
    }
}
