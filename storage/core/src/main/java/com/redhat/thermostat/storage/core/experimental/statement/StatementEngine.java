/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import com.redhat.thermostat.storage.model.Pojo;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
abstract class StatementEngine {

    protected List<String> tokens;

    private TypeMapper.Statement type;
    protected String delimiter;

    protected boolean addDelimiter;

    protected StatementEngine(TypeMapper.Statement type) {
        tokens = new LinkedList<>();
        this.type = type;
        addDelimiter = false;
    }

    protected StatementEngine add(FieldDescriptor descriptor, TypeMapper.Criteria criteria) {

        if (addDelimiter) {
            tokens.add(delimiter);
        }

        addDelimiter = true;

        StringBuilder field = new StringBuilder();

        field.append(TypeMapper.Symbol.Quote.id());
        field.append(descriptor.getName());
        field.append(TypeMapper.Symbol.Quote.id());

        tokens.add(field.toString());

        tokens.add(criteria.id());
        tokens.add(TypeMapper.get(descriptor.getType()));

        return this;
    }

    protected abstract void addPrologueClause();

    public StatementEngine prologue(com.redhat.thermostat.storage.core.Category<? extends Pojo> category) {
        tokens.add(category.getName());
        addPrologueClause();
        return this;
    }

    protected void buildImpl(StringBuilder builder) {}


    public Statement build() {
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            builder.append(token).append(TypeMapper.Symbol.Space.id());
        }
        int position = builder.length() - 1;
        builder.deleteCharAt(position);

        buildImpl(builder);

        return new Statement(builder.toString());
    }
}
