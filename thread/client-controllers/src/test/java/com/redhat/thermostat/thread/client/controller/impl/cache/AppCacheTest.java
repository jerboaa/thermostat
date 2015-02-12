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

package com.redhat.thermostat.thread.client.controller.impl.cache;

import com.redhat.thermostat.common.ApplicationCache;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppCacheTest {
    private ApplicationCache applicationCache;
    private AppCacheKey key;

    @Before
    public void setUp() {
        this.key = new AppCacheKey("root", AppCache.class);
        applicationCache = new ApplicationCache();
    }

    @Test
    public void testCache() throws Exception {
        AppCache cache = new AppCache(key, applicationCache);

        AppCacheKey key0 = new AppCacheKey("some-value-under-root-key", String.class);
        cache.save(key0, "some-value-under-root-value");

        String value = cache.retrieve(key0);
        assertEquals(value, "some-value-under-root-value");

        Object someValue = new Object();
        AppCacheKey key1 = new AppCacheKey("some-other-value-under-root-key", Object.class);
        cache.save(key1, someValue);

        Object value2 = cache.retrieve(key1);
        assertEquals(value2, someValue);

        cache.save(key1, "some-value-under-root-value");
        value = cache.retrieve(key1);
        assertEquals(value, "some-value-under-root-value");
    }
}
