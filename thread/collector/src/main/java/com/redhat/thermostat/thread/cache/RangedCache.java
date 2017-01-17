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

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 */
public class RangedCache<T extends TimeStampedPojo> {

    private TreeMap<Long, List<T>> cache;

    public RangedCache() {
        this(new TreeMap<Long, List<T>>());
    }

    RangedCache(TreeMap<Long, List<T>> cache) {
        this.cache = cache;
    }

    public synchronized void put(T value) {
        List<T> set = cache.get(value.getTimeStamp());
        if (set == null) {
            set = new LinkedList<>();
        }
        set.add(value);
        cache.put(value.getTimeStamp(), set);
    }

    public synchronized List<T> getValues(long lowerBound, long upperBound) {
        return getValues(new Range<>(lowerBound, upperBound));
    }

    public synchronized List<T> getValues(Range<Long> range) {
        List<T> result = new ArrayList<>();

        // the +1 is because the range upper bound is exclusive
        for (List<T> set : cache.subMap(range.getMin(), range.getMax() + 1).values()) {
            result.addAll(set);
        }

        return result;
    }

    public synchronized void clear() {
        cache.clear();
    }
}
