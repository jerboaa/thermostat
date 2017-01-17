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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class MXBeanConnectionPoolEntryTest {
    
    private MXBeanConnectionPoolEntry entry;
    private CountDownLatch urlLatch;

    @Before
    public void setUp() throws Exception {
        urlLatch = mock(CountDownLatch.class);
        when(urlLatch.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        entry = new MXBeanConnectionPoolEntry(8000, urlLatch);
    }

    @Test
    public void testSetJmxUrl() throws Exception {
        entry.setJmxUrl("jmxUrl");
        verify(urlLatch).countDown();
        
        String result = entry.getJmxUrlOrBlock();
        assertEquals("jmxUrl", result);
        verify(urlLatch).await(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testSetException() throws Exception {
        Exception ex = new Exception("TEST");
        entry.setException(ex);
        verify(urlLatch).countDown();
        
        try {
            entry.getJmxUrlOrBlock();
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals(ex, e.getCause());
            verify(urlLatch).await(anyLong(), any(TimeUnit.class));
        }
    }
    
    @Test(expected=IOException.class)
    public void testGetJmxUrlTimeout() throws Exception {
        when(urlLatch.await(anyLong(), any(TimeUnit.class))).thenReturn(false);
        entry.getJmxUrlOrBlock();
    }

}
