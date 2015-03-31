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

package com.redhat.thermostat.storage.core;

import java.util.Objects;

import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;

/**
 * Common super class for aggregate queries.
 */
public abstract class AggregateQuery<T extends Pojo> implements Query<T> {
    
    public enum AggregateFunction {
        /**
         * Aggregate records by counting them.
         */
        COUNT,
        /**
         * Find distinct values for a {@link Key}
         */
        DISTINCT,
    }
    
    protected final Query<T> queryToAggregate;
    private final AggregateFunction function;
    // optional Key to aggregate values for
    private Key<?> aggregateKey;
    
    public AggregateQuery(AggregateFunction function, Query<T> queryToAggregate) {
        this.function = function;
        this.queryToAggregate = queryToAggregate;
    }
    
    @Override
    public void where(Expression expr) {
        queryToAggregate.where(expr);
    }

    @Override
    public void sort(Key<?> key,
            SortDirection direction) {
        queryToAggregate.sort(key, direction);
    }

    @Override
    public void limit(int n) {
        queryToAggregate.limit(n);
    }

    @Override
    public Expression getWhereExpression() {
        return queryToAggregate.getWhereExpression();
    }
    
    /**
     * 
     * @return The function by which to aggregate by.
     */
    public AggregateFunction getAggregateFunction() {
        return this.function;
    }
    
    /**
     * 
     * @return An optional {@link Key} to aggregate values for. May be
     *         {@code null};
     */
    public Key<?> getAggregateKey() {
        return aggregateKey;
    }

    /**
     * Sets an optional {@link Key} to aggregate values for.
     * @param aggregateKey Must not be {@code null}.
     * @throws NullPointerException If the aggregate key was {@code null}
     */
    public void setAggregateKey(Key<?> aggregateKey) {
        this.aggregateKey = Objects.requireNonNull(aggregateKey);
    }
    
}

