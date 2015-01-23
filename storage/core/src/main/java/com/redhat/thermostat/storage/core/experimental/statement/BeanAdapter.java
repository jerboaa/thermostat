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

package com.redhat.thermostat.storage.core.experimental.statement;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.Pojo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class BeanAdapter<T extends Pojo> {
    public static final Id INSERT = new Id("BeanAdapter_Insert");

    private static final Logger logger = LoggingUtils.getLogger(BeanAdapter.class);

    protected Category<T> category;

    private Map<Id, Statement> describedQueries;
    private Map<Id, Query<T>> queries;
    private List<FieldDescriptor> fieldDescriptors;

    public BeanAdapter() {
        this.queries = new HashMap<>();
        this.describedQueries = new HashMap<>();
    }

    protected void setCategory(Category<T> category) {
        this.category = category;
    }

    public Category<T> getCategory() {
        return category;
    }

    public Set<String> describeStatements() {
        Set<String> descriptions = new HashSet<>();
        for (Statement statement : describedQueries.values()) {
            descriptions.add(statement.get());
        }
        return descriptions;
    }

    public void insert(T bean, Storage storage)
            throws StatementExecutionException
    {
        Statement statement = describedQueries.get(INSERT);

        StatementDescriptor<T> desc =
                new StatementDescriptor<>(category, statement.get());
        try {
            PreparedStatement<T> prepared = storage.prepareStatement(desc);
            int i = 0;
            for (FieldDescriptor descriptor : fieldDescriptors) {
                Value value = StatementUtils.getValue(bean, descriptor);
                StatementUtils.setData(prepared, value, i++);
            }

            prepared.execute();

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        }
    }

    public Query<T> getQuery(Id id) {
        return queries.get(id);
    }

    public void query(QueryValues values, ResultHandler<T> handler,
                      Storage storage) throws StatementExecutionException
    {
        Objects.requireNonNull(handler, "ResultHandler cannot be null");
        Objects.requireNonNull(values, "QueryValues cannot be null");
        Objects.requireNonNull(storage, "Storage cannot be null");

        Query query = values.getQuery();
        if (!queries.containsKey(query.getId())) {
            throw new IllegalArgumentException("This adapter does not know" +
                                               "about the given query: " +
                                               query.getId());
        }

        Statement statement = describedQueries.get(query.getId());
        List<Criterion> criteria = query.describe();

        StatementDescriptor<T> desc =
                new StatementDescriptor<>(category, statement.get());
        PreparedStatement<T> prepared = null;
        try {
            prepared = storage.prepareStatement(desc);
            int i = 0;
            for (Criterion criterion : criteria) {
                // sort doesn't take values
                if (criterion instanceof SortCriterion) {
                    continue;
                }

                Value value = values.getValue(criterion);
                StatementUtils.setData(prepared, value, i++);
            }

        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc +
                                     "' failed!", e);

            // this can't really happen, but if it does is serious
            throw new AssertionError("Autogenerated statement failed to parse",
                                     e);
        }

        Cursor<T> cursor = prepared.executeQuery();
        boolean needMoreResults = true;
        while (cursor.hasNext() && needMoreResults) {
            T result = cursor.next();
            try {
                needMoreResults = handler.onResult(result);

            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Exception executing results", t);
            }
        }
    }

    protected void addStatement(Id id, Statement statement) {
        describedQueries.put(id, statement);
    }

    protected void addQuery(Id id, Query query) {
        queries.put(id, query);
    }

    protected void setFieldDescriptors(List<FieldDescriptor> fieldDescriptors) {
        this.fieldDescriptors = fieldDescriptors;
    }
}
