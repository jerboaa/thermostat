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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.web.server.CursorManager.CursorHolder;
import com.redhat.thermostat.web.server.CursorManager.CursorSweeper;
import com.redhat.thermostat.web.server.CursorManager.CursorTimer;

public class CursorManagerTest {
    
    /**
     * Test putting cursors which return true on hasNext()
     * those cursors need to get tracked.
     */
    @Test
    public void testPutBasicCursorHasMore() {
        CursorManager manager = new CursorManager(mock(TimerRegistry.class));
        int id = manager.put(getHasMoreBatchCursor());
        assertTrue(id >= 0);
        assertEquals(0, id);
        id = manager.put(getHasMoreBatchCursor());
        assertTrue(id != 0);
        assertEquals(1, id);
    }
    
    /**
     * Verifies that cursor IDs won't overflow.
     */
    @Test
    public void testPutCursorOverflow() {
        int startValue = Integer.MAX_VALUE - 5;
        // construct a cursor manager with high enough counter to simulate
        // overflow.
        CursorManager manager = new CursorManager(startValue);
        int numIterations = 10;
        boolean rollOver = false;
        for (int i = 0, id; i < numIterations; i++) {
            id = manager.put(getHasMoreBatchCursor());
            assertTrue(id >= 0);
            assertFalse(id == Integer.MAX_VALUE);
            if (id == 0) {
                rollOver = true;
            }
        }
        assertTrue("Expected id to start again from 0", rollOver);
    }
    
    /**
     * Test putting a cursor which returns false on hasNext().
     * CursorManager should not add such cursors and should
     * return CURSOR_NOT_STORED.
     */
    @Test
    public void testPutNoHasMoreCursor() {
        CursorManager manager = new CursorManager(mock(TimerRegistry.class));
        int id = manager.put(mock(Cursor.class));
        assertEquals(CursorManager.CURSOR_NOT_STORED, id);
    }
    
    private Cursor<?> getHasMoreBatchCursor() {
        Cursor<?> c = mock(Cursor.class);
        when(c.hasNext()).thenReturn(true);
        return c;
    }
    
    @Test
    public void testGetInvalidId() {
        CursorManager manager = new CursorManager(mock(TimerRegistry.class));
        Cursor<?> c = manager.get(CursorManager.CURSOR_NOT_STORED);
        assertNull(c);
    }
    
    /**
     * Basic test for CursorManager.get(). Add some cursors,
     * then add one we track the id for and verify that we
     * can get the BatchCursor reference.
     */
    @Test
    public void testGetHasMore() {
        CursorManager manager = new CursorManager(mock(TimerRegistry.class));
        int num = (int)(Math.random() * 300);
        addCursors(num, manager);
        Cursor<?> cursor = getHasMoreBatchCursor();
        int interestingId = manager.put(cursor);
        num = (int)(Math.random() * 40);
        addCursors(num, manager);
        Cursor<?> actual = manager.get(interestingId);
        assertSame(actual, cursor);
    }
    
    @Test
    public void testExpireCursors() {
        Map<Integer, CursorHolder> cursors = new HashMap<>();
        long expiredTime = System.currentTimeMillis() - ( 5 * 60 * 1000);
        long notExpiredTime = System.currentTimeMillis();
        cursors.put(3, new CursorHolder(mock(Cursor.class), expiredTime));
        cursors.put(4, new CursorHolder(mock(Cursor.class), expiredTime));
        cursors.put(5, new CursorHolder(mock(Cursor.class), notExpiredTime));
        cursors.put(7, new CursorHolder(mock(Cursor.class), expiredTime));
        CursorManager manager = new CursorManager(mock(TimerRegistry.class), cursors);
        manager.expireCursors(); // should remove old cursors
        assertEquals(1, cursors.keySet().size());
        assertNotNull(cursors.get(5));
    }
    
