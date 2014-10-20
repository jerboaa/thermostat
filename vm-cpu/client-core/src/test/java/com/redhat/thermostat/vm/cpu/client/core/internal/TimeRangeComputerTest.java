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

package com.redhat.thermostat.vm.cpu.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.model.Range;

public class TimeRangeComputerTest {

    @Test
    public void verifyComputesEmptyResultIfAllDataIsAvaialable() {
        Range<Long> available = new Range<>(1l, 10l);
        Range<Long> desired = new Range<>(2l, 3l);
        Range<Long> displayed = new Range<>(1l, 10l);

        List<Range<Long>> result = TimeRangeComputer.computeAdditionalRangesToFetch(available, desired, displayed);

        assertTrue(result.isEmpty());
    }

    @Test
    public void verifyComputesRangeBeforeCorrectly() {
        Range<Long> available = new Range<>(1l, 10l);
        Range<Long> desired = new Range<>(1l, 7l);
        Range<Long> displayed = new Range<>(5l, 10l);

        List<Range<Long>> result = TimeRangeComputer.computeAdditionalRangesToFetch(available, desired, displayed);

        assertEquals(1, result.size());
        assertEquals(new Range<>(1l,5l), result.get(0));
    }

    @Test
    public void verifyComputesRangeAfterCorrectly() {
        Range<Long> available = new Range<>(1l, 10l);
        Range<Long> desired = new Range<>(2l, 6l);
        Range<Long> displayed = new Range<>(1l, 5l);

        List<Range<Long>> result = TimeRangeComputer.computeAdditionalRangesToFetch(available, desired, displayed);

        assertEquals(1, result.size());
        assertEquals(new Range<>(5l,6l), result.get(0));
    }

    @Test
    public void verifyComputesOverlappingRangeCorrectly() {
        Range<Long> available = new Range<>(1l, 10l);
        Range<Long> desired = new Range<>(2l, 8l);
        Range<Long> displayed = new Range<>(4l, 6l);

        List<Range<Long>> result = TimeRangeComputer.computeAdditionalRangesToFetch(available, desired, displayed);

        assertEquals(2, result.size());
        assertEquals(new Range<>(2l,4l), result.get(0));
        assertEquals(new Range<>(6l, 8l), result.get(1));
    }

}
