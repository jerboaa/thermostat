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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DataModifyingStatement;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class ParsedStatementImplTest {

    private Query<Pojo> statement;
    
    @Before
    public void setup() {
        statement = new TestQuery();
    }
    
    @After
    public void tearDown() {
        statement = null;
    }
    
    @Test
    public void patchingDuplicatesStatement() throws IllegalPatchException {
        @SuppressWarnings("unchecked")
        Statement<Pojo> stmt = (Statement<Pojo>)mock(Statement.class);
        @SuppressWarnings("unchecked")
        Statement<Pojo> mock2 = mock(Statement.class);
        when(stmt.getRawDuplicate()).thenReturn(mock2);
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        parsedStmt.setSetList(new SetList());
        parsedStmt.setSuffixExpression(suffixExpn);
        
        Statement<Pojo> result = parsedStmt.patchStatement(new PreparedParameter[] {});
        assertNotSame("Statement should get duplicated on patching", stmt, result);
        assertSame(mock2, result);
    }
    
    @Test
    public void canPatchWhereAndExpr() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        parsedStmt.setSetList(new SetList());
        // WHERE a = ? AND c = ?
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode leftEqual = new BinaryExpressionNode(and);
        BinaryExpressionNode rightEqual = new BinaryExpressionNode(and);
        leftEqual.setOperator(BinaryComparisonOperator.EQUALS);
        rightEqual.setOperator(BinaryComparisonOperator.EQUALS);
        and.setLeftChild(leftEqual);
        and.setRightChild(rightEqual);
        TerminalNode a = new TerminalNode(leftEqual);
        Key<String> aKey = new Key<>("a");
        a.setValue(aKey);
        TerminalNode b = new TerminalNode(leftEqual);
        UnfinishedValueNode patchB = new UnfinishedValueNode();
        patchB.setParameterIndex(0);
        patchB.setType(String.class);
        b.setValue(patchB);
        leftEqual.setLeftChild(a);
        leftEqual.setRightChild(b);
        TerminalNode c = new TerminalNode(rightEqual);
        c.setValue("c");
        rightEqual.setLeftChild(c);
        TerminalNode d = new TerminalNode(rightEqual);
        UnfinishedValueNode dPatch = new UnfinishedValueNode();
        dPatch.setParameterIndex(1);
        dPatch.setType(int.class);
        d.setValue(dPatch);
        rightEqual.setRightChild(d);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        // next, create the PreparedStatement we are going to use for
        // patching.
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "test1");
        preparedStatement.setInt(1, 2);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Query<Pojo> query = (Query<Pojo>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryLogicalExpression);
        BinaryLogicalExpression<?, ?> andFinal = (BinaryLogicalExpression<?, ?>) expectedExpression;
        assertEquals(BinaryLogicalOperator.AND, andFinal.getOperator());
        assertTrue(andFinal.getLeftOperand() instanceof BinaryComparisonExpression);
        assertTrue(andFinal.getRightOperand() instanceof BinaryComparisonExpression);
        BinaryComparisonExpression<?> left = (BinaryComparisonExpression<?>)andFinal.getLeftOperand();
        BinaryComparisonExpression<?> right = (BinaryComparisonExpression<?>)andFinal.getRightOperand();
        assertEquals(BinaryComparisonOperator.EQUALS, left.getOperator());
        assertEquals(BinaryComparisonOperator.EQUALS, right.getOperator());
        assertTrue(left.getLeftOperand() instanceof LiteralExpression);
        assertTrue(left.getRightOperand() instanceof LiteralExpression);
        LiteralExpression<?> leftLiteral1 = (LiteralExpression<?>)left.getLeftOperand();
        LiteralExpression<?> rightLiteral1 = (LiteralExpression<?>)left.getRightOperand();
        assertEquals(aKey, leftLiteral1.getValue());
        assertEquals("test1", rightLiteral1.getValue());
        LiteralExpression<?> leftLiteral2 = (LiteralExpression<?>)right.getLeftOperand();
        LiteralExpression<?> rightLiteral2 = (LiteralExpression<?>)right.getRightOperand();
        assertEquals("c", leftLiteral2.getValue());
        // right literal value should have been patched to a "d"
        assertEquals(2, rightLiteral2.getValue());
    }
    
    @Test
    public void canPatchBasicWhereEquals() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        parsedStmt.setSetList(new SetList());
        // WHERE a = ?
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(and);
        a.setValue(new Key<>("a"));
        TerminalNode b = new TerminalNode(and);
        UnfinishedValueNode bPatch = new UnfinishedValueNode();
        bPatch.setParameterIndex(0);
        bPatch.setType(boolean.class);
        b.setValue(bPatch);
        and.setLeftChild(a);
        and.setRightChild(b);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        // next, create the PreparedStatement we are going to use for
        // patching.
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(1);
        preparedStatement.setBoolean(0, true);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Query<?> query = (Query<?>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        BinaryComparisonExpression<?> root = (BinaryComparisonExpression<?>)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        LiteralExpression<?> leftLiteral1 = (LiteralExpression<?>)root.getLeftOperand();
        LiteralExpression<?> rightLiteral1 = (LiteralExpression<?>)root.getRightOperand();
        assertEquals(new Key<>("a"), leftLiteral1.getValue());
        // this should have gotten patched to a "b"
        assertEquals(true, rightLiteral1.getValue());
        // now do it again with a different value
        preparedStatement = new PreparedStatementImpl<>(1);
        preparedStatement.setBoolean(0, false);
        params = preparedStatement.getParams();
        query = (Query<?>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        q = (TestQuery)query;
        expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        root = (BinaryComparisonExpression<?>)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        leftLiteral1 = (LiteralExpression<?>)root.getLeftOperand();
        rightLiteral1 = (LiteralExpression<?>)root.getRightOperand();
        assertEquals(new Key<>("a"), leftLiteral1.getValue());
        assertEquals(false, rightLiteral1.getValue());
    }
    
    @Test
    public void canPatchBasicWhereEqualsLHSKeyAndRHSValue() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        parsedStmt.setSetList(new SetList());
        // WHERE ?s = ?b
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(and);
        UnfinishedValueNode aPatch = new UnfinishedValueNode();
        aPatch.setLHS(true);
        aPatch.setType(String.class);
        aPatch.setParameterIndex(0);
        a.setValue(aPatch);
        TerminalNode b = new TerminalNode(and);
        UnfinishedValueNode bPatch = new UnfinishedValueNode();
        bPatch.setParameterIndex(1);
        bPatch.setType(boolean.class);
        b.setValue(bPatch);
        and.setLeftChild(a);
        and.setRightChild(b);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        // next, create the PreparedStatement we are going to use for
        // patching.
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "a");
        preparedStatement.setBoolean(1, true);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Query<?> query = (Query<?>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        BinaryComparisonExpression<?> root = (BinaryComparisonExpression<?>)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        LiteralExpression<?> leftLiteral1 = (LiteralExpression<?>)root.getLeftOperand();
        LiteralExpression<?> rightLiteral1 = (LiteralExpression<?>)root.getRightOperand();
        assertEquals(new Key<>("a"), leftLiteral1.getValue());
        // this should have gotten patched to a "b"
        assertEquals(true, rightLiteral1.getValue());
        // now do it again with a different value
        preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "a");
        preparedStatement.setBoolean(1, false);
        params = preparedStatement.getParams();
        query = (Query<?>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        q = (TestQuery)query;
        expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        root = (BinaryComparisonExpression<?>)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        leftLiteral1 = (LiteralExpression<?>)root.getLeftOperand();
        rightLiteral1 = (LiteralExpression<?>)root.getRightOperand();
        assertEquals(new Key<>("a"), leftLiteral1.getValue());
        assertEquals(false, rightLiteral1.getValue());
    }
    
    @Test
    public void canPatchBasicLimit() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        parsedStmt.setSetList(new SetList());
        SuffixExpression suffixExpn = new SuffixExpression();
        LimitExpression limitExpnToPatch = new LimitExpression();
        UnfinishedLimitValue unfinished = new UnfinishedLimitValue();
        unfinished.setParameterIndex(0);
        limitExpnToPatch.setValue(unfinished);
        suffixExpn.setLimitExpn(limitExpnToPatch);
        suffixExpn.setSortExpn(null);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        // set the value for the one unfinished param
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(1);
        preparedStatement.setInt(0, 3);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Query<?> query = (Query<?>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        assertEquals(3, q.limitVal);
    }
    
    private SetList buildSetList() {
        // Build this set list, which corresponds to the TestPojo below
        // SET 'writerId' = ?s , 'fooTimeStamp' = ?l
        SetList setList = new SetList();
        SetListValue writerId = new SetListValue();
        TerminalNode writerKey = new TerminalNode(null);
        writerKey.setValue(new Key<>("writerId"));
        TerminalNode writerValue = new TerminalNode(null);
        UnfinishedValueNode unfinishedWriter = new UnfinishedValueNode();
        unfinishedWriter.setParameterIndex(0);
        unfinishedWriter.setType(String.class);
        unfinishedWriter.setLHS(false);
        writerValue.setValue(unfinishedWriter);
        writerId.setKey(writerKey);
        writerId.setValue(writerValue);
        setList.addValue(writerId);
        SetListValue fooTimeStamp = new SetListValue();
        TerminalNode timeStampKey = new TerminalNode(null);
        timeStampKey.setValue(new Key<>("fooTimeStamp"));
        fooTimeStamp.setKey(timeStampKey);
        TerminalNode timeStampVal = new TerminalNode(null);
        UnfinishedValueNode timeStampUnfinished = new UnfinishedValueNode();
        timeStampUnfinished.setLHS(false);
        timeStampUnfinished.setParameterIndex(1);
        timeStampUnfinished.setType(long.class);
        timeStampVal.setValue(timeStampUnfinished);
        fooTimeStamp.setValue(timeStampVal);
        setList.addValue(fooTimeStamp);
        return setList;
    }
    
    /*
     * Test for patching of:
     *  "ADD something SET 'writerId' = ?s, 'fooTimeStamp' = ?l"
     */
    @Test
    public void canPatchBasicSetListAdd() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        DataModifyingStatement<TestPojo> stmt = new TestAdd<>();
        ParsedStatementImpl<TestPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        SetList setList = buildSetList();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(2, setList.getValues().size());
        parsedStmt.setSetList(setList);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "foo-writer");
        preparedStatement.setLong(1, Long.MAX_VALUE);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Add<TestPojo> add = (Add<TestPojo>)parsedStmt.patchStatement(params);
        assertTrue(add instanceof TestAdd);
        TestAdd<TestPojo> q = (TestAdd<TestPojo>)add;
        Map<String, Object> vals = q.values;
        assertEquals(2, vals.keySet().size());
        assertEquals("foo-writer", vals.get("writerId"));
        assertEquals(Long.MAX_VALUE, vals.get("fooTimeStamp"));
    }
    
    /*
     * Test for patching of:
     *  "ADD something SET 'someList' = ?p["
     */
    @Test
    public void canPatchSetListAddWithPojoList() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        DataModifyingStatement<FancyPojo> stmt = new TestAdd<>();
        ParsedStatementImpl<FancyPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        SetList setList = new SetList();
        parsedStmt.setSetList(setList);
        TerminalNode someProperty = new TerminalNode(null);
        someProperty.setValue(new Key<>("someList"));
        TerminalNode somePropertyVal = new TerminalNode(null);
        UnfinishedValueNode unfinishedPojoList = new UnfinishedValueNode();
        unfinishedPojoList.setLHS(false);
        unfinishedPojoList.setType(Pojo[].class);
        unfinishedPojoList.setParameterIndex(0);
        somePropertyVal.setValue(unfinishedPojoList);
        SetListValue value = new SetListValue();
        value.setKey(someProperty);
        value.setValue(somePropertyVal);
        setList.addValue(value);
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(1, setList.getValues().size());
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(1);
        TestPojo elem1 = new TestPojo();
        elem1.setFooTimeStamp(-300);
        elem1.setWriterId("elem1");
        TestPojo elem2 = new TestPojo();
        elem2.setFooTimeStamp(-301);
        elem2.setWriterId("elem2");
        TestPojo[] elems = new TestPojo[] {
                elem1, elem2
        };
        preparedStatement.setPojoList(0, elems);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Add<FancyPojo> add = (Add<FancyPojo>)parsedStmt.patchStatement(params);
        assertTrue(add instanceof TestAdd);
        TestAdd<FancyPojo> q = (TestAdd<FancyPojo>)add;
        Map<String, Object> vals = q.values;
        assertEquals(1, vals.keySet().size());
        assertEquals(null, vals.get("writerId"));
        assertNotNull(vals.get("someList"));
        Object someList = vals.get("someList");
        assertTrue(someList instanceof TestPojo[]);
        TestPojo[] tPojo = (TestPojo[])someList;
        assertEquals(2, tPojo.length);
        TestPojo first = tPojo[0];
        TestPojo second = tPojo[1];
        assertEquals(elem1, first);
        assertEquals(elem2, second);
        assertEquals("elem1", first.getWriterId());
        assertEquals(-300, first.getFooTimeStamp());
        assertEquals("elem2", second.getWriterId());
        assertEquals(-301, second.getFooTimeStamp());
    }
    
    /*
     * Test for patching of:
     *  "REPLACE something SET ?s = 'foo-bar', 'fooTimeStamp' = ?l WHERE 'foo' = ?i"
     */
    @Test
    public void canPatchBasicSetListReplace() throws IllegalPatchException {
        DataModifyingStatement<TestPojo> stmt = new TestReplace();
        ParsedStatementImpl<TestPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        
        // Build this set list, which corresponds to the TestPojo below
        // SET ?s = 'foo-bar' , 'fooTimeStamp' = ?l
        SetList setList = new SetList();
        SetListValue writerId = new SetListValue();
        TerminalNode writerKey = new TerminalNode(null);
        UnfinishedValueNode unfinishedWriterKey = new UnfinishedValueNode();
        unfinishedWriterKey.setParameterIndex(0);
        unfinishedWriterKey.setType(String.class);
        unfinishedWriterKey.setLHS(true);
        writerKey.setValue(unfinishedWriterKey);
        writerId.setKey(writerKey);
        TerminalNode writerValue = new TerminalNode(null);
        writerValue.setValue("foo-bar");
        writerId.setValue(writerValue);
        setList.addValue(writerId);
        SetListValue fooTimeStamp = new SetListValue();
        TerminalNode timeStampKey = new TerminalNode(null);
        timeStampKey.setValue(new Key<>("fooTimeStamp"));
        fooTimeStamp.setKey(timeStampKey);
        TerminalNode timeStampVal = new TerminalNode(null);
        UnfinishedValueNode timeStampUnfinished = new UnfinishedValueNode();
        timeStampUnfinished.setLHS(false);
        timeStampUnfinished.setParameterIndex(1);
        timeStampUnfinished.setType(long.class);
        timeStampVal.setValue(timeStampUnfinished);
        fooTimeStamp.setValue(timeStampVal);
        setList.addValue(fooTimeStamp);
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        
        // WHERE 'foo' = ?i
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode equals = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(equals);
        equals.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode fooPatch = new TerminalNode(equals);
        UnfinishedValueNode patch2 = new UnfinishedValueNode();
        patch2.setLHS(false);
        patch2.setType(int.class);
        patch2.setParameterIndex(2);
        fooPatch.setValue(patch2);
        TerminalNode foo = new TerminalNode(equals);
        foo.setValue(new Key<>("foo"));
        equals.setLeftChild(foo);
        equals.setRightChild(fooPatch);
        suffixExpn.setWhereExpn(where);
        
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(2, setList.getValues().size());
        parsedStmt.setSetList(setList);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(3);
        preparedStatement.setString(0, "writerId");
        preparedStatement.setLong(1, Long.MAX_VALUE);
        preparedStatement.setInt(2, -400);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Replace<TestPojo> replace = (Replace<TestPojo>)parsedStmt.patchStatement(params);
        assertTrue(replace instanceof TestReplace);
        TestReplace q = (TestReplace)replace;
        Map<String, Object> vals = q.values;
        assertEquals(2, vals.keySet().size());
        assertEquals("foo-bar", vals.get("writerId"));
        assertEquals(Long.MAX_VALUE, vals.get("fooTimeStamp"));
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression expectedExpression = factory.equalTo(new Key<>("foo"), -400);
        assertEquals(expectedExpression, q.where);
    }
    
    /*
     * Test for patching of:
     *  "UPDATE something SET 'writerId' = ?s WHERE 'foo' = ?i"
     */
    @Test
    public void canPatchBasicSetListUpdate() throws IllegalPatchException {
        DataModifyingStatement<TestPojo> stmt = new TestUpdate();
        ParsedStatementImpl<TestPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        
        // Build this set list, which corresponds to the TestPojo below
        // SET 'writerId' = ?s
        SetList setList = new SetList();
        SetListValue writerId = new SetListValue();
        TerminalNode writerKey = new TerminalNode(null);
        writerKey.setValue(new Key<>("writerId"));
        TerminalNode writerValue = new TerminalNode(null);
        UnfinishedValueNode unfinishedWriterValue = new UnfinishedValueNode();
        unfinishedWriterValue.setParameterIndex(0);
        unfinishedWriterValue.setType(String.class);
        unfinishedWriterValue.setLHS(false);
        writerValue.setValue(unfinishedWriterValue);
        writerId.setKey(writerKey);
        writerId.setValue(writerValue);
        setList.addValue(writerId);
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        
        // WHERE 'foo' = ?i
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode equals = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(equals);
        equals.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode fooPatch = new TerminalNode(equals);
        UnfinishedValueNode patch2 = new UnfinishedValueNode();
        patch2.setLHS(false);
        patch2.setType(int.class);
        patch2.setParameterIndex(1);
        fooPatch.setValue(patch2);
        TerminalNode foo = new TerminalNode(equals);
        foo.setValue(new Key<>("foo"));
        equals.setLeftChild(foo);
        equals.setRightChild(fooPatch);
        suffixExpn.setWhereExpn(where);
        
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(1, setList.getValues().size());
        parsedStmt.setSetList(setList);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "foobar-writer-id");
        preparedStatement.setInt(1, -400);
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Update<TestPojo> replace = (Update<TestPojo>)parsedStmt.patchStatement(params);
        assertTrue(replace instanceof TestUpdate);
        TestUpdate q = (TestUpdate)replace;
        List<Pair<Object, Object>> updates = q.updates;
        assertEquals(1, updates.size());
        Pair<Object, Object> update = updates.get(0);
        assertEquals("writerId", update.getFirst());
        assertEquals("foobar-writer-id", update.getSecond());
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression expectedExpression = factory.equalTo(new Key<>("foo"), -400);
        assertEquals(expectedExpression, q.where);
    }
    
    @Test
    public void canPatchBasicSort() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        parsedStmt.setSetList(new SetList());
        SuffixExpression suffixExpn = new SuffixExpression();
        // SORT ? ASC, b DSC
        SortExpression sortExpn = new SortExpression();
        SortMember member = new SortMember();
        member.setDirection(SortDirection.ASCENDING);
        UnfinishedSortKey unfinished = new UnfinishedSortKey();
        unfinished.setParameterIndex(0);
        member.setSortKey(unfinished);
        SortMember member2 = new SortMember();
        member2.setDirection(SortDirection.DESCENDING);
        member2.setSortKey("b");
        sortExpn.addMember(member);
        sortExpn.addMember(member2);
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(sortExpn);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        // set the value for the one unfinished param
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(1);
        preparedStatement.setString(0, "a");
        PreparedParameter[] params = preparedStatement.getParams();
        // finally test the patching
        Query<Pojo> query = (Query<Pojo>)parsedStmt.patchStatement(params);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        List<Pair<Key<?>, SortDirection>> actualSorts = q.sorts;
        assertEquals(2, actualSorts.size());
        Pair<Key<?>, SortDirection> first = actualSorts.get(0);
        Key<?> firstKeyActual = (Key<?>)first.getFirst();
        Key<?> expectedFirst = new Key<>("a");
        assertEquals(expectedFirst, firstKeyActual);
        assertEquals(SortDirection.ASCENDING, first.getSecond());
        Pair<Key<?>, SortDirection> second = actualSorts.get(1);
        Key<?> secondKeyActual = (Key<?>)second.getFirst();
        Key<?> expectedSecond = new Key<>("b");
        assertEquals(expectedSecond, secondKeyActual);
        assertEquals(SortDirection.DESCENDING, second.getSecond());
    }
    
    @Test
    public void failPatchSetListAddWrongType() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        DataModifyingStatement<TestPojo> stmt = new TestAdd<>();
        ParsedStatementImpl<TestPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        SetList setList = buildSetList();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(2, setList.getValues().size());
        parsedStmt.setSetList(setList);
        // set the value for the one unfinished param
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setLong(0, -1);
        preparedStatement.setString(1, "foobar");
        PreparedParameter[] params = preparedStatement.getParams();
        // this should fail since types don't match
        try {
            parsedStmt.patchStatement(params);
            fail("Should have failed to patch, due to type mismatch");
        } catch (IllegalPatchException e) {
            assertTrue(e.getMessage().contains("Expected " + String.class.getName()));
            // pass
        }
    }
    
    @Test
    public void failPatchSetListAddInsufficientParams() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        DataModifyingStatement<TestPojo> stmt = new TestAdd<>();
        ParsedStatementImpl<TestPojo> parsedStmt = new ParsedStatementImpl<>(stmt);
        SuffixExpression suffixExpn = new SuffixExpression();
        SetList setList = buildSetList();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        suffixExpn.setWhereExpn(null);
        parsedStmt.setSuffixExpression(suffixExpn);
        assertEquals(2, setList.getValues().size());
        parsedStmt.setSetList(setList);
        // set the value for the one unfinished param
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(0);
        PreparedParameter[] params = preparedStatement.getParams();
        // this should fail since types don't match
        try {
            parsedStmt.patchStatement(params);
            fail("Should have failed to patch, due to type mismatch");
        } catch (IllegalPatchException e) {
            // pass
        }
    }
    
    @Test
    public void failPatchWithWrongType() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        parsedStmt.setSetList(new SetList());
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        // WHERE 'a' = ?s AND 'c' = ?i
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode leftEqual = new BinaryExpressionNode(and);
        BinaryExpressionNode rightEqual = new BinaryExpressionNode(and);
        leftEqual.setOperator(BinaryComparisonOperator.EQUALS);
        rightEqual.setOperator(BinaryComparisonOperator.EQUALS);
        and.setLeftChild(leftEqual);
        and.setRightChild(rightEqual);
        TerminalNode a = new TerminalNode(leftEqual);
        Key<?> aKey = new Key<>("a");
        a.setValue(aKey);
        TerminalNode b = new TerminalNode(leftEqual);
        UnfinishedValueNode patchB = new UnfinishedValueNode();
        patchB.setType(String.class);
        patchB.setParameterIndex(0);
        b.setValue(patchB);
        leftEqual.setLeftChild(a);
        leftEqual.setRightChild(b);
        TerminalNode c = new TerminalNode(rightEqual);
        Key<?> cKey = new Key<>("c");
        c.setValue(cKey);
        rightEqual.setLeftChild(c);
        TerminalNode d = new TerminalNode(rightEqual);
        UnfinishedValueNode dPatch = new UnfinishedValueNode();
        dPatch.setParameterIndex(1);
        dPatch.setType(Integer.class);
        d.setValue(dPatch);
        rightEqual.setRightChild(d);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        // next, create the PreparedStatement we are going to use for
        // patching.
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(2);
        preparedStatement.setString(0, "test1");
        preparedStatement.setString(1, "foo");
        // finally test the patching
        try {
            PreparedParameter[] params = preparedStatement.getParams();
            parsedStmt.patchStatement(params);
            fail("should have failed to patch param 1 with a string value (expected int)");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch"));
        }
    }
    
    @Test
    public void failPatchBasicEqualsIfIndexOutofBounds() {
        // create the parsedStatementImpl we are going to use
        ParsedStatementImpl<Pojo> parsedStmt = new ParsedStatementImpl<>(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        parsedStmt.setSetList(new SetList());
        // WHERE a = ?
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(and);
        a.setValue("a");
        TerminalNode b = new TerminalNode(and);
        UnfinishedValueNode bPatch = new UnfinishedValueNode();
        bPatch.setParameterIndex(1); // out of bounds
        b.setValue(bPatch);
        and.setLeftChild(a);
        and.setRightChild(b);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        PreparedStatementImpl<Pojo> preparedStatement = new PreparedStatementImpl<>(1);
        preparedStatement.setString(0, "b");
        try {
            PreparedParameter[] params = preparedStatement.getParams();
            // this should fail
            parsedStmt.patchStatement(params);
            fail("should not reach here");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }
    
    private static class TestQuery implements Query<Pojo> {

        private Expression expr;
        private List<Pair<Key<?>, SortDirection>> sorts;
        private int limitVal = -1;
        
        private TestQuery() {
            sorts = new ArrayList<>();
        }
        
        @Override
        public void where(Expression expr) {
            this.expr = expr;
        }

        @Override
        public void sort(Key<?> key, SortDirection direction) {
            Pair<Key<?>, SortDirection> sortPair = new Pair<Key<?>, SortDirection>(key, direction);
            sorts.add(sortPair);
        }

        @Override
        public void limit(int n) {
            this.limitVal = n;
        }

        @Override
        public Cursor<Pojo> execute() {
            // Not implemented
            throw new AssertionError();
        }

        @Override
        public Expression getWhereExpression() {
            // Not implemented
            throw new AssertionError();
        }

        @Override
        public Statement<Pojo> getRawDuplicate() {
            return new TestQuery();
        }
        
    }
    
    private static class TestAdd<T extends Pojo> implements Add<T> {
        
        private Map<String, Object> values = new HashMap<>();

        @Override
        public void set(String key, Object value) {
            values.put(key, value);
        }

        @Override
        public int apply() {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new TestAdd<>();
        }
        
    }
    
    private static class TestReplace implements Replace<TestPojo> {

        private Expression where;
        private Map<String, Object> values = new HashMap<>();
        
        @Override
        public void set(String key, Object value) {
            values.put(key, value);
        }

        @Override
        public void where(Expression expression) {
            where = expression;
        }

        @Override
        public int apply() {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public Statement<TestPojo> getRawDuplicate() {
            return new TestReplace();
        }
        
    }
    
    private static class TestUpdate implements Update<TestPojo> {

        private Expression where;
        private List<Pair<Object, Object>> updates = new ArrayList<>();
        
        @Override
        public void where(Expression expr) {
            this.where = expr;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void set(String key, Object value) {
            Pair update = new Pair<>(key, value);
            updates.add(update);
        }

        @Override
        public int apply() {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public Statement<TestPojo> getRawDuplicate() {
            return new TestUpdate();
        }
        
    }
    
    public static class TestPojo implements Pojo {
        
        private String writerId;
        private long fooTimeStamp;
        
        public String getWriterId() {
            return writerId;
        }
        public void setWriterId(String writerId) {
            this.writerId = writerId;
        }
        public long getFooTimeStamp() {
            return fooTimeStamp;
        }
        public void setFooTimeStamp(long fooTimeStamp) {
            this.fooTimeStamp = fooTimeStamp;
        }
        
    }
    
    public static class FancyPojo extends TestPojo {
        
        private TestPojo[] someList;

        public TestPojo[] getSomeList() {
            return someList;
        }

        public void setSomeList(TestPojo[] someList) {
            this.someList = someList;
        }
        
        
    }
}
