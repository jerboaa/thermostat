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

package com.redhat.thermostat.thread.dao.impl.statement.support;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.model.Pojo;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BeanAdapterBuilder<T extends Pojo> {

    private Class<T> target;
    private BeanAdapter<T> adapter;

    private List<Query<T>> queries;

    public BeanAdapterBuilder(Class<T> target, List<Query<T>> queries) {
        this.target = target;
        this.queries = queries;
    }

    public BeanAdapterBuilder(Class<T> target, Query<T> query) {
        this(target, new ArrayList<Query<T>>());
        queries.add(query);
    }

    public BeanAdapter<T> build() {
        try {
            Category<T> category = new CategoryBuilder<>(target).build();
            List<FieldDescriptor> fieldDescriptors =
                    StatementUtils.createDescriptors(target);

            adapter = new BeanAdapter<>();
            adapter.setCategory(category);
            adapter.setFieldDescriptors(fieldDescriptors);

            createInsert(fieldDescriptors);
            createQueries();

            return adapter;

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected void createQueries() {
        for (Query<T> query : queries) {
            createQuery(query);
        }
    }

    private void createQuery(Query<T> query) {
        List<Criterion> queryDescriptors = query.describe();

        QueryEngine engine = new QueryEngine();
        engine.prologue(adapter.getCategory());
        for (Criterion criterion : queryDescriptors) {
            if (criterion instanceof WhereCriterion) {
                WhereCriterion where = (WhereCriterion) criterion;
                engine.add(where.getFieldDescriptor(), where.getCriteria());

            } else if (criterion instanceof SortCriterion) {
                SortCriterion sort = (SortCriterion) criterion;
                engine.sort(sort.getFieldDescriptor(), sort.getCriteria());

            } else if (criterion instanceof LimitCriterion) {
                engine.limit();
            }
        }

        adapter.addStatement(query.getId(), engine.build());
        adapter.addQuery(query.getId(), query);
    }

    protected void createInsert(List<FieldDescriptor> descriptors) {

        InsertEngine engine = new InsertEngine();
        engine.prologue(adapter.getCategory());

        for (FieldDescriptor descriptor : descriptors) {
            engine.add(descriptor);
        }

        adapter.addStatement(BeanAdapter.INSERT, engine.build());
    }
}
