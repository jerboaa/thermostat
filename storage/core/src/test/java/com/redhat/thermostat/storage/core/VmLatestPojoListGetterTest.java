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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.VmClassStat;

public class VmLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final int VM_PID = 123;
    private static final String MAIN_CLASS = "Foo.class";
    private static final String CATEGORY_NAME = "vmcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category<VmClassStat> cat =  new Category<>(CATEGORY_NAME, VmClassStat.class);

    private static long t1 = 1;
    private static long t2 = 5;
    private static long t3 = 10;

    private static long lc1 = 10;
    private static long lc2 = 20;
    private static long lc3 = 30;

    private HostRef hostRef;
    private VmRef vmRef;
    private VmClassStat result1, result2, result3;

    @Before
    public void setUp() {
        hostRef = new HostRef(AGENT_ID, HOSTNAME);
        vmRef = new VmRef(hostRef, VM_PID, MAIN_CLASS);
        result1 = new VmClassStat(VM_PID, t1, lc1);
        result2 = new VmClassStat(VM_PID, t2, lc2);
        result3 = new VmClassStat(VM_PID, t3, lc3);
    }

    @Test
    public void testBuildQuery() {
        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);

        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat);
        query = getter.buildQuery(vmRef, 123l);

        assertNotNull(query);
        verify(storage).createQuery(cat);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).where(Key.VM_ID, Criteria.EQUALS, VM_PID);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, 123l);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() {
        Storage storage = mock(Storage.class);
        Query ignored = mock(Query.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class))).thenReturn(ignored).thenReturn(query);

        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat);
        getter.buildQuery(vmRef, Long.MIN_VALUE); // Ignore first return value.
        query = getter.buildQuery(vmRef, Long.MIN_VALUE);

        assertNotNull(query);
        verify(storage, times(2)).createQuery(cat);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).where(Key.VM_ID, Criteria.EQUALS, VM_PID);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetLatest() {
        @SuppressWarnings("unchecked")
        Cursor<VmClassStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);
        when(query.execute()).thenReturn(cursor);

        VmLatestPojoListGetter<VmClassStat> getter = new VmLatestPojoListGetter<>(storage, cat);

        List<VmClassStat> stats = getter.getLatest(vmRef, t2);

        verify(storage).createQuery(cat);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).where(Key.VM_ID, Criteria.EQUALS, VM_PID);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, t2);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertNotNull(stats);
        assertEquals(2, stats.size());
        VmClassStat stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertEquals(lc1, stat1.getLoadedClasses());
        VmClassStat stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertEquals(lc2, stat2.getLoadedClasses());
    }

}

