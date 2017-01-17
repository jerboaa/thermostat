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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

public class LogFileStatsTest {

    /*
     * bucket names returned by filters need to be unique.
     */
    @Test
    public void verifyDuplicateRegisterFilterThrowsException() {
        LineStatsFilter<?, ?> filter = mock(LineStatsFilter.class);
        String bucketName = "foo-bar";
        when(filter.getBucketName()).thenReturn(bucketName);
        LogFileStats stats = new LogFileStats(new SharedStatementState(), null);
        try {
            stats.registerStatsFilter(filter);
        } catch (IllegalFilterException e) {
            fail(e.getMessage());
        }
        try {
            stats.registerStatsFilter(filter);
            fail("Expected IllegalFilterException");
        } catch (IllegalFilterException e) {
            // pass
            assertEquals("bucket name 'foo-bar' already taken", e.getMessage());
        }
    }
    
    @Test
    public void canRegisterMultipleUniqueFilters() throws IllegalFilterException {
        LineStatsFilter<?, ?> filter = mock(LineStatsFilter.class);
        when(filter.getBucketName()).thenReturn("foo-bar");
        LineStatsFilter<?, ?> filterOther = mock(LineStatsFilter.class);
        when(filterOther.getBucketName()).thenReturn("other-than-foo-bar");
        LogFileStats stats = new LogFileStats(new SharedStatementState(), null);
        stats.registerStatsFilter(filter);
        stats.registerStatsFilter(filterOther);
        List<LineStatsFilter<?, ?>> actualFilterList = stats.getRegisteredFilters();
        assertEquals(2, actualFilterList.size());
        assertSame(filter, actualFilterList.get(0));
        assertSame(filterOther, actualFilterList.get(1));
    }
}
