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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class ParsedStatementTest {

    private Query<?> statement;
    
    @Before
    public void setup() {
        statement = new TestQuery();
    }
    
    @After
    public void tearDown() {
        statement = null;
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void canPatchWhereAndExpr() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
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
        Key aKey = new Key("a", false);
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
        dPatch.setType(Integer.class);
        d.setValue(dPatch);
        rightEqual.setRightChild(d);
        suffixExpn.setWhereExpn(expn);
        parsedStmt.setNumFreeParams(1);
        parsedStmt.setSuffixExpression(suffixExpn);
        // next, create the PreparedStatement we are going to use for
        // patching.
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(2);
        preparedStatement.setString(0, "test1");
        preparedStatement.setInt(1, 2);
        // finally test the patching
        Query<?> query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryLogicalExpression);
        BinaryLogicalExpression andFinal = (BinaryLogicalExpression)expectedExpression;
        assertEquals(BinaryLogicalOperator.AND, andFinal.getOperator());
        assertTrue(andFinal.getLeftOperand() instanceof BinaryComparisonExpression);
        assertTrue(andFinal.getRightOperand() instanceof BinaryComparisonExpression);
        BinaryComparisonExpression left = (BinaryComparisonExpression)andFinal.getLeftOperand();
        BinaryComparisonExpression right = (BinaryComparisonExpression)andFinal.getRightOperand();
        assertEquals(BinaryComparisonOperator.EQUALS, left.getOperator());
        assertEquals(BinaryComparisonOperator.EQUALS, right.getOperator());
        assertTrue(left.getLeftOperand() instanceof LiteralExpression);
        assertTrue(left.getRightOperand() instanceof LiteralExpression);
        LiteralExpression leftLiteral1 = (LiteralExpression)left.getLeftOperand();
        LiteralExpression rightLiteral1 = (LiteralExpression)left.getRightOperand();
        assertEquals(aKey, leftLiteral1.getValue());
        assertEquals("test1", rightLiteral1.getValue());
        LiteralExpression leftLiteral2 = (LiteralExpression)right.getLeftOperand();
        LiteralExpression rightLiteral2 = (LiteralExpression)right.getRightOperand();
        assertEquals("c", leftLiteral2.getValue());
        // right literal value should have been patched to a "d"
        assertEquals(2, rightLiteral2.getValue());
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void canPatchBasicWhereEquals() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
        // WHERE a = ?
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(and);
        and.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(and);
        a.setValue(new Key("a", false));
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(1);
        preparedStatement.setBoolean(0, true);
        // finally test the patching
        Query<?> query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        BinaryComparisonExpression root = (BinaryComparisonExpression)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        LiteralExpression leftLiteral1 = (LiteralExpression)root.getLeftOperand();
        LiteralExpression rightLiteral1 = (LiteralExpression)root.getRightOperand();
        assertEquals(new Key("a", false), leftLiteral1.getValue());
        // this should have gotten patched to a "b"
        assertEquals(true, rightLiteral1.getValue());
        // now do it again with a different value
        preparedStatement = new PreparedStatementImpl(1);
        preparedStatement.setBoolean(0, false);
        query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        q = (TestQuery)query;
        expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        root = (BinaryComparisonExpression)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        leftLiteral1 = (LiteralExpression)root.getLeftOperand();
        rightLiteral1 = (LiteralExpression)root.getRightOperand();
        assertEquals(new Key("a", false), leftLiteral1.getValue());
        assertEquals(false, rightLiteral1.getValue());
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void canPatchBasicWhereEqualsLHSKeyAndRHSValue() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(2);
        preparedStatement.setString(0, "a");
        preparedStatement.setBoolean(1, true);
        // finally test the patching
        Query<?> query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        Expression expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        BinaryComparisonExpression root = (BinaryComparisonExpression)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        LiteralExpression leftLiteral1 = (LiteralExpression)root.getLeftOperand();
        LiteralExpression rightLiteral1 = (LiteralExpression)root.getRightOperand();
        assertEquals(new Key("a", false), leftLiteral1.getValue());
        // this should have gotten patched to a "b"
        assertEquals(true, rightLiteral1.getValue());
        // now do it again with a different value
        preparedStatement = new PreparedStatementImpl(2);
        preparedStatement.setString(0, "a");
        preparedStatement.setBoolean(1, false);
        query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        q = (TestQuery)query;
        expectedExpression = q.expr;
        assertTrue(expectedExpression instanceof BinaryComparisonExpression);
        root = (BinaryComparisonExpression)expectedExpression;
        assertEquals(BinaryComparisonOperator.EQUALS, root.getOperator());
        assertTrue(root.getLeftOperand() instanceof LiteralExpression);
        assertTrue(root.getRightOperand() instanceof LiteralExpression);
        leftLiteral1 = (LiteralExpression)root.getLeftOperand();
        rightLiteral1 = (LiteralExpression)root.getRightOperand();
        assertEquals(new Key("a", false), leftLiteral1.getValue());
        assertEquals(false, rightLiteral1.getValue());
    }
    
    @Test
    public void canPatchBasicLimit() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(1);
        preparedStatement.setInt(0, 3);
        @SuppressWarnings("rawtypes")
        Query query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        assertEquals(3, q.limitVal);
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void canPatchBasicSort() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(1);
        preparedStatement.setString(0, "a");
        Query query = (Query)parsedStmt.patchQuery(preparedStatement);
        assertTrue(query instanceof TestQuery);
        TestQuery q = (TestQuery)query;
        List<Pair<Key, SortDirection>> actualSorts = q.sorts;
        assertEquals(2, actualSorts.size());
        Pair first = actualSorts.get(0);
        Key firstKeyActual = (Key)first.getFirst();
        Key expectedFirst = new Key("a", false);
        assertEquals(expectedFirst, firstKeyActual);
        assertEquals(SortDirection.ASCENDING, first.getSecond());
        Pair second = actualSorts.get(1);
        Key secondKeyActual = (Key)second.getFirst();
        Key expectedSecond = new Key("b", false);
        assertEquals(expectedSecond, secondKeyActual);
        assertEquals(SortDirection.DESCENDING, second.getSecond());
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void failPatchWithWrongType() throws IllegalPatchException {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
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
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        TerminalNode b = new TerminalNode(leftEqual);
        UnfinishedValueNode patchB = new UnfinishedValueNode();
        patchB.setType(String.class);
        patchB.setParameterIndex(0);
        b.setValue(patchB);
        leftEqual.setLeftChild(a);
        leftEqual.setRightChild(b);
        TerminalNode c = new TerminalNode(rightEqual);
        Key cKey = new Key("c", false);
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(2);
        preparedStatement.setString(0, "test1");
        preparedStatement.setString(1, "foo");
        // finally test the patching
        try {
            parsedStmt.patchQuery(preparedStatement);
            fail("should have failed to patch param 1 with a string value (expected int)");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch"));
        }
    }
    
    @Test
    public void failPatchBasicEqualsIfIndexOutofBounds() {
        // create the parsedStatementImpl we are going to use
        ParsedStatement parsedStmt = new ParsedStatement(statement);
        SuffixExpression suffixExpn = new SuffixExpression();
        suffixExpn.setLimitExpn(null);
        suffixExpn.setSortExpn(null);
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
        PreparedStatementImpl preparedStatement = new PreparedStatementImpl(1);
        preparedStatement.setString(0, "b");
        // this should fail
        try {
            parsedStmt.patchQuery(preparedStatement);
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }
    
    @SuppressWarnings("rawtypes")
    private static class TestQuery implements Query {

        private Expression expr;
        private List<Pair<Key, SortDirection>> sorts;
        private int limitVal = -1;
        
        private TestQuery() {
            sorts = new ArrayList<>();
        }
        
        @Override
        public void where(Expression expr) {
            this.expr = expr;
        }

        @Override
        public void sort(Key key, SortDirection direction) {
            Pair<Key, SortDirection> sortPair = new Pair<>(key, direction);
            sorts.add(sortPair);
        }

        @Override
        public void limit(int n) {
            this.limitVal = n;
        }

        @Override
        public Cursor execute() {
            // Not implemented
            return null;
        }
        
    }
}
