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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.model.TimeStampedPojo;

public class VmBoundaryPojoGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final String VM_ID = "vmId";
    private static final int VM_PID = 123;
    private static final String MAIN_CLASS = "Foo.class";
    private static final String CATEGORY_NAME = "vm-boundary-category";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category<TestPojo> cat =  new Category<>(CATEGORY_NAME, TestPojo.class);

    private static long t1 = 1;
    private static long t2 = 5;

    private static long lc1 = 10;
    private static long lc2 = 20;

    private HostRef hostRef;
    private VmRef vmRef;
    private TestPojo result1, result2;

    @Before
    public void setUp() {
        hostRef = new HostRef(AGENT_ID, HOSTNAME);
        vmRef = new VmRef(hostRef, VM_ID, VM_PID, MAIN_CLASS);
        result1 = mock(TestPojo.class);
        when(result1.getTimeStamp()).thenReturn(t1);
        when(result1.getData()).thenReturn(lc1);
        result2 = mock(TestPojo.class);
        when(result2.getTimeStamp()).thenReturn(t2);
        when(result2.getData()).thenReturn(lc2);
    }

    @Test
    public void verifyLatestQueryDescriptorFormat() {
        String latestExpected = "QUERY %s WHERE 'agentId' = ?s AND " +
                "'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(latestExpected, VmBoundaryPojoGetter.DESC_LATEST_VM_STAT);
    }

    @Test
    public void verifyOldestQueryDescriptorFormat() {
        String oldestExpected = "QUERY %s WHERE 'agentId' = ?s AND " +
                "'vmId' = ?s SORT 'timeStamp' ASC LIMIT 1";
        assertEquals(oldestExpected, VmBoundaryPojoGetter.DESC_OLDEST_VM_STAT);
    }

    @Test
    public void verifyLatestQueryDescriptorIsSane() {
        Storage storage = mock(Storage.class);
        VmBoundaryPojoGetter<TestPojo> getter = new VmBoundaryPojoGetter<>(storage, cat);

        String actualLatestDesc = getter.getLatestQueryDesc();
        String latestExpected = "QUERY vm-boundary-category WHERE 'agentId' = ?s AND " +
                "'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(latestExpected, actualLatestDesc);
    }

    @Test
    public void verifyOldestQueryDescriptorIsSane() {
        Storage storage = mock(Storage.class);
        VmBoundaryPojoGetter<TestPojo> getter = new VmBoundaryPojoGetter<>(storage, cat);

        String actualOldestDesc = getter.getOldestQueryDesc();
        String oldestExpected = "QUERY vm-boundary-category WHERE 'agentId' = ?s AND " +
                "'vmId' = ?s SORT 'timeStamp' ASC LIMIT 1";

        assertEquals(oldestExpected, actualOldestDesc);
    }

    @Test
         public void testGetOldest() throws DescriptorParsingException, StatementExecutionException {
        Cursor<TestPojo> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(null);

        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(query.executeQuery()).thenReturn(cursor);

        Storage storage = mock(Storage.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);

        VmBoundaryPojoGetter<TestPojo> getter = new VmBoundaryPojoGetter<>(storage, cat);

        TestPojo oldest = getter.getOldestStat(vmRef);

        assertEquals(t1, oldest.getTimeStamp());
        assertEquals(lc1, oldest.getData());
    }

    @Test
    public void testGetLatest() throws DescriptorParsingException, StatementExecutionException {
        Cursor<TestPojo> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result2).thenReturn(null);

        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(query.executeQuery()).thenReturn(cursor);

        Storage storage = mock(Storage.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);

        VmBoundaryPojoGetter<TestPojo> getter = new VmBoundaryPojoGetter<>(storage, cat);

        TestPojo oldest = getter.getLatestStat(vmRef);

        assertEquals(t2, oldest.getTimeStamp());
        assertEquals(lc2, oldest.getData());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<TestPojo> anyDescriptor() {
        return (StatementDescriptor<TestPojo>) any(StatementDescriptor.class);
    }

    private static interface TestPojo extends TimeStampedPojo {
        long getData();
    }
}
