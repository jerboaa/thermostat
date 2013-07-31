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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class PreparedStatementImplTest {

    @Test
    public void failToSetIndexOutOfBounds() {
        PreparedStatementImpl<?> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setInt(1, 3);
        preparedStatement.setString(0, "testing");
        try {
            preparedStatement.setLong(3, 1);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            preparedStatement.setInt(4, 1);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            preparedStatement.setString(10, "ignored");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            preparedStatement.setStringList(3, new String[] { "ignored" });
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
    
    @Test
    public void canDoParsingPatchingAndExecution() throws Exception {
        String queryString = "QUERY foo WHERE 'a' = ?s";
        @SuppressWarnings("unchecked")
        StatementDescriptor<Pojo> desc = (StatementDescriptor<Pojo>) mock(StatementDescriptor.class);
        when(desc.getQueryDescriptor()).thenReturn(queryString);
        @SuppressWarnings("unchecked")
        Category<Pojo> mockCategory = (Category<Pojo>) mock(Category.class);
        when(desc.getCategory()).thenReturn(mockCategory);
        when(mockCategory.getName()).thenReturn("foo");
        BackingStorage storage = mock(BackingStorage.class);
        StubQuery stmt = new StubQuery();
        when(storage.createQuery(mockCategory)).thenReturn(stmt);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(storage, desc);
        preparedStatement.setString(0, "foo");
        preparedStatement.executeQuery();
        assertTrue(stmt.called);
        LiteralExpression<Key<String>> o1 = new LiteralExpression<>(new Key<String>("a", false));
        LiteralExpression<String> o2 = new LiteralExpression<>("foo"); 
        BinaryComparisonExpression<String> binComp = new BinaryComparisonExpression<>(
                o1, BinaryComparisonOperator.EQUALS, o2);
        assertEquals(binComp, stmt.expr);
    }
    
    @Test
    public void failExecutionWithWronglyTypedParams() throws Exception {
        String queryString = "QUERY foo WHERE 'a' = ?b";
        @SuppressWarnings("unchecked")
        StatementDescriptor<Pojo> desc = (StatementDescriptor<Pojo>) mock(StatementDescriptor.class);
        when(desc.getQueryDescriptor()).thenReturn(queryString);
        @SuppressWarnings("unchecked")
        Category<Pojo> mockCategory = (Category<Pojo>) mock(Category.class);
        when(desc.getCategory()).thenReturn(mockCategory);
        when(mockCategory.getName()).thenReturn("foo");
        BackingStorage storage = mock(BackingStorage.class);
        StubQuery stmt = new StubQuery();
        when(storage.createQuery(mockCategory)).thenReturn(stmt);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(storage, desc);
        preparedStatement.setString(0, "foo");
        try {
            preparedStatement.executeQuery();
            fail("Should have thrown SEE due to type mismatch. boolean vs. string");
        } catch (StatementExecutionException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch"));
        }
    }
    
    private static class StubQuery implements Query<Pojo> {

        private Expression expr;
        private boolean called = false;
        
        @Override
        public void where(Expression expr) {
            this.expr = expr;
        }

        @Override
        public void sort(Key<?> key, SortDirection direction) {
            // nothing
        }

        @Override
        public void limit(int n) {
            // nothing
        }

        @Override
        public Cursor<Pojo> execute() {
            called = true;
            return null;
        }

        @Override
        public Expression getWhereExpression() {
            // not implemented
            return null;
        }
        
    }
}
