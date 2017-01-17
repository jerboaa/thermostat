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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.model.SessionID;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 */
public class SessionCheckingActionTest {

    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);

    @Test
    public void getRangeOnlyReturnsDelta() throws Exception {

        final long[] delta = new long[1];
        final Range<Long> [] results = new Range [2];

        SessionCheckingAction action = new SessionCheckingAction() {

            @Override
            protected Range<Long> getTotalRange(SessionID session) {
                Range<Long> range = new Range<>(0l, delta[0] + ONE_HOUR);
                return range;
            }

            @Override
            protected boolean isNewSession(SessionID session) {
                return false;
            }

            @Override
            protected boolean isSessionValid(SessionID session) {
                return true;
            }

            @Override
            protected long getTimeDeltaOnNewSession() {
                return 0;
            }

            @Override
            protected SessionID getCurrentSessionID() {
                return null;
            }

            @Override
            protected SessionID getLastAvailableSessionID() {
                return null;
            }

            @Override
            protected void actionPerformed(SessionID session, Range<Long> range, Range<Long> totalRange) {
                results[0] = range;
                results[1] = totalRange;
            }
        };

        action.run();

        Range<Long> range = results[0];
        Range<Long> totalRange = results[1];

        // the first run, we should get the full range
        assertEquals(range, totalRange);

        // simulate an elapsed second
        delta[0] = 1000;
        long lastMax = totalRange.getMax();

        action.run();

        range = results[0];
        totalRange = results[1];

        assertEquals(lastMax, (long) range.getMin());
        assertEquals((long) totalRange.getMax(), (long) range.getMax());
    }
}
