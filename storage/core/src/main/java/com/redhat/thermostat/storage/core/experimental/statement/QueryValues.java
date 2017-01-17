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

package com.redhat.thermostat.storage.core.experimental.statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.storage.core.Id;

/**
 *
 */
public class QueryValues {

    private static final Class<?> EMPTY_VALUE = QueryValues.class;

    private Query query;
    private Map<Id, Value> values;
    private Map<Id, Class<?>> types;

    protected QueryValues(Query query) {
        this.query = query;
        values = new HashMap<>();
        types = new HashMap<>();
    }

    public Query getQuery() {
        return query;
    }

    public void set(Id criteriaId, String value) {
        setImpl(criteriaId, value);
    }

    public void set(Id criteriaId, long value) {
        setImpl(criteriaId, value);
    }

    public void set(Id criteriaId, int value) {
        setImpl(criteriaId, value);
    }

    public void set(Id criteriaId, boolean value) {
        setImpl(criteriaId, value);
    }

    private void setImpl(Id criteriaId, Object value) {
        if (!values.containsKey(criteriaId)) {
            throw new IllegalArgumentException("Query does not contain this criteria");
        }
        check(criteriaId, value);
    }

    private void check(Id criteriaId, Object value) {

        Class<?> type = value.getClass();
        Class<?> targetType = types.get(criteriaId);

        boolean match = false;
        if (targetType.isAssignableFrom(int.class) ||
            targetType.isAssignableFrom(Integer.class))
        {
            if (type.isAssignableFrom(int.class) ||
                type.isAssignableFrom(Integer.class))
            {
                match = true;
            }
        } else if (targetType.isAssignableFrom(long.class) ||
                   targetType.isAssignableFrom(Long.class))
        {
            if (type.isAssignableFrom(long.class) ||
                type.isAssignableFrom(Long.class))
            {
                match = true;
            }
        } else if (targetType.isAssignableFrom(boolean.class) ||
                   targetType.isAssignableFrom(Boolean.class))
        {
            if (type.isAssignableFrom(boolean.class) ||
                type.isAssignableFrom(Boolean.class))
            {
                match = true;
            }
        } else if (targetType.isAssignableFrom(String.class)) {
            if (type.isAssignableFrom(String.class))
            {
                match = true;
            }
        }

        if (!match) {
            throw new IllegalArgumentException("value type does not match " +
                                               "target criteria with id: " +
                                               criteriaId.get() +
                                               " Wanted: "  +
                                               targetType + ", received: "  +
                                               type);
        }

        values.put(criteriaId, new Value(value));
    }

    public void set(Criterion criterion, Object value) {
        setImpl(criterion.getId(), value);
    }

    Value getValue(Criterion criterion) {
        return values.get(criterion.getId());
    }

    void addCriteria(List<Criterion> _criteria) {
        for (Criterion criterion : _criteria) {

            // sort doesn't take values
            if (criterion instanceof SortCriterion) {
                continue;
            }

            Id id = criterion.getId();
            values.put(id, new Value(EMPTY_VALUE));
            types.put(id, criterion.getType());
        }
    }
}
