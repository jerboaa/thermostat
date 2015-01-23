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

package com.redhat.thermostat.storage.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;


public class BinaryExpressionTest {
    
    private Expression left;
    private Expression right;
    private BinaryOperator op;
    private BinaryExpression<Expression, Expression, BinaryOperator> expr;

    @Before
    public void setup() {
        left = mock(Expression.class);
        right = mock(Expression.class);
        op = mock(BinaryOperator.class);
        expr = new BinaryExpression<Expression, Expression, BinaryOperator>(
                left, op, right) {
        };
    }
    
    @Test
    public void testGetLeftOperand() {
        assertEquals(left, expr.getLeftOperand());
    }
    
    @Test
    public void testGetRightOperand() {
        assertEquals(right, expr.getRightOperand());
    }
    
    @Test
    public void testGetOperator() {
        assertEquals(op, expr.getOperator());
    }
    
    @Test
    public void testEquals() {
        BinaryExpression<Expression, Expression, BinaryOperator> otherExpr = new BinaryExpression<Expression, Expression, BinaryOperator>(
                left, op, right) {
        };
        assertEquals(expr, otherExpr);
    }
    
    @Test
    public void testNotEquals() {
        Expression otherLeft = mock(Expression.class);
        BinaryExpression<Expression, Expression, BinaryOperator> otherExpr = new BinaryExpression<Expression, Expression, BinaryOperator>(
                otherLeft, op, right) {
        };
        
        assertFalse(expr.equals(otherExpr));
    }
    
    @Test
    public void testNotEqualsWrongClass() {
        assertFalse(expr.equals(new Object()));
    }
    
    @Test
    public void testNotEqualsNull() {
        assertFalse(expr.equals(null));
    }
    
    @Test
    public void testHashCode() {
        int hashCode = left.hashCode() + right.hashCode() + op.hashCode();
        assertEquals(hashCode, expr.hashCode());
    }

}

