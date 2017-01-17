/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.web.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebQueryResponse;

public class WebCursorTest {

    private WebStorage storage;
    private int cursorId;
    private Type fakeType;
    private WebPreparedStatement<TestObj> stmt;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        storage = mock(WebStorage.class);
        cursorId = 4441;
        fakeType = mock(Type.class);
        stmt = mock(WebPreparedStatement.class);
    }
    
    @Test
    public void testHasNext() {
        boolean hasMoreBatches = false;
        TestObj[] dataBatch = new TestObj[] { };
        WebCursor<TestObj> cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        assertFalse("no results and no more batches", cursor.hasNext());
        hasMoreBatches = true;
        cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        assertTrue("no results, but more batches", cursor.hasNext());
        TestObj o1 = new TestObj();
        dataBatch = new TestObj[] { o1 };
        hasMoreBatches = false;
        cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        assertTrue("one result, no more batches", cursor.hasNext());
    }
    
    @Test
    public void testNext() {
        // test empty results and no more batches
        boolean hasMoreBatches = false;
        TestObj[] dataBatch = new TestObj[] { };
        WebCursor<TestObj> cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        try {
            cursor.next();
            fail("Cursor should throw a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // pass
        }
        
        // test empty results but more batches
        hasMoreBatches = true;
        cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        WebQueryResponse<TestObj> response = new WebQueryResponse<>();
        response.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        response.setCursorId(cursorId);
        response.setHasMoreBatches(false);
        TestObj o1 = new TestObj();
        o1.setProperty1("next-test");
        response.setResultList(new TestObj[] { o1 } );
        WebQueryResponse<TestObj> second = new WebQueryResponse<>();
        assertNull(second.getResultList());
        // be sure to return a bad result should storage.getMore() be called
        // more than once
        when(storage.getMore(cursorId, fakeType, Cursor.DEFAULT_BATCH_SIZE, stmt)).thenReturn(response).thenReturn(second);
        TestObj actual = cursor.next();
        assertEquals("next-test", actual.getProperty1());
        
        // test non-empty results and no more batches
        hasMoreBatches = false;
        o1.setAgentId("foo-agent-123");
        dataBatch = new TestObj[] { o1 };
        cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        actual = cursor.next();
        assertEquals("foo-agent-123", actual.getAgentId());
    }
    
    /**
     * Tests next() calls where get-more fails due to an expired or missing
     * cursor on the web endpoint.
     */
    @Test
    public void testNextGetMoreBadCursorFailure() {
        boolean hasMoreBatches = true;
        TestObj[] dataBatch = new TestObj[] { };
        WebCursor<TestObj> cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        WebQueryResponse<TestObj> response = new WebQueryResponse<>();
        response.setResponseCode(PreparedStatementResponseCode.GET_MORE_NULL_CURSOR);
        response.setCursorId(cursorId);
        response.setHasMoreBatches(false);
        when(storage.getMore(cursorId, fakeType, Cursor.DEFAULT_BATCH_SIZE, stmt)).thenReturn(response);
        try {
            cursor.next();
            fail("Expected StorageException to be thrown");
        } catch (StorageException e) {
            assertEquals("[get-more] Failed to get more results for cursorId: 4441" +
                         " This may be caused because the cursor timed out. " +
                         "Resubmitting the original query might be an approach to fix it. " +
                         "See server logs for more details.",
                         e.getMessage());
        }
    }
    
    /**
     * Tests next() calls where get-more fails due to some unknown reason.
     */
    @Test
    public void testNextGenericGetMoreFailure() {
        boolean hasMoreBatches = true;
        TestObj[] dataBatch = new TestObj[] { };
        WebCursor<TestObj> cursor = new WebCursor<>(storage, dataBatch, hasMoreBatches, cursorId, fakeType, stmt);
        WebQueryResponse<TestObj> response = new WebQueryResponse<>();
        response.setResponseCode(PreparedStatementResponseCode.QUERY_FAILURE);
        response.setCursorId(cursorId);
        response.setHasMoreBatches(false);
        when(storage.getMore(cursorId, fakeType, Cursor.DEFAULT_BATCH_SIZE, stmt)).thenReturn(response);
        try {
            cursor.next();
            fail("Expected StorageException to be thrown");
        } catch (StorageException e) {
            assertEquals("[get-more] Failed to get more results for cursorId: " + 
                         "4441. See server logs for details.",
                         e.getMessage());
        }
    }
    
    /**
     * Verify that if a batch size is explicitly set it gets passed on to
     * web storage on the next call to getMore. Default batch size is accounted
     * for in other tests (e.g. testNext()).
     */
    @Test
    public void testSetBatchSize() {
        boolean hasMoreBatches = true;
        TestObj[] empty = new TestObj[] {};
        WebCursor<TestObj> cursor = new WebCursor<>(storage, empty, hasMoreBatches, cursorId, fakeType, stmt);
        try {
            cursor.setBatchSize(-1);
            fail("expected IAE for batch size of -1");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        try {
            cursor.setBatchSize(0);
            fail("expected IAE for batch size of 0");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        cursor = new WebCursor<>(storage, empty, hasMoreBatches, cursorId, fakeType, stmt);
        cursor.setBatchSize(128);
        TestObj o1 = new TestObj();
        WebQueryResponse<TestObj> response = new WebQueryResponse<>();
        response.setResultList(new TestObj[] { o1 });
        response.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        when(storage.getMore(cursorId, fakeType, 128, stmt)).thenReturn(response);
        cursor.next();
        verify(storage).getMore(cursorId, fakeType, 128, stmt);
        Mockito.verifyNoMoreInteractions(storage);
    }
    
}
