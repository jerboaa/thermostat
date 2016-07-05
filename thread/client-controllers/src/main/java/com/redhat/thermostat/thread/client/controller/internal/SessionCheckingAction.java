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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.model.SessionID;

import java.util.concurrent.TimeUnit;

/**
 */
public abstract class SessionCheckingAction implements Runnable {

    private SessionID lastSession;
    private long lastUpdate;

    public SessionCheckingAction() {
        lastUpdate  = getTimeDeltaOnNewSession();
    }

    protected long getTimeDeltaOnNewSession() {
        return System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
    }

    /**
     * Returns the sessions ID the user wants to track, which can be null.
     */
    protected abstract SessionID getCurrentSessionID();

    /**
     * Returns the last available sessions ID. This is typically the sessions
     * with the most recent timestamp from a set of sessions.
     */
    protected abstract SessionID getLastAvailableSessionID() ;

    /**
     * Returns the sessions id to track by this action, which is either the
     * session returned by {@link #getCurrentSessionID()} or, if null,
     * the one returned by {@link #getLastAvailableSessionID()}.
     */
    protected SessionID getSessionID() {
        SessionID session = getCurrentSessionID();
        if (session == null) {
            // no session selected, but let's try to default to the last
            // available
            session = getLastAvailableSessionID();
        }
        return session;
    }


    /**
     * The actual action to perform.
     */
    protected abstract void actionPerformed(SessionID session,
                                            Range<Long> range,
                                            Range<Long> totalRange);

    protected abstract Range<Long> getTotalRange(SessionID session);

    protected Range<Long> getRange(SessionID session, Range<Long> totalRange) {
        if (totalRange == null) {
            // there is range to check
            return null;
        }

        if (lastUpdate >= totalRange.getMax()) {
            // we already covered this range
            return null;
        }

        Range<Long> result = new Range<>(lastUpdate, totalRange.getMax());
        lastUpdate = totalRange.getMax();
        return result;
    }

    protected boolean isRangeValid(Range<Long> range, Range<Long> totalRange) {
        return totalRange != null && range != null;
    }

    protected boolean isSessionValid(SessionID session) {
        return session != null;
    }

    /**
     * Determine if the session in input is a new session. By default the
     * implementation checks the sessions against the last session.
     */
    protected boolean isNewSession(SessionID session) {
        boolean result = (lastSession == null || !session.get().equals(lastSession.get()));
        lastSession = session;
        return result;
    }

    /**
     * Called when a new session is detected, as defined by
     * {@link #isNewSession(SessionID)}.
     */
    protected void onNewSession() {}

    @Override
    public void run() {
        SessionID session = getSessionID();
        if (!isSessionValid(session)) {
            return;
        }

        if (isNewSession(session)) {
            lastUpdate = getTimeDeltaOnNewSession();
            onNewSession();
        }

        Range<Long> totalRange = getTotalRange(session);
        Range<Long> range = getRange(session, totalRange);
        if (isRangeValid(range, totalRange)) {
            actionPerformed(session, range, totalRange);
        }
    }
}