    @Test
    public void testRemoveCursor() {
        Map<Integer, CursorHolder> cursors = new HashMap<>();
        cursors.put(3, new CursorHolder(mock(Cursor.class), 3));
        cursors.put(4, new CursorHolder(mock(Cursor.class), 4));
        cursors.put(5, new CursorHolder(mock(Cursor.class), 5));
        CursorManager manager = new CursorManager(mock(TimerRegistry.class), cursors);
        manager.removeCursor(3);
        assertEquals(2, cursors.keySet().size());
        assertNotNull(cursors.get(4));
        assertNotNull(cursors.get(5));
        assertNull(cursors.get(3));
    }
    
    @Test
    public void testUpdateCursorTimeStamp() {
        Map<Integer, CursorHolder> cursors = new HashMap<>();
        long expiredTime = System.currentTimeMillis() - ( 5 * 60 * 1000);
        long notExpiredTime = System.currentTimeMillis();
        cursors.put(4, new CursorHolder(mock(Cursor.class), expiredTime));
        cursors.put(5, new CursorHolder(mock(Cursor.class), notExpiredTime));
        cursors.put(7, new CursorHolder(mock(Cursor.class), expiredTime));
        CursorManager manager = new CursorManager(mock(TimerRegistry.class), cursors);
        // refresh 4's timestamp so that it's now no longer expired
        manager.updateCursorTimeStamp(4);
        manager.expireCursors();
        assertEquals(2, cursors.keySet().size());
        assertNotNull("4 has been updated and is thus still alive", cursors.get(4));
        assertNotNull("5 was expired from the outset", cursors.get(5));
    }
    
    @Test
    public void canStartSweeperTimerViaManager() {
        TimerRegistry registry = mock(TimerRegistry.class);
        CursorManager manager = new CursorManager(registry);
        manager.startSweeperTimer();
        ArgumentCaptor<CursorTimer> timerCaptor = ArgumentCaptor.forClass(CursorTimer.class);
        verify(registry).registerTimer(timerCaptor.capture());
        CursorTimer timer = timerCaptor.getValue();
        assertNotNull("expected non-null timer => timer thread started", timer);
        assertTrue("startSweeperTimer() expected to schedule task", timer.taskScheduled);
    }
    
    private void addCursors(int num, CursorManager manager) {
        for (int i = 0; i < num; i++) {
            manager.put(getHasMoreBatchCursor());
        }
    }
    
    // CursorTimer tests
    
    @Test
    public void testCursorTimerScheduleTask() {
        TimerTask timerTask = mock(TimerTask.class);
        Timer timer = mock(Timer.class);
        CursorTimer cursorTimer = new CursorTimer(timerTask, timer);
        long threeMinutes = 3 * 60 * 1000;
        cursorTimer.scheduleTask();
        verify(timer).scheduleAtFixedRate(timerTask, 0, threeMinutes);
    }
    
    @Test
    public void testCursorTimerStop() {
        TimerTask timerTask = mock(TimerTask.class);
        Timer timer = mock(Timer.class);
        CursorTimer cursorTimer = new CursorTimer(timerTask, timer);
        cursorTimer.stop();
        verify(timer).cancel();
    }
    
    // CursorHolder tests
    
    @Test
    public void testUpdateTimeStamp() {
        long now = System.currentTimeMillis();
        CursorHolder holder = new CursorHolder(mock(Cursor.class), now);
        sleep(10);
        holder.updateTimestamp();
        assertTrue(now != holder.getLastUpdated());
    }
    
    @Test
    public void testCheckIsCursorExpired() {
        long now = 0;
        CursorHolder holder = new CursorHolder(mock(Cursor.class), now);
        long laterTime = 10;
        assertFalse("cursor still valid. timeout == 20ms, but only 10ms old.",
                holder.checkIsCursorExpired(laterTime, 20));
        assertTrue("cursor older than 5 milliseconds", holder.checkIsCursorExpired(laterTime, 5));
    }
    
    @Test
    public void testGetCursor() {
        long now = System.currentTimeMillis();
        Cursor<?> cursor = mock(Cursor.class);
        CursorHolder holder = new CursorHolder(cursor, now);
        assertSame(cursor, holder.getCursor());
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    // CursorSweeper tests
    
    public void testCursorSweeper() {
        CursorManager manager = mock(CursorManager.class);
        CursorSweeper sweeper = new CursorSweeper(manager);
        sweeper.run();
        verify(manager).expireCursors();
    }

}
