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

package com.redhat.thermostat.common.dao;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.model.VmClassStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final int VM_PID = 123;
    private static final String MAIN_CLASS = "Foo.class";
    private static final String CATEGORY_NAME = "vmcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category cat =  new Category(CATEGORY_NAME);

    private static long t1 = 1;
    private static long t2 = 5;
    private static long t3 = 10;

    private static long lc1 = 10;
    private static long lc2 = 20;
    private static long lc3 = 30;

    private HostRef hostRef;
    private VmRef vmRef;
    private Converter<VmClassStat> converter;
    private Chunk result1, result2, result3;

    @Before
    public void setUp() {
        hostRef = new HostRef(AGENT_ID, HOSTNAME);
        vmRef = new VmRef(hostRef, VM_PID, MAIN_CLASS);
        converter = new VmClassStatConverter();
        result1 = new Chunk(cat, false);
        result1.put(Key.AGENT_ID, AGENT_ID);
        result1.put(Key.VM_ID, VM_PID);
        result1.put(Key.TIMESTAMP, t1);
        result1.put(VmClassStatDAO.loadedClassesKey, lc1);
        result2 = new Chunk(cat, false);
        result2.put(Key.AGENT_ID, AGENT_ID);
        result2.put(Key.VM_ID, VM_PID);
        result2.put(Key.TIMESTAMP, t2);
        result2.put(VmClassStatDAO.loadedClassesKey, lc2);
        result3 = new Chunk(cat, false);
        result3.put(Key.AGENT_ID, AGENT_ID);
        result3.put(Key.VM_ID, VM_PID);
        result3.put(Key.TIMESTAMP, t3);
        result3.put(VmClassStatDAO.loadedClassesKey, lc3);
    }

    @Test
    public void testBuildQuery() {
        Storage storage = mock(Storage.class);
        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat, converter, vmRef);
        Chunk query = getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(2, query.getKeys().size());
        assertTrue(query.getKeys().contains(Key.AGENT_ID));
        assertTrue(query.getKeys().contains(Key.VM_ID));
        assertFalse(query.getKeys().contains(Key.WHERE));
        assertEquals(AGENT_ID, query.get(Key.AGENT_ID));
        assertEquals((Integer) VM_PID, query.get(Key.VM_ID));
    }

    @Test
    public void testBuildQueryWithSince() {
        Storage storage = mock(Storage.class);
        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat, converter, vmRef, 123);
        Chunk query = getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(3, query.getKeys().size());
        assertTrue(query.getKeys().contains(Key.AGENT_ID));
        assertTrue(query.getKeys().contains(Key.VM_ID));
        assertEquals("this.timestamp > 123" , query.get(Key.WHERE));
        assertEquals(AGENT_ID, query.get(Key.AGENT_ID));
        assertEquals((Integer) VM_PID, query.get(Key.VM_ID));
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() {
        Storage storage = mock(Storage.class);
        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat, converter, vmRef);
        getter.buildQuery(); // Ignore first return value.
        Chunk query = getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(3, query.getKeys().size());
        assertTrue(query.getKeys().contains(Key.AGENT_ID));
        assertTrue(query.getKeys().contains(Key.VM_ID));
        assertTrue(query.getKeys().contains(Key.WHERE));
        assertEquals("this.timestamp > " + Long.MIN_VALUE, query.get(Key.WHERE));
    }

    @Test
    public void testGetLatest() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat, converter, vmRef);

        List<VmClassStat> stats = getter.getLatest();

        assertNotNull(stats);
        assertEquals(2, stats.size());
        VmClassStat stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertEquals(lc1, stat1.getLoadedClasses());
        VmClassStat stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertEquals(lc2, stat2.getLoadedClasses());
    }

    @Test
    public void testGetLatestMultipleCalls() {
        Cursor cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Cursor cursor2 = mock(Cursor.class);
        when(cursor2.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor2.next()).thenReturn(result3);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor1);

        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat, converter, vmRef);
        getter.getLatest();
        getter.getLatest();

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage, times(2)).findAll(arg.capture());
        List<Chunk> queries = arg.getAllValues();

        assertEquals(2, queries.size());
        Chunk query = queries.get(1);
        assertNotNull(query);
        assertEquals(AGENT_ID, query.get(Key.AGENT_ID));
        assertEquals((Integer) VM_PID, query.get(Key.VM_ID));
        assertEquals("this.timestamp > " + t2, query.get(Key.WHERE));
    }
}
