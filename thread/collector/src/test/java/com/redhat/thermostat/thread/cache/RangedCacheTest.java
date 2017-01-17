/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.thread.cache;

import com.redhat.thermostat.storage.model.TimeStampedPojo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 */
public class RangedCacheTest {

    private static class Bean implements TimeStampedPojo {

        private long timesStamp;

        public Bean(long timesStamp) {
            this.timesStamp = timesStamp;
        }

        @Override
        public long getTimeStamp() {
            return timesStamp;
        }

        @Override
        public String toString() {
            return "" + timesStamp;
        }
    }

    @Test
    public void testRangeCache() {
        RangedCache<Bean> cache = new RangedCache<>();

        Bean bean0  = new Bean(0);
        Bean bean1a = new Bean(1_000);
        Bean bean1b = new Bean(1_000);
        Bean bean2  = new Bean(2_000);
        Bean bean3  = new Bean(3_000);
        Bean bean4  = new Bean(4_000);
        Bean bean5  = new Bean(5_000);

        cache.put(bean0);
        cache.put(bean1a);
        cache.put(bean1b);
        cache.put(bean2);
        cache.put(bean3);
        cache.put(bean4);
        cache.put(bean5);

        List<Bean> values = cache.getValues(0, 5_000);
        assertEquals(7, values.size());

        assertTrue(values.contains(bean0));
        assertTrue(values.contains(bean1a));
        assertTrue(values.contains(bean1b));
        assertTrue(values.contains(bean2));
        assertTrue(values.contains(bean3));
        assertTrue(values.contains(bean4));
        assertTrue(values.contains(bean5));

        values = cache.getValues(2_000, 5_000);
        assertEquals(4, values.size());

        assertTrue(values.contains(bean2));
        assertTrue(values.contains(bean3));
        assertTrue(values.contains(bean4));
        assertTrue(values.contains(bean5));

        values = cache.getValues(2_100, 5_000);
        assertEquals(3, values.size());

        assertTrue(values.contains(bean3));
        assertTrue(values.contains(bean4));
        assertTrue(values.contains(bean5));

        values = cache.getValues(2_100, 4_900);
        assertEquals(2, values.size());

        assertTrue(values.contains(bean3));
        assertTrue(values.contains(bean4));

        cache.clear();

        // out of order insertion
        cache.put(bean5);
        cache.put(bean3);
        cache.put(bean2);
        cache.put(bean4);
        cache.put(bean0);
        cache.put(bean1b);
        cache.put(bean1a);

        values = cache.getValues(0, 5_000);
        assertEquals(7, values.size());

        assertEquals(bean0, values.get(0));
        // those compare to the same value, so they could eventually show
        // in either order, although the underlying list is kept in insertion
        // order
        assertEquals(bean1b, values.get(1));
        assertEquals(bean1a, values.get(2));
        assertEquals(bean2, values.get(3));
        assertEquals(bean3, values.get(4));
        assertEquals(bean4, values.get(5));
        assertEquals(bean5, values.get(6));
    }
}
