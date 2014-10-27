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

package com.redhat.thermostat.storage.core.experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.redhat.thermostat.storage.model.Pojo;

public class BasicBatchCursorTest {
    
    @Test
    public void testSetBatchSize() {
        BasicBatchCursor<TestPojo> cursor = new BasicBatchCursorImpl<>();
        try {
            cursor.setBatchSize(-1);
            fail("expected IAE for batch size of -1");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        try {
            cursor.setBatchSize(0);
            fail("expected IAE for batch size of 0");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        cursor.setBatchSize(BatchCursor.DEFAULT_BATCH_SIZE);
        assertEquals(BatchCursor.DEFAULT_BATCH_SIZE, cursor.getBatchSize());
    }
    
    @Test
    public void testGetBatchSize() {
        BasicBatchCursor<TestPojo> cursor = new BasicBatchCursorImpl<>();
        assertNotNull("should always return default if never set", cursor.getBatchSize());
        assertEquals(BatchCursor.DEFAULT_BATCH_SIZE, cursor.getBatchSize());
        cursor.setBatchSize(3);
        assertEquals(3, cursor.getBatchSize());
    }

    private static class BasicBatchCursorImpl<T extends Pojo> extends BasicBatchCursor<T> {

        @Override
        public boolean hasNext() {
            // not implemented
            return false;
        }

        @Override
        public T next() {
            // not implemented
            return null;
        }
        
    }
    
    private static class TestPojo implements Pojo {
        // nothing
    }
}
