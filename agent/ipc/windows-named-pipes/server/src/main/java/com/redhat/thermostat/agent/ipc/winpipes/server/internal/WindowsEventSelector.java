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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesNativeHelper;
import com.redhat.thermostat.common.utils.LoggingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * WindowsEventSelector will keep track of a number of selectables (Windows event handles) and wait for one of them to activate
 */
class WindowsEventSelector {

    interface EventHandler {
        long getHandle();
        void processEvent() throws IOException;
    }

    private static final Logger logger = LoggingUtils.getLogger(WindowsEventSelector.class);

    private static final WinPipesNativeHelper helper = WinPipesNativeHelper.INSTANCE;

    private final Set<EventHandler> eventHandlers;

    private ArrayList<EventHandler> ehArray;

    // use donly by select() call
    private long[] eventHandles = null;

    WindowsEventSelector(int maxInstances) {
        eventHandlers = new HashSet<>(maxInstances);
    }

    void add(EventHandler e) {
        eventHandlers.add(e);
        fixArray();
    }

    void remove(EventHandler e) {
        eventHandlers.remove(e);
        fixArray();
    }

    /**
     * create an array oif handlers and handles suitable for a call to waitForMultipleObjects()
     */
    private void fixArray() {
        ehArray = new ArrayList<>(eventHandlers.size());
        for (EventHandler instance : eventHandlers) {
            ehArray.add(instance);
        }
        eventHandles = new long[ehArray.size()];
        for (int i = 0; i < ehArray.size(); i++) {
            eventHandles[i] = ehArray.get(i).getHandle();
        }
    }

    /**
     * AKA select() for events
     * @return handler for event that was raied
     * @throws IOException is there was an issue during the call
     */
    EventHandler waitForEvent() throws IOException {
        logger.finest("WinPipe waiting for one of " + eventHandles.length + " events");
        final int pipeNum = helper.waitForMultipleObjects(eventHandles.length, eventHandles, false, (int)WinPipesNativeHelper.INFINITE) - (int)WinPipesNativeHelper.WAIT_OBJECT_0;
        if (pipeNum >= 0 && pipeNum < eventHandles.length) {
            logger.finest("WinPipe got event on handle " + ehArray.get(pipeNum) + " err=" + helper.getLastError());
            return ehArray.get(pipeNum);
        } else {
            final String msg = "WinPipe waitForMultipleObjects returned " + pipeNum + " err=" + helper.getLastError();
            logger.info(msg);
            throw new IOException(msg);
        }
    }
}
