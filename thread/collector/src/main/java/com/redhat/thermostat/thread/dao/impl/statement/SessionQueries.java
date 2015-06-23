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

package com.redhat.thermostat.thread.dao.impl.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.storage.core.Id;
import com.redhat.thermostat.storage.core.experimental.statement.FieldDescriptor;
import com.redhat.thermostat.storage.core.experimental.statement.LimitCriterion;
import com.redhat.thermostat.storage.core.experimental.statement.Query;
import com.redhat.thermostat.storage.core.experimental.statement.SortCriterion;
import com.redhat.thermostat.storage.core.experimental.statement.StatementUtils;
import com.redhat.thermostat.storage.core.experimental.statement.TypeMapper;
import com.redhat.thermostat.storage.core.experimental.statement.WhereCriterion;
import com.redhat.thermostat.thread.model.ThreadSession;

/**
 *
 */
public class SessionQueries {

    public static final Id getRangeAscending = new Id("SessionQueries::getRangeAscending");
    public static final Id getRangeDescending = new Id("SessionQueries::getRangeDescending");

    public static class CriteriaId {
        public static final Id vmId = new Id("vmId");
        public static final Id agentId = new Id("agentId");
        public static final Id timeStampGEQ = new Id("timeStampGEQ");
        public static final Id timeStampLEQ = new Id("timeStampLEQ");
        public static final Id limit = new Id("limit");
    }

    private static class GetDescending extends Query<ThreadSession> {
        @Override
        protected void describe(Criteria criteria) {
            List<FieldDescriptor> descriptors = StatementUtils.createDescriptors(ThreadSession.class);
            final Map<String, FieldDescriptor> map = StatementUtils.createDescriptorMap(descriptors);

            criteria.add(new WhereCriterion(CriteriaId.vmId, map.get("vmId"),
                                            TypeMapper.Criteria.Equal));
            criteria.add(new WhereCriterion(CriteriaId.agentId, map.get("agentId"),
                                            TypeMapper.Criteria.Equal));

            criteria.add(new WhereCriterion(CriteriaId.timeStampGEQ, map.get("timeStamp"),
                                            TypeMapper.Criteria.GreaterEqual));
            criteria.add(new WhereCriterion(CriteriaId.timeStampLEQ, map.get("timeStamp"),
                                            TypeMapper.Criteria.LessEqual));

            criteria.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Descending));
            criteria.add(new LimitCriterion(CriteriaId.limit));
        }

        @Override
        public Id getId() {
            return getRangeDescending;
        }
    }

    private static class GetAscending extends Query<ThreadSession> {
        @Override
        protected void describe(Criteria criteria) {
            List<FieldDescriptor> descriptors = StatementUtils.createDescriptors(ThreadSession.class);
            final Map<String, FieldDescriptor> map = StatementUtils.createDescriptorMap(descriptors);

            criteria.add(new WhereCriterion(CriteriaId.vmId, map.get("vmId"),
                                            TypeMapper.Criteria.Equal));
            criteria.add(new WhereCriterion(CriteriaId.agentId, map.get("agentId"),
                                            TypeMapper.Criteria.Equal));

            criteria.add(new WhereCriterion(CriteriaId.timeStampGEQ, map.get("timeStamp"),
                                            TypeMapper.Criteria.GreaterEqual));
            criteria.add(new WhereCriterion(CriteriaId.timeStampLEQ, map.get("timeStamp"),
                                            TypeMapper.Criteria.LessEqual));

            criteria.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Ascending));
            criteria.add(new LimitCriterion(CriteriaId.limit));
        }

        @Override
        public Id getId() {
            return getRangeAscending;
        }
    }

    private static final List<Query<ThreadSession>> queries = new ArrayList<>();
    static {
        queries.add(new GetDescending());
        queries.add(new GetAscending());
    }

    public static List<Query<ThreadSession>> asList() {
        return Collections.unmodifiableList(queries);
    }
}
