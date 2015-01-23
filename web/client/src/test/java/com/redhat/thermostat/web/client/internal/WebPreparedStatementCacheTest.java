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

package com.redhat.thermostat.web.client.internal;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;

public class WebPreparedStatementCacheTest {

    private WebPreparedStatementCache cache;

    @Before
    public void setup() {
        cache = new WebPreparedStatementCache();
    }

    @Test
    public void testPut() {
        SharedStateId id = new SharedStateId(3, UUID.randomUUID());
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        when(holder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> foo = new StatementDescriptor<>(cat,
                "something");
        cache.put(foo, holder);
        assertSame(holder, cache.get(foo));
        assertSame(foo, cache.get(id));
        WebPreparedStatementHolder holder2 = mock(WebPreparedStatementHolder.class);
        when(holder2.getStatementId()).thenReturn(id);
        cache.put(foo, holder2);
        assertNotSame(holder, cache.get(foo));
        assertSame(holder2, cache.get(foo));
        assertSame(foo, cache.get(id));
        
        // Different server token should not result in an look-up-by-id clash.
        SharedStateId id2 = new SharedStateId(3, UUID.randomUUID());
        assertNull(cache.get(id2));
    }

    @Test
    public void testGetByDesc() {
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = mock(SharedStateId.class);
        when(holder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> foo = new StatementDescriptor<>(cat,
                "something");
        cache.put(foo, holder);
        assertEquals(holder, cache.get(foo));
        StatementDescriptor<TestPojo> other = new StatementDescriptor<>(cat,
                "something-else");
        assertNull(cache.get(other));
    }

    @Test
    public void testGetById() {
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = mock(SharedStateId.class);
        when(holder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> foo = new StatementDescriptor<>(cat,
                "something");
        cache.put(foo, holder);
        assertEquals(foo, cache.get(id));
        assertNull(cache.get(mock(SharedStateId.class)));
    }

    @Test
    public void testRemove() {
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = mock(SharedStateId.class);
        when(holder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> foo = new StatementDescriptor<>(cat,
                "something");
        cache.put(foo, holder);
        assertEquals(holder, cache.get(foo));

        cache.remove(id);
        assertNull(cache.get(foo));
        assertNull(cache.get(id));
    }

    /**
     * Verify that we are able to create a snapshot of a cache in order to be
     * able to look up old ID mappings.
     */
    @Test
    public void verifyCreateSnapshot() {
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        SharedStateId id = mock(SharedStateId.class);
        when(holder.getStatementId()).thenReturn(id);
        @SuppressWarnings("unchecked")
        Category<TestPojo> cat = mock(Category.class);
        StatementDescriptor<TestPojo> foo = new StatementDescriptor<>(cat,
                "something");
        cache.put(foo, holder);
        assertEquals(holder, cache.get(foo));
        assertNotNull(cache.get(id));
        
        WebPreparedStatementCache copy = cache.createSnapshot();

        cache.remove(id);
        
        assertNull(cache.get(foo));
        assertNull(cache.get(id));
        
        assertNotNull(copy.get(foo));
        assertNotNull(copy.get(id));
    }

    private static class TestPojo implements Pojo {
        // nothing
    }
}
