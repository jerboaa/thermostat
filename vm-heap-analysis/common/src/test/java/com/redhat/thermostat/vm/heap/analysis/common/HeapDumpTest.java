/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.store.Directory;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Snapshot;

/*
 * This testcase uses a minimalistic heapdump that is stored as binary in
 * src/test/resources.
 *
 * It is generated from a simple Java class:
 * public class Test {
 *   public static void main(String[] args) throws Exception {
 *     Thread.sleep(1000000);
 *   }
 * }
 *
 * The actual heapdump can be generated like this:
 *
 * jmap -dump:format=b,file=heapdump.hprof $PID
 * 
 * where $PID is the PID of the Java process running the above simple program.
 *
 * Finally, the heapdump is gzipped:
 *
 * gzip heapdump.hprof
 *
 */
public class HeapDumpTest {

    private static final String HEAP_ID = "TEST_HEAP_ID";

    private HeapDump heapDump;

    @Before
    public void setUp() throws IOException {
        InputStream in = getClass().getResourceAsStream("/heapdump.hprof.gz");
        GZIPInputStream gzipIn = new GZIPInputStream(in);

        HeapInfo heapInfo = mock(HeapInfo.class);
        when(heapInfo.getHeapId()).thenReturn(HEAP_ID);
        HeapDAO heapDAO = mock(HeapDAO.class);
        when(heapDAO.getHeapDumpData(heapInfo)).thenReturn(gzipIn);
        heapDump = new HeapDump(heapInfo, heapDAO);
    }

    @Test
    public void testSearchObjects() {
        Collection<String> foundObjectIds = heapDump.searchObjects("java.util.ArrayDeque", 10);
        assertEquals(8, foundObjectIds.size());
        assertTrue(foundObjectIds.contains("0x7d704eb20"));
        assertTrue(foundObjectIds.contains("0x7d70485e8"));
        assertTrue(foundObjectIds.contains("0x7d704aed8"));
        assertTrue(foundObjectIds.contains("0x7d70447a0"));
        assertTrue(foundObjectIds.contains("0x7d704d438"));
        assertTrue(foundObjectIds.contains("0x7d70471e8"));
        assertTrue(foundObjectIds.contains("0x7d7049aa0"));
        assertTrue(foundObjectIds.contains("0x7d704bfe0"));
    }
    
    /*
     * Smoke test for basic lucene compat testing.
     */
    @Test
    public void canCreateLuceneIndex() {
        try {
            Snapshot mockSnapShot = mock(Snapshot.class);
            JavaHeapObject obj1 = mock(JavaHeapObject.class);
            JavaClass clazz1 = mock(JavaClass.class);
            when(obj1.getClazz()).thenReturn(clazz1);
            when(clazz1.getName()).thenReturn("fake-class-one");
            when(obj1.getIdString()).thenReturn("foo");
            JavaClass clazz2 = mock(JavaClass.class);
            when(clazz2.getName()).thenReturn("fake-class-two");
            JavaHeapObject obj2 = mock(JavaHeapObject.class);
            when(obj2.getIdString()).thenReturn("bar");
            when(obj2.getClazz()).thenReturn(clazz2);
            Vector<JavaHeapObject> things = new Vector<>();
            things.add(obj1);
            things.add(obj2);
            when(mockSnapShot.getThings()).thenReturn(things.elements());
            HeapDump bareDump = new HeapDump(null, null, mockSnapShot);
            Directory dir = bareDump.createLuceneIndex();
            assertNotNull(dir);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create lucene index");
        }
    }

    @Test
    public void testSearchObjectsWithLimit() {
        Collection<String> foundObjectIds = heapDump.searchObjects("java.util.ArrayDeque", 2);
        assertEquals(2, foundObjectIds.size());
        // we know 2 things matched. Don't know which ones (there is no guarentee on order).
        String[] possibleMatches = new String[] {
            "0x7d704eb20",
            "0x7d70485e8",
            "0x7d704aed8",
            "0x7d70447a0",
            "0x7d704d438",
            "0x7d70471e8",
            "0x7d7049aa0",
            "0x7d704bfe0",
        };
        int totalMatches = 0;
        for (String possible : possibleMatches) {
            if (foundObjectIds.contains(possible)) {
                totalMatches++;
            }
        }
        assertEquals(2, totalMatches);
    }

    @Test
    public void testFindObject() {
        JavaHeapObject obj = heapDump.findObject("0x7d704eb20");
        assertEquals("0x7d704eb20", obj.getIdString());
        assertEquals("java.util.ArrayDeque", obj.getClazz().getName());
    }
}

