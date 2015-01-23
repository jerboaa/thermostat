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


public class UnaryExpressionTest {
    
    private Expression operand;
    private UnaryOperator operator;
    private UnaryExpression<Expression, UnaryOperator> expr;

    @Before
    public void setup() {
        operand = mock(Expression.class);
        operator = mock(UnaryOperator.class);
        expr = new UnaryExpression<Expression, UnaryOperator>(
                operand, operator) {
        };
    }
    
    @Test
    public void testGetOperand() {
        assertEquals(operand, expr.getOperand());
    }
    
    @Test
    public void testGetOperator() {
        assertEquals(operator, expr.getOperator());
    }
    
    @Test
    public void testEquals() {
        UnaryExpression<Expression, UnaryOperator> otherExpr = new UnaryExpression<Expression, UnaryOperator>(
                operand, operator) {
        };
        assertEquals(expr, otherExpr);
    }
    
    @Test
    public void testNotEquals() {
        Expression otherOperand = mock(Expression.class);
        UnaryExpression<Expression, UnaryOperator> otherExpr = new UnaryExpression<Expression, UnaryOperator>(
                otherOperand, operator) {
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
        int hashCode = operator.hashCode() + operand.hashCode();
        assertEquals(hashCode, expr.hashCode());
    }

}

