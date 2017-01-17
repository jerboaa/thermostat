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

package com.redhat.thermostat.web.server;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;

public class PreparedStatementManagerTest {
    
    /**
     * Verify that every call to put creates a new ID entry only if
     * statements (id'ed by descriptor) are different.
     */
    @Test
    public void canCreateAndPutStmt() {
        PreparedStatementManager manager = new PreparedStatementManager();
        UUID serverToken = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> targetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        StatementDescriptor<TestPojo> testDesc = new StatementDescriptor<>(mock(Category.class), "unused");
        Class<TestPojo> testClass = TestPojo.class;
        SharedStateId id = manager.createAndPutHolder(serverToken, targetStmt, testClass, testDesc);
        assertNotNull(id);
        assertEquals(serverToken, id.getServerToken());
        assertEquals("counter starts with 0", 0, id.getId());
        // do it again with a different statement
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> otherTargetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        StatementDescriptor<TestPojo> otherTestDesc = new StatementDescriptor<>(mock(Category.class), "other");
        SharedStateId otherId = manager.createAndPutHolder(serverToken, otherTargetStmt, testClass, otherTestDesc);
        assertNotNull(otherId);
        assertFalse(id.equals(otherId));
        assertEquals(1, otherId.getId());
        assertEquals(serverToken, otherId.getServerToken());
        // do it once more with same token, which should yield the same id than
        // we used to get for the first createAndPutHolder() call.
        SharedStateId thirdId = manager.createAndPutHolder(serverToken, targetStmt, testClass, testDesc);
        assertEquals(id, thirdId);
        assertSame(id, thirdId);
    }
    
    /**
     * Verify that adding a new value when the int ID would overflow throws an
     * exception.
     */
    @Test
    public void intIdOverflowThrowsException() {
        PreparedStatementManager manager = new PreparedStatementManager(Integer.MAX_VALUE - 1);
        UUID serverNonce = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> targetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        StatementDescriptor<TestPojo> testDesc = new StatementDescriptor<>(mock(Category.class), "unused");
        Class<TestPojo> testClass = TestPojo.class;
        try {
            manager.createAndPutHolder(serverNonce, targetStmt, testClass, testDesc);
            fail("Should have thrown ISE due to int id overflow");
        } catch (IllegalStateException e) {
            assertEquals("Too many different statements!", e.getMessage());
        }
    }
    
    /**
     * Basic parameters must not be null.
     */
    @Test
    public void rejectsNullValuesAsParameters() {
        PreparedStatementManager manager = new PreparedStatementManager();
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> targetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        StatementDescriptor<TestPojo> testDesc = new StatementDescriptor<>(mock(Category.class), "unused");
        Class<TestPojo> testClass = TestPojo.class;
        try {
            manager.createAndPutHolder(null, targetStmt, testClass, testDesc);
            fail("Expected NPE due to null server token");
        } catch (NullPointerException e) {
            // pass
        }
        UUID serverNonce = UUID.randomUUID();
        try {
            manager.createAndPutHolder(serverNonce, null, testClass, testDesc);
            fail("Expected NPE due to null target stmt");
        } catch (NullPointerException e) {
            // pass
        }
        try {
            manager.createAndPutHolder(serverNonce, targetStmt, null, testDesc);
            fail("Expected NPE due to null data class");
        } catch (NullPointerException e) {
            // pass
        }
        try {
            manager.createAndPutHolder(serverNonce, targetStmt, testClass, null);
            fail("Expected NPE due to null stmt descriptor");
        } catch (NullPointerException e) {
            // pass
        }
    }
    
    @Test
    public void canGetAddedHolderItemViaID() {
        PreparedStatementManager manager = new PreparedStatementManager();
        UUID serverNonce = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> targetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        StatementDescriptor<TestPojo> testDesc = new StatementDescriptor<>(mock(Category.class), "unused");
        Class<TestPojo> testClass = TestPojo.class;
        SharedStateId id = manager.createAndPutHolder(serverNonce, targetStmt, testClass, testDesc);
        PreparedStatementHolder<TestPojo> holder = manager.getStatementHolder(id);
        assertNotNull(holder);
        assertSame(serverNonce, holder.getId().getServerToken());
        assertSame(id, holder.getId());
        assertSame(targetStmt, holder.getStmt());
        assertSame(testClass, holder.getDataClass());
        assertSame(testDesc, holder.getStatementDescriptor());
        // do it again with an equal ID
        SharedStateId retrievalId = new SharedStateId(0, serverNonce);
        assertTrue(retrievalId.equals(id));
        holder = manager.getStatementHolder(retrievalId);
        assertEquals(id, holder.getId());
        assertNotSame(retrievalId, holder.getId());
        assertSame(id, holder.getId());
        assertSame(targetStmt, holder.getStmt());
        assertSame(testClass, holder.getDataClass());
        assertSame(testDesc, holder.getStatementDescriptor());
        
        // unknown server token should return null
        assertNull(manager.getStatementHolder(new SharedStateId(0, UUID.randomUUID())));
    }
    
    @Test
    public void canGetAddedHolderItemViaDescriptor() {
        PreparedStatementManager manager = new PreparedStatementManager();
        UUID serverNonce = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> targetStmt = mock(PreparedStatement.class);
        @SuppressWarnings({ "unchecked" })
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> testDesc = new StatementDescriptor<>(cat, "no-matter");
        Class<TestPojo> testClass = TestPojo.class;
        SharedStateId id = manager.createAndPutHolder(serverNonce, targetStmt, testClass, testDesc);
        PreparedStatementHolder<TestPojo> holder = manager.getStatementHolder(testDesc);
        assertNotNull(holder);
        assertSame(serverNonce, holder.getId().getServerToken());
        assertSame(id, holder.getId());
        assertSame(targetStmt, holder.getStmt());
        assertSame(testClass, holder.getDataClass());
        assertSame(testDesc, holder.getStatementDescriptor());
        
        // do it again with an equal descriptor
        StatementDescriptor<TestPojo> equalDesc = new StatementDescriptor<>(cat, "no-matter");
        assertNotSame(testDesc, equalDesc);
        assertEquals(testDesc, equalDesc);
        holder = manager.getStatementHolder(equalDesc);
        assertNotNull(holder);
        assertSame(serverNonce, holder.getId().getServerToken());
        assertSame(id, holder.getId());
        assertSame(targetStmt, holder.getStmt());
        assertSame(testClass, holder.getDataClass());
        assertSame(testDesc, holder.getStatementDescriptor());
    }

    private static class TestPojo implements Pojo {
        // nothing
    }
}
