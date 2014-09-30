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

package com.redhat.thermostat.storage.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class ThreadPoolSizeRetrieverTest {

    @After
    public void tearDown() {
        System.clearProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE);
        System.clearProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE_UNBOUNDED);
    }
    
    /*
     * No properties set. Should return the default pool size.
     */
    @Test
    public void canGetDefaultPoolSize() {
        ThreadPoolSizeRetriever retriever = new ThreadPoolSizeRetriever();
        assertEquals(ThreadPoolSizeRetriever.DEFAULT_THREAD_POOL_SIZE, retriever.getPoolSize());
    }
    
    /*
     * The pool size may be set via a property. However, if the parsed value
     * from that property is too large, we fall back to the default pool size.
     */
    @Test
    public void getPoolSizeWithTooLargeValue() {
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE, "300");
        ThreadPoolSizeRetriever retriever = new ThreadPoolSizeRetriever();
        try {
            retriever.getPoolSize();
            fail("Pool size capped at 100");
        } catch (InvalidConfigurationException e) {
            // pass
            assertEquals("Value of property com.redhat.thermostat.storage.queue.poolSize: " +
                         "300 > 100 and property com.redhat.thermostat.storage.queue.unbounded unset or set to false", e.getMessage());
        }
    }
    
    @Test
    public void testInvalidPoolSize() {
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE, "-2");
        ThreadPoolSizeRetriever retriever = new ThreadPoolSizeRetriever();
        try {
            retriever.getPoolSize();
            fail("Pool size invalid: <= 0");
        } catch (InvalidConfigurationException e) {
            // pass
            assertEquals("Value of property com.redhat.thermostat.storage.queue.poolSize: -2 <= 0",
                    e.getMessage());
        }
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE, "0");
        try {
            retriever.getPoolSize();
            fail("Pool size invalid: <= 0");
        } catch (InvalidConfigurationException e) {
            // pass
            assertEquals("Value of property com.redhat.thermostat.storage.queue.poolSize: 0 <= 0",
                    e.getMessage());
        }
    }
    
    /*
     * Value in range: > 0 && <= 100
     */
    @Test
    public void validCappedValue() {
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE, "59");
        ThreadPoolSizeRetriever retriever = new ThreadPoolSizeRetriever();
        assertEquals(59, retriever.getPoolSize());
    }
    
    /*
     * Out of range but overriden via unbounded property set to true.
     */
    @Test
    public void validUnboundedValue() {
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE_UNBOUNDED, "true");
        System.setProperty(ThreadPoolSizeRetriever.THREAD_POOL_SIZE, "300");
        ThreadPoolSizeRetriever retriever = new ThreadPoolSizeRetriever();
        assertTrue(retriever.getPoolSize() > ThreadPoolSizeRetriever.DEFAULT_THREAD_POOL_SIZE);
        assertEquals(300, retriever.getPoolSize());
    }
}
