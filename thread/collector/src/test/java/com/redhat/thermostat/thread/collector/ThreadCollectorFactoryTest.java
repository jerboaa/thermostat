/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.thread.collector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.collector.impl.ThreadCollectorFactoryImpl;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ThreadCollectorFactoryTest {

    @Test
    public void testThreadCollectorFactory() {
        ThreadDao threadDao = mock(ThreadDao.class);
        VmRef reference = mock(VmRef.class);
        
        ScheduledExecutorService threadPool = mock(ScheduledExecutorService.class);
        
        ThreadCollectorFactory factory = new ThreadCollectorFactoryImpl(threadDao, threadPool);
        ThreadCollector collector = factory.getCollector(reference);
        assertNotNull(collector);
        
        // ask again, it must be the same instance as before
        ThreadCollector collector2 = factory.getCollector(reference);
        assertSame(collector, collector2);
        
        // and now of course it must be different
        VmRef reference2 = mock(VmRef.class);
        ThreadCollector collector3 = factory.getCollector(reference2);
        assertNotSame(collector, collector3);
    }
}
