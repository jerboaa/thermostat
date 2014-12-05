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

import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.internal.statement.StatementDescriptorTester;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BeanAdapterBuilderTest {

    private List<Query<SampleBean>> queries;

    @Before
    public void setUp() {
        queries = new ArrayList<>();

        List<FieldDescriptor> descriptors = StatementUtils.createDescriptors(SampleBean.class);
        final Map<String, FieldDescriptor> map = StatementUtils.createDescriptorMap(descriptors);
        Query query = new Query<SampleBean>() {
            @Override
            protected void describe(Criteria criterias) {
                criterias.add(new WhereCriterion(new Id("name"), map.get("name"), TypeMapper.Criteria.Equal));
                criterias.add(new WhereCriterion(new Id("vmId>="), map.get("vmId"), TypeMapper.Criteria.GreaterEqual));
                criterias.add(new WhereCriterion(new Id("vmId<="), map.get("vmId"), TypeMapper.Criteria.LessEqual));
                criterias.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Ascending));
                criterias.add(new LimitCriterion(new Id("limit")));
            }

            @Override
            public Id getId() {
                return new Id("SortByTimeStamp");
            }
        };
        queries.add(query);

        query = new Query<SampleBean>() {
            @Override
            protected void describe(Criteria criterias) {
                criterias.add(new WhereCriterion(new Id("name"), map.get("name"), TypeMapper.Criteria.Equal));
                criterias.add(new WhereCriterion(new Id("timeStamp>="), map.get("timeStamp"), TypeMapper.Criteria.GreaterEqual));
                criterias.add(new WhereCriterion(new Id("timeStamp<="), map.get("timeStamp"), TypeMapper.Criteria.LessEqual));
                criterias.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Ascending));
                criterias.add(new LimitCriterion(new Id("limit")));
            }

            @Override
            public Id getId() {
                return new Id("RangedQuery");
            }
        };
        queries.add(query);
    }

    @Test
    public void testBeanAdapterBuilder() throws Exception {

        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();
        assertNotNull(adapter);

        com.redhat.thermostat.storage.core.Category<SampleBean> category =
                adapter.getCategory();

        assertNotNull(category);
        assertEquals("testCategory", category.getName());

        String expected = "ADD testCategory SET 'name' = ?s , 'timeStamp' = ?l , 'vmId' = ?i";
        Set<String> statements = adapter.describeStatements();

        assertEquals(3, statements.size());
        assertTrue(statements.contains(expected));

        expected = "QUERY testCategory WHERE 'name' = ?s AND 'vmId' >= ?i AND 'vmId' <= ?i SORT 'timeStamp' ASC LIMIT ?i";
        assertTrue(statements.contains(expected));

        expected = "QUERY testCategory WHERE 'name' = ?s AND 'timeStamp' >= ?l AND 'timeStamp' <= ?l SORT 'timeStamp' ASC LIMIT ?i";
        assertTrue(statements.contains(expected));

        for (String s : statements) {
            testStatement(category, s);
        }
    }

    private void testStatement(com.redhat.thermostat.storage.core.Category<SampleBean> category,
                               String statement) throws DescriptorParsingException
    {
        StatementDescriptorTester<SampleBean> tester = new StatementDescriptorTester<>();
        StatementDescriptor<SampleBean> desc = new StatementDescriptor<>(category, statement);
        tester.testParseBasic(desc);
        tester.testParseSemantic(desc);
    }
}
