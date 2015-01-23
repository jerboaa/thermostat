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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.storage.internal.statement.BinaryExpressionNode;
import com.redhat.thermostat.storage.internal.statement.NotBooleanExpressionNode;
import com.redhat.thermostat.storage.internal.statement.TerminalNode;
import com.redhat.thermostat.storage.internal.statement.WhereExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;

public class WhereExpressionsTest {

    @Test
    public void testEqualsSelf() {
        WhereExpression expn1 = new WhereExpression();
        assertTrue(WhereExpressions.equals(expn1, expn1));
    }
    
    @Test
    public void testEqualsEmpty() {
        WhereExpression expn1 = new WhereExpression();
        WhereExpression expn2 = new WhereExpression();
        assertTrue(WhereExpressions.equals(expn1, expn2));
    }
    
    @Test
    public void testEqualsSimpleTerminal() {
        WhereExpression expn1 = new WhereExpression();
        WhereExpression expn2 = new WhereExpression();
        TerminalNode node = new TerminalNode(expn1.getRoot());
        node.setValue("testing");
        TerminalNode node2 = new TerminalNode(expn2.getRoot());
        node2.setValue("testing");
        expn1.getRoot().setValue(node);
        expn2.getRoot().setValue(node2);
        assertTrue(WhereExpressions.equals(expn1, expn2));
    }
    
    @Test
    public void testEqualsSimpleTerminalWithDifferentValues() {
        WhereExpression expn1 = new WhereExpression();
        WhereExpression expn2 = new WhereExpression();
        TerminalNode node = new TerminalNode(expn1.getRoot());
        node.setValue("test");
        TerminalNode node2 = new TerminalNode(expn2.getRoot());
        node2.setValue("other");
        expn1.getRoot().setValue(node);
        expn2.getRoot().setValue(node2);
        assertFalse(WhereExpressions.equals(expn1, expn2));
    }
    
    @Test
    public void testEqualsComplexish() {
        WhereExpression expn1 = new WhereExpression();
        BinaryExpressionNode binNode1 = new BinaryExpressionNode(expn1.getRoot());
        expn1.getRoot().setValue(binNode1);
        binNode1.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode1 = new NotBooleanExpressionNode(binNode1);
        TerminalNode termNode1 = new TerminalNode(notNode1);
        termNode1.setValue("testing");
        notNode1.setValue(termNode1);
        binNode1.setLeftChild(notNode1);
        BinaryExpressionNode and1 = new BinaryExpressionNode(binNode1);
        binNode1.setRightChild(and1);
        and1.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left1 = new BinaryExpressionNode(and1);
        left1.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode1Equal = new TerminalNode(left1);
        termNode1Equal.setValue("a");
        TerminalNode termNode1EqualOther = new TerminalNode(left1);
        termNode1EqualOther.setValue("b");
        left1.setLeftChild(termNode1Equal);
        left1.setRightChild(termNode1EqualOther);
        and1.setLeftChild(left1);
        BinaryExpressionNode right1 = new BinaryExpressionNode(and1);
        right1.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode1LessEqual = new TerminalNode(right1);
        termNode1LessEqual.setValue("x");
        TerminalNode termNode1LessEqualOther = new TerminalNode(right1);
        termNode1LessEqualOther.setValue("y");
        right1.setLeftChild(termNode1LessEqual);
        right1.setRightChild(termNode1LessEqualOther);
        and1.setRightChild(right1);
        
        // now build the second and equal where expn
        WhereExpression expn2 = new WhereExpression();
        BinaryExpressionNode binNode2 = new BinaryExpressionNode(expn2.getRoot());
        expn2.getRoot().setValue(binNode2);
        binNode2.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode2 = new NotBooleanExpressionNode(binNode2);
        TerminalNode termNode2 = new TerminalNode(notNode2);
        termNode2.setValue("testing");
        notNode2.setValue(termNode2);
        binNode2.setLeftChild(notNode2);
        BinaryExpressionNode and2 = new BinaryExpressionNode(binNode2);
        binNode2.setRightChild(and2);
        and2.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left2 = new BinaryExpressionNode(and2);
        left2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode2Equal = new TerminalNode(left2);
        termNode2Equal.setValue("a");
        TerminalNode termNode2EqualOther = new TerminalNode(left2);
        termNode2EqualOther.setValue("b");
        left2.setLeftChild(termNode2Equal);
        left2.setRightChild(termNode2EqualOther);
        and2.setLeftChild(left2);
        BinaryExpressionNode right2 = new BinaryExpressionNode(and2);
        right2.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode2LessEqual = new TerminalNode(right2);
        termNode2LessEqual.setValue("x");
        TerminalNode termNode2LessEqualOther = new TerminalNode(right2);
        termNode2LessEqualOther.setValue("y");
        right2.setLeftChild(termNode2LessEqual);
        right2.setRightChild(termNode2LessEqualOther);
        and2.setRightChild(right2);
        assertTrue(WhereExpressions.equals(expn1, expn2));
    }
    
