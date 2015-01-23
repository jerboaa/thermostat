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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class PreparedStatementImplTest {
    
    private static int counter = 0;
    
    /*
     * Category names need to be unique. In order to prevent IllegalStateExceptions
     * create a new category name each time this is called.
     */
    private synchronized String getNextCategoryName() {
        String name = "foo-table-" + counter;
        counter++;
        return name;
    }
    
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
    public void canDoParsingPatchingAndExecutionQuery() throws Exception {
        String categoryName = getNextCategoryName();
        String queryString = "QUERY " + categoryName + " WHERE 'a' = ?s";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("a"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, queryString);
        BackingStorage storage = mock(BackingStorage.class);
        StubQuery<FooPojo> stmt = new StubQuery<>();
        when(storage.createQuery(fooCategory)).thenReturn(stmt);
        PreparedStatementImpl<FooPojo> preparedStatement = new PreparedStatementImpl<>(storage, desc);
        preparedStatement.setString(0, "foo");
        preparedStatement.executeQuery();
        assertTrue(stmt.called);
        LiteralExpression<Key<String>> o1 = new LiteralExpression<>(new Key<String>("a"));
        LiteralExpression<String> o2 = new LiteralExpression<>("foo"); 
        BinaryComparisonExpression<String> binComp = new BinaryComparisonExpression<>(
                o1, BinaryComparisonOperator.EQUALS, o2);
        assertEquals(binComp, stmt.expr);
    }
    
    @Test
    public void canDoParsingPatchingAndExecutionForAdd() throws Exception {
        String categoryName = getNextCategoryName();
        String addString = "ADD " + categoryName +" SET 'foo' = ?s";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("foo"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, addString);
        BackingStorage storage = mock(BackingStorage.class);
        TestAdd<FooPojo> add = new TestAdd<>();
        when(storage.createAdd(fooCategory)).thenReturn(add);
        PreparedStatement<FooPojo> preparedStatement = new PreparedStatementImpl<FooPojo>(storage, desc);
        preparedStatement.setString(0, "foo-val");
        assertFalse(add.executed);
        try {
            // this should call add.apply();
            preparedStatement.execute();
        } catch (StatementExecutionException e) {
            fail(e.getMessage());
        }
        assertFalse(add.values.isEmpty());
        assertTrue(add.executed);
        assertEquals(1, add.values.keySet().size());
        assertEquals("foo-val", add.values.get("foo"));
    }
    
    @Test
    public void canDoParsingPatchingAndExecutionForAddInvolvingFancyPojo() throws Exception {
        String categoryName = getNextCategoryName();
        String addString = "ADD " + categoryName + " SET 'fancyFoo' = ?p[";
        Category<FancyFoo> fooCategory = new Category<>(categoryName, FancyFoo.class, new Key<String>("fancyFoo"));
        StatementDescriptor<FancyFoo> desc = new StatementDescriptor<>(fooCategory, addString);
        BackingStorage storage = mock(BackingStorage.class);
        TestAdd<FancyFoo> add = new TestAdd<>();
        when(storage.createAdd(fooCategory)).thenReturn(add);
        PreparedStatement<FancyFoo> preparedStatement = new PreparedStatementImpl<FancyFoo>(storage, desc);
        FooPojo one = new FooPojo();
        one.setFoo("one");
        FooPojo two = new FooPojo();
        two.setFoo("two");
        FooPojo[] list = new FooPojo[] {
                one, two
        };
        preparedStatement.setPojoList(0, list);
        assertFalse(add.executed);
        try {
            // this should call add.apply();
            preparedStatement.execute();
        } catch (StatementExecutionException e) {
            fail(e.getMessage());
        }
        assertFalse(add.values.isEmpty());
        assertEquals(1, add.values.keySet().size());
        assertTrue(add.executed);
        FooPojo[] fancyFoo = (FooPojo[])add.values.get("fancyFoo");
        assertEquals(2, fancyFoo.length);
        FooPojo first = fancyFoo[0];
        FooPojo second = fancyFoo[1];
        assertEquals("one", first.getFoo());
        assertEquals("two", second.getFoo());
    }
    
    @Test
    public void canDoParsingPatchingAndExecutionForUpdate() throws Exception {
        String categoryName = getNextCategoryName();
        String addString = "UPDATE " + categoryName + " SET 'foo' = ?s WHERE 'foo' = ?s";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("foo"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, addString);
        BackingStorage storage = mock(BackingStorage.class);
        TestUpdate update = new TestUpdate();
        when(storage.createUpdate(fooCategory)).thenReturn(update);
        PreparedStatement<FooPojo> preparedStatement = new PreparedStatementImpl<FooPojo>(storage, desc);
        preparedStatement.setString(0, "foo-val");
        preparedStatement.setString(1, "nice");
        assertFalse(update.executed);
        try {
            // this should call apply();
            preparedStatement.execute();
        } catch (StatementExecutionException e) {
            fail(e.getMessage());
        }
        assertTrue(update.executed);
        assertEquals(1, update.updates.size());
        Pair<String, Object> item = update.updates.get(0);
        assertEquals("foo", item.getFirst());
        assertEquals("foo-val", item.getSecond());
        LiteralExpression<Key<String>> o1 = new LiteralExpression<>(new Key<String>("foo"));
        LiteralExpression<String> o2 = new LiteralExpression<>("nice"); 
        BinaryComparisonExpression<String> binComp = new BinaryComparisonExpression<>(
                o1, BinaryComparisonOperator.EQUALS, o2);
        assertEquals(binComp, update.where);
    }
    
    @Test
    public void canDoParsingPatchingAndExecutionForReplace() throws Exception {
        String categoryName = getNextCategoryName();
        String addString = "REPLACE " + categoryName + " SET 'foo' = ?s WHERE 'foo' = ?s";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("foo"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, addString);
        BackingStorage storage = mock(BackingStorage.class);
        TestReplace<FooPojo> replace = new TestReplace<>();
        when(storage.createReplace(fooCategory)).thenReturn(replace);
        PreparedStatement<FooPojo> preparedStatement = new PreparedStatementImpl<FooPojo>(storage, desc);
        preparedStatement.setString(0, "foo-val");
        preparedStatement.setString(1, "bar");
        assertFalse(replace.executed);
        try {
            // this should call apply();
            preparedStatement.execute();
        } catch (StatementExecutionException e) {
            fail(e.getMessage());
        }
        assertFalse(replace.values.isEmpty());
        assertTrue(replace.executed);
        assertEquals("foo-val", replace.values.get("foo"));
        LiteralExpression<Key<String>> o1 = new LiteralExpression<>(new Key<String>("foo"));
        LiteralExpression<String> o2 = new LiteralExpression<>("bar"); 
        BinaryComparisonExpression<String> binComp = new BinaryComparisonExpression<>(
                o1, BinaryComparisonOperator.EQUALS, o2);
        assertEquals(binComp, replace.where);
    }
    
    @Test
    public void canDoParsingPatchingAndExecutionForRemove() throws Exception {
        String categoryName = getNextCategoryName();
        String addString = "REMOVE " + categoryName + " WHERE 'fooRem' = ?s";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("foo"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, addString);
        BackingStorage storage = mock(BackingStorage.class);
        TestRemove<FooPojo> remove = new TestRemove<>();
        when(storage.createRemove(fooCategory)).thenReturn(remove);
        PreparedStatement<FooPojo> preparedStatement = new PreparedStatementImpl<FooPojo>(storage, desc);
        preparedStatement.setString(0, "bar");
        assertFalse(remove.executed);
        try {
            // this should call apply();
            preparedStatement.execute();
        } catch (StatementExecutionException e) {
            fail(e.getMessage());
        }
        assertTrue(remove.executed);
        LiteralExpression<Key<String>> o1 = new LiteralExpression<>(new Key<String>("fooRem"));
        LiteralExpression<String> o2 = new LiteralExpression<>("bar"); 
        BinaryComparisonExpression<String> binComp = new BinaryComparisonExpression<>(
                o1, BinaryComparisonOperator.EQUALS, o2);
        assertEquals(binComp, remove.where);
    }
    
    @Test
    public void failExecutionWithWronglyTypedParams() throws Exception {
        String categoryName = getNextCategoryName();
        String queryString = "QUERY " + categoryName + " WHERE 'a' = ?b";
        Category<FooPojo> fooCategory = new Category<>(categoryName, FooPojo.class, new Key<String>("a"));
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(fooCategory, queryString);
        BackingStorage storage = mock(BackingStorage.class);
        StubQuery<FooPojo> stmt = new StubQuery<>();
        when(storage.createQuery(fooCategory)).thenReturn(stmt);
        PreparedStatementImpl<FooPojo> preparedStatement = new PreparedStatementImpl<>(storage, desc);
        preparedStatement.setString(0, "foo");
        try {
            preparedStatement.executeQuery();
            fail("Should have thrown SEE due to type mismatch. boolean vs. string");
        } catch (StatementExecutionException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch"));
        }
    }
    
    private static class TestAdd<T extends Pojo> implements Add<T> {

        private Map<String, Object> values = new HashMap<>();
        private boolean executed = false;
        
        @Override
        public void set(String key, Object value) {
            values.put(key, value);
        }

        @Override
        public int apply() {
            executed = true;
            return 0;
        }

        @Override
        public Statement<T> getRawDuplicate() {
            // we don't duplicate for this test
            return this;
        }

    }
    
    private static class TestReplace<T extends Pojo> implements Replace<T> {

        private Map<String, Object> values = new HashMap<>();
        private boolean executed = false;
        private Expression where;
        
        @Override
        public void set(String key, Object value) {
            values.put(key, value);
        }

        @Override
        public void where(Expression expression) {
            this.where = expression;
        }

        @Override
        public int apply() {
            this.executed = true;
            return 0;
        }

        @Override
        public Statement<T> getRawDuplicate() {
            // we don't duplicate for this test
            return this;
        }
        
    }
    
    private static class TestUpdate implements Update<FooPojo> {

        private Expression where;
        private List<Pair<String, Object>> updates = new ArrayList<>();
        private boolean executed = false;
        
        @Override
        public void where(Expression expr) {
            this.where = expr;
        }

        @Override
        public void set(String key, Object value) {
            Pair<String, Object> item = new Pair<>(key, value);
            updates.add(item);
        }

        @Override
        public int apply() {
            this.executed = true;
            return 0;
        }

        @Override
        public Statement<FooPojo> getRawDuplicate() {
            // we don't duplicate for this test
            return this;
        }
        
    }
    
    private static class TestRemove<T extends Pojo> implements Remove<T> {

        private Expression where;
        private boolean executed = false;
        
        @Override
        public void where(Expression where) {
            this.where = where;
        }

        @Override
        public int apply() {
            this.executed = true;
            return 0;
        }

        @Override
        public Statement<T> getRawDuplicate() {
            // we don't duplicate for this test
            return this;
        }
        
    }
    
    public static class FooPojo implements Pojo {
        
        String foo;
        
        public void setFoo(String foo) {
            this.foo = foo;
        }
        
        public String getFoo() {
            return this.foo;
        }
    }
    
    public static class FancyFoo extends FooPojo {
        
        private FooPojo[] fancyFoo;

        public FooPojo[] getFancyFoo() {
            return fancyFoo;
        }

        public void setFancyFoo(FooPojo[] fancyFoo) {
            this.fancyFoo = fancyFoo;
        }
        
    }
    
    private static class StubQuery<T extends Pojo> implements Query<T> {

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
        public Cursor<T> execute() {
            called = true;
            return null;
        }

        @Override
        public Expression getWhereExpression() {
            // not implemented
            return null;
        }

        @Override
        public Statement<T> getRawDuplicate() {
            // For this test, we don't duplicate
            return this;
        }
        
    }
}

