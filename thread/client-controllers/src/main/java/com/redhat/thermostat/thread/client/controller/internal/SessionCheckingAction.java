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

import com.redhat.thermostat.thread.model.SessionID;

/**
 */
public abstract class SessionCheckingAction implements Runnable {

    private SessionID lastSession;
    private long lastUpdate;

    protected abstract SessionID getCurrentSessionID();
    protected abstract SessionID getLastAvailableSessionID();

    @Override
    public void run() {
        SessionID session = getCurrentSessionID();
        if (session == null) {
            // no session selected, but let's try to default to the last
            // available
            session = getLastAvailableSessionID();
            if (session == null) {
                // ok, really no data, let's skip this round
                return;
            }
        }

        if (lastSession == null || !session.get().equals(lastSession.get())) {
            onNewSession();
        }
        lastSession = session;

        actionPerformed(session);
    }

    protected void onNewSession() {}
    protected void actionPerformed(SessionID session) {}
}