    @Test
    public void testNotEqualsComplexish() {
        WhereExpression expn1 = new WhereExpression();
        BinaryExpressionNode binNode1 = new BinaryExpressionNode(expn1.getRoot());
        expn1.getRoot().setValue(binNode1);
        binNode1.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode1 = new NotBooleanExpressionNode(binNode1);
        TerminalNode termNode1 = new TerminalNode(notNode1);
        termNode1.setValue("testing");
        notNode1.setValue(termNode1);
        binNode1.setLeftChild(notNode1);
        BinaryExpressionNode and1 = new BinaryExpressionNode(binNode1);
        binNode1.setRightChild(and1);
        and1.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left1 = new BinaryExpressionNode(and1);
        left1.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode1Equal = new TerminalNode(left1);
        termNode1Equal.setValue("d"); // should be "a" to make it equal
        TerminalNode termNode1EqualOther = new TerminalNode(left1);
        termNode1EqualOther.setValue("b");
        left1.setLeftChild(termNode1Equal);
        left1.setRightChild(termNode1EqualOther);
        and1.setLeftChild(left1);
        BinaryExpressionNode right1 = new BinaryExpressionNode(and1);
        right1.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode1LessEqual = new TerminalNode(right1);
        termNode1LessEqual.setValue("x");
        TerminalNode termNode1LessEqualOther = new TerminalNode(right1);
        termNode1LessEqualOther.setValue("y");
        right1.setLeftChild(termNode1LessEqual);
        right1.setRightChild(termNode1LessEqualOther);
        and1.setRightChild(right1);
        
        // now build the second and equal where expn
        WhereExpression expn2 = new WhereExpression();
        BinaryExpressionNode binNode2 = new BinaryExpressionNode(expn2.getRoot());
        expn2.getRoot().setValue(binNode2);
        binNode2.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode2 = new NotBooleanExpressionNode(binNode2);
        TerminalNode termNode2 = new TerminalNode(notNode2);
        termNode2.setValue("testing");
        notNode2.setValue(termNode2);
        binNode2.setLeftChild(notNode2);
        BinaryExpressionNode and2 = new BinaryExpressionNode(binNode2);
        binNode2.setRightChild(and2);
        and2.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left2 = new BinaryExpressionNode(and2);
        left2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode2Equal = new TerminalNode(left2);
        termNode2Equal.setValue("a");
        TerminalNode termNode2EqualOther = new TerminalNode(left2);
        termNode2EqualOther.setValue("b");
        left2.setLeftChild(termNode2Equal);
        left2.setRightChild(termNode2EqualOther);
        and2.setLeftChild(left2);
        BinaryExpressionNode right2 = new BinaryExpressionNode(and2);
        right2.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode2LessEqual = new TerminalNode(right2);
        termNode2LessEqual.setValue("x");
        TerminalNode termNode2LessEqualOther = new TerminalNode(right2);
        termNode2LessEqualOther.setValue("y");
        right2.setLeftChild(termNode2LessEqual);
        right2.setRightChild(termNode2LessEqualOther);
        and2.setRightChild(right2);
        assertFalse(WhereExpressions.equals(expn1, expn2));
    }
    
    @Test
    public void testNotEqualsNotSameTreeAtAll() {
        WhereExpression expn1 = new WhereExpression();
        BinaryExpressionNode binNode1 = new BinaryExpressionNode(expn1.getRoot());
        expn1.getRoot().setValue(binNode1);
        binNode1.setOperator(BinaryLogicalOperator.OR);
        NotBooleanExpressionNode notNode1 = new NotBooleanExpressionNode(binNode1);
        TerminalNode termNode1 = new TerminalNode(notNode1);
        termNode1.setValue("testing");
        notNode1.setValue(termNode1);
        binNode1.setLeftChild(notNode1);
        BinaryExpressionNode and1 = new BinaryExpressionNode(binNode1);
        binNode1.setRightChild(and1);
        and1.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left1 = new BinaryExpressionNode(and1);
        left1.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode1Equal = new TerminalNode(left1);
        termNode1Equal.setValue("d"); // should be "a" to make it equal
        TerminalNode termNode1EqualOther = new TerminalNode(left1);
        termNode1EqualOther.setValue("b");
        left1.setLeftChild(termNode1Equal);
        left1.setRightChild(termNode1EqualOther);
        and1.setLeftChild(left1);
        BinaryExpressionNode right1 = new BinaryExpressionNode(and1);
        right1.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode1LessEqual = new TerminalNode(right1);
        termNode1LessEqual.setValue("x");
        TerminalNode termNode1LessEqualOther = new TerminalNode(right1);
        termNode1LessEqualOther.setValue("y");
        right1.setLeftChild(termNode1LessEqual);
        right1.setRightChild(termNode1LessEqualOther);
        and1.setRightChild(right1);
        
        // now build the second fairly different tree
        WhereExpression expn2 = new WhereExpression();
        NotBooleanExpressionNode notNode2 = new NotBooleanExpressionNode(expn2.getRoot());
        BinaryExpressionNode and2 = new BinaryExpressionNode(notNode2);
        notNode2.setValue(and2);
        and2.setOperator(BinaryLogicalOperator.AND);
        BinaryExpressionNode left2 = new BinaryExpressionNode(and2);
        left2.setOperator(BinaryComparisonOperator.EQUALS);
        TerminalNode termNode2Equal = new TerminalNode(left2);
        termNode2Equal.setValue("a");
        TerminalNode termNode2EqualOther = new TerminalNode(left2);
        termNode2EqualOther.setValue("b");
        left2.setLeftChild(termNode2Equal);
        left2.setRightChild(termNode2EqualOther);
        and2.setLeftChild(left2);
        BinaryExpressionNode right2 = new BinaryExpressionNode(and2);
        right2.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
        TerminalNode termNode2LessEqual = new TerminalNode(right2);
        termNode2LessEqual.setValue("x");
        TerminalNode termNode2LessEqualOther = new TerminalNode(right2);
        termNode2LessEqualOther.setValue("y");
        right2.setLeftChild(termNode2LessEqual);
        right2.setRightChild(termNode2LessEqualOther);
        and2.setRightChild(right2);
        assertFalse(WhereExpressions.equals(expn1, expn2));
    }
}

