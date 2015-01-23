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

import com.redhat.thermostat.storage.model.TimeStampedPojo;

public class VmLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final String VM_ID = "vmId";
    private static final int VM_PID = 123;
    private static final String MAIN_CLASS = "Foo.class";
    private static final String CATEGORY_NAME = "vmcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category<TestPojo> cat =  new Category<>(CATEGORY_NAME, TestPojo.class);

    private static long t1 = 1;
    private static long t2 = 5;
    private static long t3 = 10;

    private static long lc1 = 10;
    private static long lc2 = 20;
    private static long lc3 = 30;

    private HostRef hostRef;
    private VmRef vmRef;
    private TestPojo result1, result2, result3;

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
        result3 = mock(TestPojo.class);
        when(result3.getTimeStamp()).thenReturn(t3);
        when(result3.getData()).thenReturn(lc3);
    }
    
    @Test
    public void verifyQueryDescriptorFormat() {
        String expected = "QUERY %s WHERE 'agentId' = ?s AND " +
         "'vmId' = ?s AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expected, VmLatestPojoListGetter.VM_LATEST_QUERY_FORMAT);
    }
    
    @Test
    public void verifyQueryDescriptorIsSane() {
        Storage storage = mock(Storage.class);
        VmLatestPojoListGetter<TestPojo> getter = new VmLatestPojoListGetter<>(storage, cat);
        String actualDesc = getter.getQueryLatestDesc();
        String expected = "QUERY vmcategory WHERE 'agentId' = ?s AND " +
         "'vmId' = ?s AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expected, actualDesc);
    }

    @Test
    public void testBuildQuery() throws DescriptorParsingException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);

        VmLatestPojoListGetter<TestPojo> getter = new VmLatestPojoListGetter<>(storage, cat);
        query = getter.buildQuery(vmRef, 123l);

        assertNotNull(query);
        verify(storage).prepareStatement(anyDescriptor());
        verify(query).setString(0, AGENT_ID);
        verify(query).setString(1, VM_ID);
        verify(query).setLong(2, 123l);
        verifyNoMoreInteractions(query);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<TestPojo> anyDescriptor() {
        return (StatementDescriptor<TestPojo>) any(StatementDescriptor.class);
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() throws DescriptorParsingException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> ignored = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(ignored).thenReturn(query);

        VmLatestPojoListGetter<TestPojo> getter = new VmLatestPojoListGetter<>(storage, cat);
        getter.buildQuery(vmRef, Long.MIN_VALUE); // Ignore first return value.
        query = getter.buildQuery(vmRef, Long.MIN_VALUE);

        assertNotNull(query);
        verify(storage, times(2)).prepareStatement(anyDescriptor());
        verify(query).setString(0, AGENT_ID);
        verify(query).setString(1, VM_ID);
        verify(query).setLong(2, Long.MIN_VALUE);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetLatest() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<TestPojo> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);
        when(query.executeQuery()).thenReturn(cursor);

        VmLatestPojoListGetter<TestPojo> getter = new VmLatestPojoListGetter<>(storage, cat);

        List<TestPojo> stats = getter.getLatest(vmRef, t2);

        verify(storage).prepareStatement(anyDescriptor());
        verify(query).setString(0, AGENT_ID);
        verify(query).setString(1, VM_ID);
        verify(query).setLong(2, t2);
        verify(query).executeQuery();
        verifyNoMoreInteractions(query);

        assertNotNull(stats);
        assertEquals(2, stats.size());
        TestPojo stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertEquals(lc1, stat1.getData());
        TestPojo stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertEquals(lc2, stat2.getData());
    }
    
    private static interface TestPojo extends TimeStampedPojo {
        
        long getData();
        
    }

}

