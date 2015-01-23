/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;

public class ActivatorTest {

    @Test
    public void testStartStopWithoutDependencies() {
        StubBundleContext ctx = new StubBundleContext();

        Activator activator = new Activator();

        activator.start(ctx);

        assertEquals(0, ctx.getAllServices().size());

        activator.stop(ctx);

        assertEquals(0, ctx.getAllServices().size());
        assertEquals(0, ctx.getServiceListeners().size());
    }

    @Test
    public void testStartStopWithDependency() throws Exception {
        StubBundleContext ctx = new StubBundleContext();

        HeapDAO heapDao = mock(HeapDAO.class);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        WriterID writerId = mock(WriterID.class);
        ctx.registerService(HeapDAO.class, heapDao, null);
        ctx.registerService(MXBeanConnectionPool.class, pool, null);
        ctx.registerService(WriterID.class, writerId, null);

        Activator activator = new Activator();

        activator.start(ctx);

        assertTrue(ctx.isServiceRegistered(RequestReceiver.class.getName(), HeapDumpReceiver.class));

        assertEquals(3, ctx.getServiceListeners().size());

        activator.stop(ctx);

        assertFalse(ctx.isServiceRegistered(RequestReceiver.class.getName(), HeapDumpReceiver.class));

        assertEquals(0, ctx.getServiceListeners().size());
    }

}

