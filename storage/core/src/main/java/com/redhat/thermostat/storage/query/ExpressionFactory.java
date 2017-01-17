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

import java.util.Set;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;

/**
 * This class provides convenience methods that should be used
 * to create all expressions used in queries.
 * 
 * @see Expression
 * @see Query#where(Expression)
 */
public class ExpressionFactory {
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing the
     * provided key and value for equality.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> equalTo(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.EQUALS);
    }
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing the
     * provided key and value for inequality.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> notEqualTo(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.NOT_EQUAL_TO);
    }
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing if the
     * provided key has a value greater than the provided value.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> greaterThan(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.GREATER_THAN);
    }
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing if the
     * provided key has a value less than the provided value.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> lessThan(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.LESS_THAN);
    }
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing if the
     * provided key has a value greater than or equal to the provided value.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> greaterThanOrEqualTo(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.GREATER_THAN_OR_EQUAL_TO);
    }
    
    /**
     * Creates a {@link BinaryComparisonExpression} comparing if the
     * provided key has a value less than or equal to the provided value.
     * @param key - {@link Key} whose value to compare against the provided value
     * @param value - the value to compare against the key
     * @return the new comparison expression
     */
    public <T> BinaryComparisonExpression<T> lessThanOrEqualTo(Key<T> key, T value) {
        return createComparisonExpression(key, value, BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
    }
    
    /**
     * Creates a {@link BinarySetMembershipExpression} comparing if the
     * provided key has a value equal to any of the provided values.
     * @param key - {@link Key} whose value to compare against the provided values
     * @param value - a set of values to compare against the key
     * @param type - the type of values of stored in the provided set
     * @return the new comparison expression
     */
    public <T> BinarySetMembershipExpression<T> in(Key<T> key, Set<T> values, Class<T> type) {
        return createSetMembershipExpression(key, values, BinarySetMembershipOperator.IN, type);
    }
    
    /**
     * Creates a {@link BinarySetMembershipExpression} comparing if the
     * provided key has a value not equal to all of the provided values.
     * @param key - {@link Key} whose value to compare against the provided values
     * @param value - a set of values to compare against the key
     * @param type - the type of values of stored in the provided set
     * @return the new comparison expression
     */
    public <T> BinarySetMembershipExpression<T> notIn(Key<T> key, Set<T> values, Class<T> type) {
        return createSetMembershipExpression(key, values, BinarySetMembershipOperator.NOT_IN, type);
    }
    
    /**
     * Creates a {@link UnaryLogicalExpression} which is a logical
     * negation of the provided expression.
     * @param expr - the expression to negate
     * @return the new negated expression
     */
    public <T extends ComparisonExpression> UnaryLogicalExpression<T> not(T expr) {
        return new UnaryLogicalExpression<>(expr, UnaryLogicalOperator.NOT);
    }
    
    /**
     * Creates a {@link BinaryLogicalExpression} with the two provided expressions
     * joined in order by a logical AND operation.
     * @param left - the left operand
     * @param right - the right operand
     * @return the new logical expression
     */
    public <S extends Expression, T extends Expression> BinaryLogicalExpression<S, T> and(S left, T right) {
        return new BinaryLogicalExpression<S, T>(left, BinaryLogicalOperator.AND, right);
    }
    
    /**
     * Creates a {@link BinaryLogicalExpression} with the two provided expressions
     * joined in order by a logical OR operation.
     * @param left - the left operand
     * @param right - the right operand
     * @return the new logical expression
     */
    public <S extends Expression, T extends Expression> BinaryLogicalExpression<S, T> or(S left, T right) {
        return new BinaryLogicalExpression<S, T>(left, BinaryLogicalOperator.OR, right);
    }
    
    private <T> BinaryComparisonExpression<T> createComparisonExpression(Key<T> key, T value, BinaryComparisonOperator op) {
        return new BinaryComparisonExpression<>(new LiteralExpression<>(key), op, new LiteralExpression<>(value));
    }
    
    private <T> BinarySetMembershipExpression<T> createSetMembershipExpression(Key<T> key, Set<T> values,
            BinarySetMembershipOperator op, Class<T> type) {
        return new BinarySetMembershipExpression<>(new LiteralExpression<>(key), op, 
                new LiteralSetExpression<>(values, type));
    }
    
}

