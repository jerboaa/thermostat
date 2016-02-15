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

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Id;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BeanAdapterTest {

    private Storage storage;
    private PreparedStatement<SampleBean> insert;
    private PreparedStatement<SampleBean> query;

    private List<Query<SampleBean>> queries;

    private static final Id SORT = new Id("SortByTimeStamp");
    private static final Id RANGE = new Id("RangedQuery");

    @Before
    public void setUp() throws Exception {
        storage = mock(Storage.class);
        insert = mock(PreparedStatement.class);
        query = mock(PreparedStatement.class);

        queries = new ArrayList<>();

        List<FieldDescriptor> descriptors = StatementUtils.createDescriptors(SampleBean.class);
        final Map<String, FieldDescriptor> map = StatementUtils.createDescriptorMap(descriptors);
        Query query = new Query<SampleBean>() {
            @Override
            protected void describe(Criteria criterias) {
                criterias.add(new WhereCriterion(new Id("0"), map.get("name"), TypeMapper.Criteria.Equal));
                criterias.add(new WhereCriterion(new Id("1"), map.get("vmId"), TypeMapper.Criteria.GreaterEqual));
                criterias.add(new WhereCriterion(new Id("2"), map.get("vmId"), TypeMapper.Criteria.LessEqual));
                criterias.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Ascending));
                criterias.add(new LimitCriterion(new Id("3")));
            }

            @Override
            public Id getId() {
                return SORT;
            }
        };
        queries.add(query);

        query = new Query<SampleBean>() {
            @Override
            protected void describe(Criteria criterias) {
                criterias.add(new WhereCriterion(new Id("0"), map.get("name"), TypeMapper.Criteria.Equal));
                criterias.add(new WhereCriterion(new Id("1"), map.get("timeStamp"), TypeMapper.Criteria.GreaterEqual));
                criterias.add(new WhereCriterion(new Id("2"), map.get("timeStamp"), TypeMapper.Criteria.LessEqual));
                criterias.add(new SortCriterion(map.get("timeStamp"), TypeMapper.Sort.Descending));
                criterias.add(new LimitCriterion(new Id("3")));
            }

            @Override
            public Id getId() {
                return RANGE;
            }
        };
        queries.add(query);
    }

    @Test
    public void testHasQueries() throws Exception {
        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();

        Query query = adapter.getQuery(SORT);
        assertSame(queries.get(0), query);

        query = adapter.getQuery(RANGE);
        assertSame(queries.get(1), query);
    }

    @Test
    public void testInsert() throws Exception {
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(insert);

        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();

        SampleBean bean = new SampleBean();
        bean.setName("fluff");
        bean.setTimeStamp(1000l);
        bean.setVmId(0xcafe);

        adapter.insert(bean, storage);

        verify(storage).prepareStatement(any(StatementDescriptor.class));

        // the order is given by the bean methods, they are sorted in
        // alphabetical order, this is not really important though,
        // since the order is based on the same FieldDescriptors used by
        // both the builder and the adapter, so it's transparent to the user

        verify(insert).setString(0, "fluff");
        verify(insert).setLong(1, 1000l);
        verify(insert).setInt(2, 0xcafe);

        verify(insert).execute();
        verifyNoMoreInteractions(insert);
    }

    @Test
    public void testSortQuery() throws Exception {

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(query);
        Cursor cursor = mock(Cursor.class);
        SampleBean sample = mock(SampleBean.class);
        when(query.executeQuery()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(sample);

        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();
        Query sort = adapter.getQuery(SORT);
        assertNotNull(sort);
        assertSame(sort, queries.get(0));

        QueryValues values = sort.createValues();
        values.set(new Id("0"), "fluff");
        values.set(new Id("1"), 5);
        values.set(new Id("2"), 5);
        values.set(new Id("3"), 2);

        final boolean [] called = new boolean[1];
        ResultHandler<SampleBean> handler = new ResultHandler<SampleBean>() {
            @Override
            public boolean onResult(SampleBean result) {
                called[0] = true;
                return true;
            }
        };

        adapter.query(values, handler, storage);

        verify(query).setString(0, "fluff");
        verify(query).setInt(1, 5);
        verify(query).setInt(2, 5);
        verify(query).setInt(3, 2);

        assertTrue(called[0]);
    }

    @Test
    public void testRangeQuery() throws Exception {

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(query);
        Cursor cursor = mock(Cursor.class);
        SampleBean sample = mock(SampleBean.class);
        when(query.executeQuery()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(sample);

        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();
        Query range = adapter.getQuery(RANGE);
        assertNotNull(range);
        assertSame(range, queries.get(1));

        QueryValues values = range.createValues();
        values.set(new Id("0"), "fluff");
        values.set(new Id("1"), 10l);
        values.set(new Id("2"), 20l);
        values.set(new Id("3"), 2);

        final boolean [] called = new boolean[1];
        ResultHandler<SampleBean> handler = new ResultHandler<SampleBean>() {
            @Override
            public boolean onResult(SampleBean result) {
                called[0] = true;
                return true;
            }
        };

        adapter.query(values, handler, storage);

        verify(query).setString(0, "fluff");
        verify(query).setLong(1, 10);
        verify(query).setLong(2, 20);
        verify(query).setInt(3, 2);

        assertTrue(called[0]);
    }

    @Test
    public void testSkipResultsOnHandlerRequest() throws Exception {
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(query);
        Cursor cursor = mock(Cursor.class);

        final SampleBean sample0 = mock(SampleBean.class);
        final SampleBean sample1 = mock(SampleBean.class);
        final SampleBean sample2 = mock(SampleBean.class);

        when(query.executeQuery()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true);
        when(cursor.next()).thenReturn(sample0).thenReturn(sample1).thenReturn(sample2);

        BeanAdapterBuilder<SampleBean> builder =
                new BeanAdapterBuilder<>(SampleBean.class, queries);

        BeanAdapter<SampleBean> adapter = builder.build();
        Query range = adapter.getQuery(RANGE);
        QueryValues values = range.createValues();
        values.set(new Id("0"), "fluff");
        values.set(new Id("1"), 10l);
        values.set(new Id("2"), 20l);
        values.set(new Id("3"), 2);

        ResultHandler<SampleBean> handler = new ResultHandler<SampleBean>() {
            @Override
            public boolean onResult(SampleBean result) {
                if (result.equals(sample1)) {
                    return false;
                }
                return true;
            }
        };
        adapter.query(values, handler, storage);

        verify(cursor, times(2)).next();
    }
}
