/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import static com.redhat.thermostat.storage.core.experimental.statement.TypeMapper.Symbol;
import static com.redhat.thermostat.storage.core.experimental.statement.TypeMapper.Clause;
import static com.redhat.thermostat.storage.core.experimental.statement.TypeMapper.Symbol.Quote;
import static com.redhat.thermostat.storage.core.experimental.statement.TypeMapper.Symbol.Sort;
import static com.redhat.thermostat.storage.core.experimental.statement.TypeMapper.Symbol.Space;

/**
 *
 */
class QueryEngine extends StatementEngine {

    private String limit;
    private String sort;

    public QueryEngine() {
        super(TypeMapper.Statement.Query);
        addDelimiter = false;
        delimiter = TypeMapper.Symbol.InsertSeparator.id();
        delimiter = Symbol.QuerySeparator.id();
        tokens.add(Clause.Query.id());
    }

    public QueryEngine add(FieldDescriptor descriptor, TypeMapper.Criteria criteria) {

        if (addDelimiter) {
            tokens.add(delimiter);
        }

        addDelimiter = true;

        StringBuilder field = new StringBuilder();

        field.append(Quote.id());
        field.append(descriptor.getName());
        field.append(Quote.id());

        tokens.add(field.toString());

        tokens.add(criteria.id());
        tokens.add(TypeMapper.get(descriptor.getType()));

        return this;
    }

    public QueryEngine sort(FieldDescriptor descriptor, TypeMapper.Sort criteria) {

        StringBuilder field = new StringBuilder();

        field.append(Sort.id());
        field.append(Space.id());

        field.append(Quote.id());
        field.append(descriptor.getName());
        field.append(Quote.id());

        field.append(Space.id());
        field.append(criteria.id());

        sort = field.toString();

        return this;
    }

    public QueryEngine limit() {

        StringBuilder field = new StringBuilder();

        field.append(Symbol.Limit.id());
        field.append(Space.id());
        field.append(TypeMapper.get(int.class));

        limit = field.toString();

        return this;
    }

    @Override
    protected void addPrologueClause() {
        tokens.add(Clause.Where.id());
    }

    @Override
    protected void buildImpl(StringBuilder builder) {
        if (sort != null) {
            builder.append(Space.id()).append(sort);
        }

        if (limit != null) {
            builder.append(Space.id()).append(limit);
        }
    }
}
