/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.redhat.thermostat.common.storage.AbstractQuery;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.MongoQuery;

public class MockQuery extends AbstractQuery {

    public static class WhereClause <T> {
        public final Key<T> key;
        public final Criteria criteria;
        public final T value;

        public WhereClause(Key<T> key, Criteria criteria, T value) {
            this.key = key;
            this.criteria = criteria;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof WhereClause)) {
                return false;
            }
            WhereClause<?> other = (WhereClause<?>) obj;
            return Objects.equals(key, other.key) && Objects.equals(criteria, other.criteria) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, criteria, value);
        }
    }

    private final List<WhereClause<?>> whereClauses = new ArrayList<>();
    private Category category;

    @Override
    public MockQuery from(Category category) {
        setCategory(category);
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public <T> MockQuery where(Key<T> key, Criteria criteria, T value) {
        whereClauses.add(new WhereClause<>(key, criteria, value));
        return this;
    }

    public List<WhereClause<?>> getWhereClauses() {
        return whereClauses;
    }

    public int getWhereClausesCount() {
        return whereClauses.size();
    }

    public <T> boolean hasWhereClause(Key<T> key, Criteria criteria, T value) {
        for (WhereClause<?> whereClause: whereClauses) {
            if (whereClause.equals(new WhereClause<T>(key, criteria, value))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWhereClauseFor(Key<?> key) {
        for (WhereClause<?> where : whereClauses) {
            if (where.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MockQuery)) {
            return false;
        }
        MockQuery other = (MockQuery) obj;
        return Objects.equals(getCategory(), other.getCategory()) && Objects.equals(whereClauses, other.whereClauses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCategory(), whereClauses);
    }

    public boolean hasSort(Key<?> key, SortDirection direction) {
        
        return getSorts().contains(new Sort(key, direction));
    }

}
