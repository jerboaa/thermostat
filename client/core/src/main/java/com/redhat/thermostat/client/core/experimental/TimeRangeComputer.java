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

package com.redhat.thermostat.client.core.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.common.model.Range;

/**
 * Compute additional data ranges to fetch when trying to update a currently
 * displayed time-data with additional time-data.
 */
public class TimeRangeComputer {

    public static List<Range<Long>> computeAdditionalRangesToFetch(Range<Long> availableRange, Range<Long> desiredRange, Range<Long> displayedRange) {

        // TODO find out how to show 'sampled' values across a
        // very, very large desiredRange

        // Do we need to show additional data?
        if (displayedRange.equals(desiredRange)) {
            return Collections.emptyList();
        }

        // Do we already have all the data?
        if (contains(displayedRange, desiredRange)) {
            // TODO 'narrow' view to the desired range
            return Collections.emptyList();
        }

        // What's the best that we can do with the desired range?
        Range<Long> closestInterval = closestInterval(availableRange, desiredRange);

        List<Range<Long>> results = new ArrayList<>();
        if (closestInterval.getMin() < displayedRange.getMin()) {
            if (closestInterval.getMax() < displayedRange.getMin()) {
                results.add(closestInterval);
            } else if (closestInterval.getMax() < displayedRange.getMax()) {
                results.add(new Range<>(closestInterval.getMin(), displayedRange.getMin()));
            } else {
                results.add(new Range<>(closestInterval.getMin(), displayedRange.getMin()));
                results.add(new Range<>(displayedRange.getMax(), closestInterval.getMax()));
            }
        } else if (closestInterval.getMin() < displayedRange.getMax()) {
            if (closestInterval.getMax() < displayedRange.getMax()) {
                // nothing to do here
            } else if (closestInterval.getMax() > displayedRange.getMax()) {
                results.add(new Range<>(displayedRange.getMax(), closestInterval.getMax()));
            }
        } else {
            results.add(closestInterval);
        }
        return results;
    }

    private static boolean contains(Range<Long> larger, Range<Long> smaller) {
        return (larger.getMin() <= smaller.getMin()) && (larger.getMax() >= smaller.getMax());
    }

    private static Range<Long> closestInterval(Range<Long> avilable, Range<Long> desired) {
        long min = Math.max(avilable.getMin(), desired.getMin());
        long max = Math.min(avilable.getMax(), desired.getMax());
        return new Range<>(min, max);
    }

}
