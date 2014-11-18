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

import com.redhat.thermostat.storage.model.Pojo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class Query<T extends Pojo> {

    protected final class Criteria {
        private List<Criterion> criteria;
        private Map<Id, Criterion> map;

        Criteria() {
            criteria = new ArrayList<>();
            map = new HashMap<>();
        }

        public void add(Criterion criterion) {
            Id id = criterion.getId();
            if (map.containsKey(id)) {
                throw new IllegalArgumentException("Already contains criteria" +
                                                   " with this id." +
                                                   " New: " + criterion +
                                                   " Old: " + map.get(id));
            }
            map.put(id, criterion);
            criteria.add(criterion);
        }
    }

    private List<Criterion> describedQuery;
    private Criteria criteria;

    public Query() {
        criteria = new Criteria();
    }

    public abstract Id getId();

    public final List<Criterion> describe() {
        if (describedQuery == null) {
            describe(criteria);
            describedQuery = Collections.unmodifiableList(criteria.criteria);
        }
        return describedQuery;
    }

    protected abstract void describe(Criteria criteria);

    public final QueryValues createValues() {
        if (describedQuery == null) {
            throw new IllegalStateException("Query must be described first");
        }

        QueryValues setter = new QueryValues(this);
        setter.addCriteria(describedQuery);

        return setter;
    }
}
