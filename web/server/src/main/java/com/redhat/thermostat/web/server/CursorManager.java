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

package com.redhat.thermostat.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import com.redhat.thermostat.storage.core.experimental.BatchCursor;

/**
 * Manages (query) cursors for a single user.
 * 
 */
final class CursorManager {

    public static final int CURSOR_NOT_STORED = -1;
    private final Map<Integer, CursorHolder> cursors;
    private final TimerRegistry registry;
    private int cursorIdCounter;
    
    // test-only
    CursorManager(TimerRegistry registry, Map<Integer, CursorHolder> cursors) {
        this.registry = registry;
        this.cursors = cursors;
    }
    
    CursorManager(TimerRegistry registry) {
        this.cursors = new HashMap<>();
        this.registry = registry;
    }
    
    // test-only
    CursorManager(int cursorIdCounter) {
        this(null);
        this.cursorIdCounter = cursorIdCounter;
    }
    
    /**
     * Add a cursor to the map we know of if it has more results.
     * 
     * @param cursor The potential candidate to add to the state we keep track
     *               of.
     * @return The cursor ID or {@link CursorManager#CURSOR_NOT_STORED} if the
     *         passed in cursor has no more elements.
     */
    synchronized int put(final BatchCursor<?> cursor) {
        int cursorId = CURSOR_NOT_STORED;
        if (cursor.hasNext()) {
            // Be sure we don't overflow. For a long running web storage we
            // could potentially run out of id's for a single user. However,
            // the time between 0 and Integer.MAX_VALUE should be sufficiently
            // large so that any given cursor expires before the id will get
            // reused.
            if (cursorIdCounter == Integer.MAX_VALUE) {
                cursorIdCounter = 0; // start again from 0
            }
            cursorId = cursorIdCounter;
            cursors.put(cursorId, new CursorHolder(cursor, System.currentTimeMillis()));
            cursorIdCounter++;
        }
        return cursorId;
    }
    
    synchronized BatchCursor<?> get(int cursorId) {
        CursorHolder holder = cursors.get(cursorId);
        if (holder == null) {
            return null;
        }
        return holder.getCursor();
    }
    
    synchronized void updateCursorTimeStamp(int cursorId) {
        CursorHolder holder = cursors.get(cursorId);
        if (holder == null) {
            return;
        }
        holder.updateTimestamp();
    }
    
    synchronized void removeCursor(int cursorId) {
        cursors.remove(cursorId);
    }
    
    synchronized void expireCursors() {
        final long currentTime = System.currentTimeMillis();
        List<Integer> expiredCursors = new ArrayList<>();
        for (Entry<Integer, CursorHolder> entry: cursors.entrySet()) {
            CursorHolder holder = entry.getValue();
            if (holder.isCursorExpired(currentTime)) {
                expiredCursors.add(entry.getKey());
            }
        }
        for (Integer expiredKey: expiredCursors) {
            cursors.remove(expiredKey);
        }
    }
    
    void startSweeperTimer() {
        CursorTimer sweeperTimer = new CursorTimer(new CursorSweeper(this));
        sweeperTimer.scheduleTask();
        registry.registerTimer(sweeperTimer);
    }
    
    /**
     * 
     * A container in order save and track cursors. This holder enables
     * expiring cursors, as well as extending the liveness of a cursor.
     *
     */
    static class CursorHolder {

        private static final int MINUTES = 1000 * 60;
        // The time out in minutes
        static final int TIMEOUT = 3 * MINUTES;
        
        private final BatchCursor<?> cursor;
        private long lastUpdated;
        
        CursorHolder(BatchCursor<?> cursor, long lastUpdated) {
            this.cursor = cursor;
            this.lastUpdated = lastUpdated;
        }
        
        void updateTimestamp() {
            this.lastUpdated = System.currentTimeMillis();
        }
        
        boolean isCursorExpired(long currentTime) {
            return checkIsCursorExpired(currentTime, TIMEOUT);
        }
        
        BatchCursor<?> getCursor() {
            return cursor;
        }
        
        // here in order to facilitate testing
        boolean checkIsCursorExpired(long currentTime, final int timeoutInMillis) {
            return lastUpdated < (currentTime - timeoutInMillis);
        }
        
        // test-only
        long getLastUpdated() {
            return lastUpdated;
        }
    }
    
    /**
     * {@link TimerTask} which times out cursors.
     */
    static class CursorSweeper extends TimerTask {
        
        private final CursorManager manager;
        
        CursorSweeper(CursorManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            manager.expireCursors();
        }

    }
    
    static class CursorTimer implements StoppableTimer {

        private static final String NAME = NAME_PREFIX + "cursor-manager";
        private final Timer timer;
        private final TimerTask task;
        boolean taskScheduled = false;
        
        CursorTimer(TimerTask task) {
            this(task, new Timer(NAME));
        }
        
        // for testing
        CursorTimer(TimerTask task, Timer timer) {
            this.timer = timer;
            this.task = task;
        }
        
        void scheduleTask() {
            timer.scheduleAtFixedRate(task, 0, CursorHolder.TIMEOUT);
            taskScheduled = true;
        }

        @Override
        public void stop() {
            timer.cancel();
        }
        
    }
    
}
