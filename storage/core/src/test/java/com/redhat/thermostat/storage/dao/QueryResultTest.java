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

package com.redhat.thermostat.storage.dao;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class QueryResultTest {

    private static final List<VmInfo> MOCK_RESULTS = Collections.unmodifiableList(Arrays.asList(mock(VmInfo.class), mock(VmInfo.class)));

    @Test
    public void testAsListContents() {
        QueryResult<VmInfo> queryResult = getQueryResult(MOCK_RESULTS);
        List<VmInfo> list = queryResult.asList();
        assertThat(list, is(equalTo(MOCK_RESULTS)));
    }

    @Test
    public void testAsListAllowsInPlaceModification() {
        QueryResult<VmInfo> queryResult = getQueryResult(MOCK_RESULTS);
        List<VmInfo> list = queryResult.asList();
        VmInfo foo = mock(VmInfo.class);
        list.add(foo);
        assertThat(queryResult.asList().contains(foo), is(true));
        list.clear();
        assertThat(queryResult.asList().isEmpty(), is(true));
    }

    @Test
    public void testHeadEmpty() {
        QueryResult<VmInfo> queryResult = getQueryResult(Collections.<VmInfo>emptyList());
        VmInfo head = queryResult.head();
        assertThat(head, is(equalTo(null)));
    }

    @Test
    public void testHeadNonEmpty() {
        QueryResult<VmInfo> queryResult = getQueryResult(MOCK_RESULTS);
        VmInfo head = queryResult.head();
        assertThat(head, is(MOCK_RESULTS.get(0)));
    }

    @Test
    public void testIterator() {
        QueryResult<VmInfo> queryResult = getQueryResult(MOCK_RESULTS);
        Iterator<VmInfo> iterator = queryResult.iterator();
        List<VmInfo> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        assertThat(list, is(equalTo(MOCK_RESULTS)));
    }

    @Test
    public void testIteratorCanRemove() {
        List<VmInfo> list = new ArrayList<>();
        list.addAll(MOCK_RESULTS);
        VmInfo foo = MOCK_RESULTS.get(0);
        QueryResult<VmInfo> queryResult = getQueryResult(list);
        assertThat(queryResult.asList().size(), is(equalTo(2)));
        assertThat(queryResult.asList().contains(foo), is(true));
        Iterator<VmInfo> iterator = queryResult.iterator();
        iterator.next();
        iterator.remove();
        assertThat(queryResult.asList().size(), is(equalTo(1)));
        assertThat(queryResult.asList().contains(foo), is(false));
    }

    private QueryResult<VmInfo> getQueryResult(final List<VmInfo> list) {
        Cursor<VmInfo> cursor = new Cursor<VmInfo>() {

            private final Iterator<VmInfo> it = list.iterator();

            @Override
            public void setBatchSize(int n) throws IllegalArgumentException {
            }

            @Override
            public int getBatchSize() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public VmInfo next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new QueryResult<>(cursor);
    }

}
