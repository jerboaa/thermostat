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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.internal.statement.BinaryExpressionNode;
import com.redhat.thermostat.storage.internal.statement.LimitExpression;
import com.redhat.thermostat.storage.internal.statement.NotBooleanExpressionNode;
import com.redhat.thermostat.storage.internal.statement.ParsedStatement;
import com.redhat.thermostat.storage.internal.statement.SortExpression;
import com.redhat.thermostat.storage.internal.statement.SortMember;
import com.redhat.thermostat.storage.internal.statement.StatementDescriptorParser;
import com.redhat.thermostat.storage.internal.statement.SuffixExpression;
import com.redhat.thermostat.storage.internal.statement.TerminalNode;
import com.redhat.thermostat.storage.internal.statement.UnfinishedLimitValue;
import com.redhat.thermostat.storage.internal.statement.UnfinishedSortKey;
import com.redhat.thermostat.storage.internal.statement.UnfinishedValueNode;
import com.redhat.thermostat.storage.internal.statement.WhereExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;

public class StatementDescriptorParserTest {

    private Storage storage;
    @SuppressWarnings("rawtypes")
    private Query mockQuery;
    private StatementDescriptorParser parser;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        storage = mock(Storage.class);
        mockQuery = mock(Query.class);
        when(storage.createQuery(any(AgentInfoDAO.CATEGORY.getClass()))).thenReturn(mockQuery);
    }
    
    @After
    public void teardown() {
        storage = null;
        mockQuery = null;
    }
    
    @Test
    public void testParseQuerySimple() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName();
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(0, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression tree = statement.getSuffixExpression();
        assertNull(tree.getLimitExpn());
        assertNull(tree.getSortExpn());
        assertNull(tree.getWhereExpn());
    }
    
    @Test
    public void testParseQuerySimpleWithLimit() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " LIMIT ?i";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(1, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression expn = statement.getSuffixExpression();
        assertNotNull(expn.getLimitExpn());
        assertNull(expn.getSortExpn());
        assertNull(expn.getWhereExpn());
        LimitExpression limitExp = expn.getLimitExpn();
        assertTrue(limitExp.getValue() instanceof UnfinishedLimitValue);
        UnfinishedLimitValue value = (UnfinishedLimitValue)limitExp.getValue();
        assertEquals(0, value.getParameterIndex());
    }
    
    @Test
    public void testParseSortMultiple() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " SORT 'a' ASC , 'b' DSC , 'c' ASC";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(0, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        SortExpression sortExpn = suffixExpn.getSortExpn();
        assertNotNull(sortExpn);
        assertNull(suffixExpn.getLimitExpn());
        assertNull(suffixExpn.getWhereExpn());
        List<SortMember> list = sortExpn.getMembers();
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(SortDirection.ASCENDING, list.get(0).getDirection());
        assertEquals(SortDirection.DESCENDING, list.get(1).getDirection());
        assertEquals(SortDirection.ASCENDING, list.get(2).getDirection());
        assertEquals("a", list.get(0).getSortKey());
        assertEquals("b", list.get(1).getSortKey());
        assertEquals("c", list.get(2).getSortKey());
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testParseQueryWhereAndSortMultiple() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' < 'b' AND 'c' = ?s OR NOT 'x' >= ?i SORT 'a' ASC , 'b' DSC , 'c' ASC";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(2, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNotNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        List<SortMember> list = suffixExpn.getSortExpn().getMembers();
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(SortDirection.ASCENDING, list.get(0).getDirection());
        assertEquals(SortDirection.DESCENDING, list.get(1).getDirection());
        assertEquals(SortDirection.ASCENDING, list.get(2).getDirection());
        assertEquals("a", list.get(0).getSortKey());
        assertEquals("b", list.get(1).getSortKey());
        assertEquals("c", list.get(2).getSortKey());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode or = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(or);
        or.setOperator(BinaryLogicalOperator.OR);
        BinaryExpressionNode and = new BinaryExpressionNode(or);
        and.setOperator(BinaryLogicalOperator.AND);
        or.setLeftChild(and);
        NotBooleanExpressionNode not = new NotBooleanExpressionNode(or);
        or.setRightChild(not);
        BinaryExpressionNode unequality = new BinaryExpressionNode(and);
        unequality.setOperator(BinaryComparisonOperator.LESS_THAN);
        TerminalNode a = new TerminalNode(unequality);
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        unequality.setLeftChild(a);
        TerminalNode b = new TerminalNode(unequality);
        b.setValue("b");
        unequality.setRightChild(b);
        and.setLeftChild(unequality);
        BinaryExpressionNode equality = new BinaryExpressionNode(and);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode c = new TerminalNode(equality);
        Key cKey = new Key("c", false);
        c.setValue(cKey);
        equality.setLeftChild(c);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(0);
        patch1.setLHS(false);
        patch1.setType(String.class);
        TerminalNode d = new TerminalNode(equality);
        d.setValue(patch1);
        equality.setRightChild(d);
        and.setRightChild(equality);
        BinaryExpressionNode greaterEqual = new BinaryExpressionNode(not);
        not.setValue(greaterEqual);
        greaterEqual.setOperator(BinaryComparisonOperator.GREATER_THAN_OR_EQUAL_TO);
        TerminalNode x = new TerminalNode(greaterEqual);
        Key xKey = new Key("x", false);
        x.setValue(xKey);
        greaterEqual.setLeftChild(x);
        UnfinishedValueNode patch2 = new UnfinishedValueNode();
        patch2.setParameterIndex(1);
        patch2.setLHS(false);
        patch2.setType(Integer.class);
        TerminalNode y = new TerminalNode(greaterEqual);
        y.setValue(patch2);
        greaterEqual.setRightChild(y);
        // finally assert equality
        assertTrue( WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testParseQueryWhereOrSortMultiple() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' < 'b' OR 'c' = ?s SORT 'a' ASC , ?s DSC , 'c' ASC";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(2, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNotNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        SortExpression sortExp = suffixExpn.getSortExpn();
        List<SortMember> list = sortExp.getMembers();
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(SortDirection.ASCENDING, list.get(0).getDirection());
        assertEquals(SortDirection.DESCENDING, list.get(1).getDirection());
        assertEquals(SortDirection.ASCENDING, list.get(2).getDirection());
        assertEquals("a", list.get(0).getSortKey());
        UnfinishedSortKey unfinished = new UnfinishedSortKey();
        unfinished.setParameterIndex(1);
        assertEquals(unfinished, list.get(1).getSortKey());
        assertEquals("c", list.get(2).getSortKey());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode or = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(or);
        or.setOperator(BinaryLogicalOperator.OR);
        BinaryExpressionNode unequality = new BinaryExpressionNode(or);
        unequality.setOperator(BinaryComparisonOperator.LESS_THAN);
        TerminalNode a = new TerminalNode(unequality);
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        unequality.setLeftChild(a);
        TerminalNode b = new TerminalNode(unequality);
        b.setValue("b");
        unequality.setRightChild(b);
        or.setLeftChild(unequality);
        BinaryExpressionNode equality = new BinaryExpressionNode(or);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode c = new TerminalNode(equality);
        Key cKey = new Key("c", false);
        c.setValue(cKey);
        equality.setLeftChild(c);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(0);
        patch1.setLHS(false);
        patch1.setType(String.class);
        TerminalNode d = new TerminalNode(equality);
        d.setValue(patch1);
        equality.setRightChild(d);
        or.setRightChild(equality);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }
    
    @Test
    public void testParseQuerySimpleWhereAndSimpleSort() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' < 'b' SORT 'a' DSC";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = (ParsedStatement)parser.parse();
        assertEquals(0, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNotNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        SortExpression sortExp = suffixExpn.getSortExpn();
        List<SortMember> list = sortExp.getMembers();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(SortDirection.DESCENDING, list.get(0).getDirection());
        assertEquals("a", list.get(0).getSortKey());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode unequality = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(unequality);
        unequality.setOperator(BinaryComparisonOperator.LESS_THAN);
        TerminalNode a = new TerminalNode(unequality);
        @SuppressWarnings("rawtypes")
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        unequality.setLeftChild(a);
        TerminalNode b = new TerminalNode(unequality);
        b.setValue("b");
        unequality.setRightChild(b);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }
    
    @Test
    public void testParseQuerySimpleWithOneWhere() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            fail(e.getMessage());
        }
        assertEquals(1, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode equality = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(equality);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(equality);
        @SuppressWarnings("rawtypes")
        Key aKey = new Key(Key.AGENT_ID.getName(), false);
        a.setValue(aKey);
        equality.setLeftChild(a);
        TerminalNode b = new TerminalNode(equality);
        UnfinishedValueNode node = new UnfinishedValueNode();
        node.setParameterIndex(0);
        node.setLHS(false);
        node.setType(String.class);
        b.setValue(node);
        equality.setRightChild(b);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testParseSimpleWithAndOr() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE '" + Key.AGENT_ID.getName() + "' = ?s" +
                            " AND ?s < ?b OR 'a' = 'b'";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(3, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode or = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(or);
        or.setOperator(BinaryLogicalOperator.OR);
        BinaryExpressionNode and = new BinaryExpressionNode(or);
        and.setOperator(BinaryLogicalOperator.AND);
        or.setLeftChild(and);
        BinaryExpressionNode equality = new BinaryExpressionNode(and);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(equality);
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        equality.setLeftChild(a);
        TerminalNode b = new TerminalNode(equality);
        b.setValue("b");
        equality.setRightChild(b);
        or.setRightChild(equality);
        BinaryExpressionNode equality2 = new BinaryExpressionNode(and);
        equality2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode c = new TerminalNode(equality2);
        Key cKey = new Key(Key.AGENT_ID.getName(), false);
        c.setValue(cKey);
        equality2.setLeftChild(c);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(0);
        patch1.setLHS(false);
        patch1.setType(String.class);
        TerminalNode d = new TerminalNode(equality2);
        d.setValue(patch1);
        equality2.setRightChild(d);
        and.setLeftChild(equality2);
        BinaryExpressionNode lessThan = new BinaryExpressionNode(and);
        lessThan.setOperator(BinaryComparisonOperator.LESS_THAN);
        UnfinishedValueNode patch = new UnfinishedValueNode();
        patch.setParameterIndex(1);
        patch.setType(String.class);
        patch.setLHS(true);
        TerminalNode x = new TerminalNode(lessThan);
        x.setValue(patch);
        lessThan.setLeftChild(x);
        UnfinishedValueNode patch2 = new UnfinishedValueNode();
        patch2.setLHS(false);
        patch2.setParameterIndex(2);
        patch2.setType(boolean.class);
        TerminalNode y = new TerminalNode(lessThan);
        y.setValue(patch2);
        lessThan.setRightChild(y);
        and.setRightChild(lessThan);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }

    @Test
    public void testParseSimpleWithAnd() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' = ?s AND ?s = 'd'";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(2, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        // build the expected expression tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(and);
        and.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode equality = new BinaryExpressionNode(and);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(equality);
        @SuppressWarnings("rawtypes")
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        equality.setLeftChild(a);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setParameterIndex(0);
        unfinished.setLHS(false);
        unfinished.setType(String.class);
        TerminalNode b = new TerminalNode(equality);
        b.setValue(unfinished);
        equality.setRightChild(b);
        and.setLeftChild(equality);
        BinaryExpressionNode equality2 = new BinaryExpressionNode(and);
        equality2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode c = new TerminalNode(equality2);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(1);
        patch1.setType(String.class);
        patch1.setLHS(true);
        c.setValue(patch1);
        equality2.setLeftChild(c);
        TerminalNode d = new TerminalNode(equality2);
        d.setValue("d");
        equality2.setRightChild(d);
        and.setRightChild(equality2);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }

    @Test
    public void testParseSimpleWithNotOR() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE NOT 'a' = ?s OR ?s = 'd'";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(2, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        // build the expected parse tree
        WhereExpression expn = new WhereExpression();
        BinaryExpressionNode or = new BinaryExpressionNode(expn.getRoot());
        expn.getRoot().setValue(or);
        or.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode = new NotBooleanExpressionNode(or);
        BinaryExpressionNode comparison = new BinaryExpressionNode(notNode);
        notNode.setValue(comparison);
        or.setLeftChild(notNode);
        comparison.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode rightCompTerm = new TerminalNode(comparison);
        TerminalNode leftCompTerm = new TerminalNode(comparison);
        @SuppressWarnings("rawtypes")
        Key aKey = new Key("a", false);
        leftCompTerm.setValue(aKey);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(0);
        patch1.setType(String.class);
        patch1.setLHS(false);
        rightCompTerm.setValue(patch1);
        comparison.setLeftChild(leftCompTerm);
        comparison.setRightChild(rightCompTerm);
        BinaryExpressionNode otherComparison = new BinaryExpressionNode(or);
        otherComparison.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode leftUnfinished = new TerminalNode(otherComparison);
        UnfinishedValueNode patch2 = new UnfinishedValueNode();
        patch2.setParameterIndex(1);
        patch2.setLHS(true);
        patch2.setType(String.class);
        leftUnfinished.setValue(patch2);
        TerminalNode other = new TerminalNode(otherComparison);
        other.setValue("d");
        otherComparison.setLeftChild(leftUnfinished);
        otherComparison.setRightChild(other);
        or.setRightChild(otherComparison);
        assertTrue(WhereExpressions.equals(expn, suffixExpn.getWhereExpn()));
    }

    @Test
    public void testParseSimpleWithOr() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' = ?s OR ?s = 'd'";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(2, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getLimitExpn());
        assertNotNull(suffixExpn.getWhereExpn());
        // build the expected parse tree
        WhereExpression where = new WhereExpression();
        BinaryExpressionNode and = new BinaryExpressionNode(where.getRoot());
        where.getRoot().setValue(and);
        and.setOperator(BinaryLogicalOperator.OR);
        BinaryExpressionNode equality = new BinaryExpressionNode(and);
        equality.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode a = new TerminalNode(equality);
        @SuppressWarnings("rawtypes")
        Key aKey = new Key("a", false);
        a.setValue(aKey);
        equality.setLeftChild(a);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setParameterIndex(0);
        unfinished.setType(String.class);
        unfinished.setLHS(false);
        TerminalNode b = new TerminalNode(equality);
        b.setValue(unfinished);
        equality.setRightChild(b);
        and.setLeftChild(equality);
        BinaryExpressionNode equality2 = new BinaryExpressionNode(and);
        equality2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode c = new TerminalNode(equality2);
        UnfinishedValueNode patch1 = new UnfinishedValueNode();
        patch1.setParameterIndex(1);
        patch1.setType(String.class);
        patch1.setLHS(true);
        c.setValue(patch1);
        equality2.setLeftChild(c);
        TerminalNode d = new TerminalNode(equality2);
        d.setValue("d");
        equality2.setRightChild(d);
        and.setRightChild(equality2);
        assertTrue(WhereExpressions.equals(where, suffixExpn.getWhereExpn()));
    }
    
    @Test
    public void testParseSimpleWithLimit() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " LIMIT 1";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        ParsedStatement statement = null; 
        try {
            statement = (ParsedStatement)parser.parse();
        } catch (DescriptorParsingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(0, statement.getNumParams());
        assertEquals(mockQuery.getClass().getName(), statement.getRawStatement().getClass().getName());
        SuffixExpression suffixExpn = statement.getSuffixExpression();
        assertNull(suffixExpn.getSortExpn());
        assertNull(suffixExpn.getWhereExpn());
        assertNotNull(suffixExpn.getLimitExpn());
        assertEquals(1, suffixExpn.getLimitExpn().getValue());
    }
    
    @Test
    public void rejectLimitWhichIsNotInt() {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " LIMIT illegal";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            assertEquals("Invalid limit expression. 'illegal' not an integer", e.getMessage());
        }
    }

    @Test
    public void rejectLHSnotString() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE a < 1";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Expected string value. Got term ->a<-"));
        }
    }
    
    @Test
    public void rejectIllegalFreeParamType() throws DescriptorParsingException {
        // ? should be one of '?i', '?s', '?b', '?s['
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE ? < 1";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
            assertEquals("Unknown type of free parameter: '?'", e.getMessage());
        }
    }
    
    @Test
    public void rejectParseQueryWhereAndSortMultipleIllegalSortModifier() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'somekey' < 2 AND 'c' = ?s OR 'a' >= ?i SORT 'a' ASC , 'b' ILLEGAL , 'c' ASC";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Expected ASC or DSC"));
        }
    }

    @Test
    public void rejectParseQueryWhereBoolTerm() throws DescriptorParsingException {
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE true AND false";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
        }
    }

    @Test
    public void rejectSimpleQueryWithMissingSpaces() throws DescriptorParsingException {
        // we require a space before every operator/keyword
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY + " WHERE " + "'a'='b'";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
        }
    }
    
    @Test
    public void rejectSimpleQueryWithMissingSpaces2() throws DescriptorParsingException {
        // we require a space before every operator/keyword
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY + " WHERE " + "'a' ='b'";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
        }
    }
    
    @Test
    public void rejectSimpleQueryWithInvalidComparison() throws DescriptorParsingException {
        // <> is illegal
        String descrString = "QUERY " + AgentInfoDAO.CATEGORY + " WHERE " + "'a' <> 'b'";
        StatementDescriptor desc = getDescriptor(descrString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
        } catch (DescriptorParsingException e) {
            // pass
        }
    }
    
    @Test
    public void rejectInvalidDescriptorStringBadWhere() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " where '" + Key.AGENT_ID.getName() + "'= ?s";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
            fail("lower case where not allowed in descriptor. Should have rejected.");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Unexpected token: 'where'"));
        }
    }
    
    @Test
    public void rejectInvalidDescriptorStringBadStatementType() throws DescriptorParsingException {
        String descString = "UNKNOWN some-unknown-category WHERE 1 = ?i";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
            fail("UNKNOWN not a valid statement type");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Unknown statement type"));
        }
    }
    
    @Test
    public void rejectInvalidDescriptorStringCategoryMismatch() throws DescriptorParsingException {
        String descString = "QUERY some-unknown-category WHERE 1 = ?i";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
            fail("category names in descriptor and Category object did not match!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Category mismatch"));
        }
    }
    
    @Test
    public void rejectInvalidDescriptorStringBadSortNoArg() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE 'a' = ?i SORT";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
            fail("category names in descriptor and Category object did not match!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("Invalid where clause"));
        }
    }
    
    @Test
    public void rejectInvalidDescriptorStringBadWhereNoArg() throws DescriptorParsingException {
        String descString = "QUERY " + AgentInfoDAO.CATEGORY.getName() + " WHERE SORT";
        StatementDescriptor desc = getDescriptor(descString, AgentInfoDAO.CATEGORY);
        parser = new StatementDescriptorParser(storage, desc);
        try {
            parser.parse();
            fail("category names in descriptor and Category object did not match!");
        } catch (DescriptorParsingException e) {
            // pass
            assertTrue(e.getMessage().contains("SORT"));
            assertTrue(e.getMessage().contains("Expected string value"));
        }
    }
    
    private StatementDescriptor getDescriptor(final String desc, final Category<?> category) {
        return new StatementDescriptor() {
            
            @Override
            public String getQueryDescriptor() {
                return desc;
            }
            
            @Override
            public Category<?> getCategory() {
                return category;
            }
        };
    }
}