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

package com.redhat.thermostat.web.client.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.web.common.SharedStateId;

public class ExpirableWebPreparedStatementCacheTest {
    
    private static final UUID SERVER_TOKEN = UUID.randomUUID();
    private static final SharedStateId STMT_ID = new SharedStateId(3, SERVER_TOKEN);
    private WebPreparedStatementCache cache;
    private StatementDescriptor<?> desc;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        cache = new WebPreparedStatementCache();
        WebPreparedStatementHolder holder = mock(WebPreparedStatementHolder.class);
        when(holder.getStatementId()).thenReturn(STMT_ID);
        desc = new StatementDescriptor<>(mock(Category.class), "test-desc");
        cache.put(desc, holder);
    }

    @Test
    public void testGetByDesc() {
        long expires = System.nanoTime() + TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);
        ExpirableWebPreparedStatementCache expCache = new ExpirableWebPreparedStatementCache(cache, expires);
        assertNotNull("should not yet be expired", expCache.get(desc));
        @SuppressWarnings("unchecked")
        StatementDescriptor<?> notExisting = new StatementDescriptor<>(mock(Category.class), "testing");
        assertNull("entry does not exist", expCache.get(notExisting));
        
        expires = System.nanoTime() - TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);
        expCache = new ExpirableWebPreparedStatementCache(cache, expires);
        assertNull("should have expired", expCache.get(desc));
    }
    
    @Test
    public void testGetById() {
        long expires = System.nanoTime() + TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);
        ExpirableWebPreparedStatementCache expCache = new ExpirableWebPreparedStatementCache(cache, expires);
        assertNotNull("should not yet be expired", expCache.get(STMT_ID));
        assertNull("entry does not exist", expCache.get(new SharedStateId(3, UUID.randomUUID())));
        
        expires = System.nanoTime() - TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);
        expCache = new ExpirableWebPreparedStatementCache(cache, expires);
        assertNull("should have expired", expCache.get(STMT_ID));
    }
    
    @Test
    public void testIsExpired() {
        long expires = System.nanoTime() - TimeUnit.NANOSECONDS.convert(20, TimeUnit.MILLISECONDS);
        ExpirableWebPreparedStatementCache cache = new ExpirableWebPreparedStatementCache(null, expires);
        assertTrue(cache.isExpired());
        
        expires = System.nanoTime() + TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS);
        cache = new ExpirableWebPreparedStatementCache(null, expires);
        assertFalse(cache.isExpired());
    }

}
