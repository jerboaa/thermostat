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

package com.redhat.thermostat.storage.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Key;

public class ExpressionFactoryTest {
    
    private static final Key<String> key = new Key<>("hello");
    private static final String VALUE = "world";
    private static final Set<String> VALUES = new HashSet<>(Arrays.asList("world", "worlds"));
    ExpressionFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new ExpressionFactory();
    }

    @Test
    public void testEqualTo() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.EQUALS,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.equalTo(key, VALUE));
    }

    @Test
    public void testNotEqualTo() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.NOT_EQUAL_TO,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.notEqualTo(key, VALUE));
    }

    @Test
    public void testGreaterThan() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.GREATER_THAN,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.greaterThan(key, VALUE));
    }

    @Test
    public void testLessThan() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.LESS_THAN,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.lessThan(key, VALUE));
    }

    @Test
    public void testGreaterThanOrEqualTo() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.GREATER_THAN_OR_EQUAL_TO,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.greaterThanOrEqualTo(key, VALUE));
    }

    @Test
    public void testLessThanOrEqualTo() {
        Expression expr = new BinaryComparisonExpression<>(
                new LiteralExpression<>(key), BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO,
                new LiteralExpression<>(VALUE));
        assertEquals(expr, factory.lessThanOrEqualTo(key, VALUE));
    }

    @Test
    public void testIn() {
        Expression expr = new BinarySetMembershipExpression<>(
                new LiteralExpression<>(key), BinarySetMembershipOperator.IN,
                new LiteralSetExpression<>(VALUES, String.class));
        assertEquals(expr, factory.in(key, VALUES, String.class));
    }
    
    @Test
    public void testNotIn() {
        Expression expr = new BinarySetMembershipExpression<>(
                new LiteralExpression<>(key), BinarySetMembershipOperator.NOT_IN,
                new LiteralSetExpression<>(VALUES, String.class));
        assertEquals(expr, factory.notIn(key, VALUES, String.class));
    }
    
    @Test
    public void testNot() {
        ComparisonExpression operand = mock(ComparisonExpression.class);
        Expression expr = new UnaryLogicalExpression<ComparisonExpression>(operand, UnaryLogicalOperator.NOT);
        assertEquals(expr, factory.not(operand));
    }

    @Test
    public void testAnd() {
        Expression left = mock(Expression.class);
        Expression right = mock(Expression.class);
        Expression expr = new BinaryLogicalExpression<>(left, BinaryLogicalOperator.AND, right);
        assertEquals(expr, factory.and(left, right));
    }

    @Test
    public void testOr() {
        Expression left = mock(Expression.class);
        Expression right = mock(Expression.class);
        Expression expr = new BinaryLogicalExpression<>(left, BinaryLogicalOperator.OR, right);
        assertEquals(expr, factory.or(left, right));
    }

}

