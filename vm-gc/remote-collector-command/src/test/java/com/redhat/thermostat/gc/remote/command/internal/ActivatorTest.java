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

package com.redhat.thermostat.gc.remote.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.gc.remote.command.GCRequestReceiver;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ActivatorTest {

    private StubBundleContext context;
    private Activator activator;

    @Before
    public void setup() {
        context = new StubBundleContext();
        activator = new Activator();
    }

    @Test
    public void verifyReceiverRegistered() throws Exception {
        MXBeanConnectionPool mxBeanConnectionPool = mock(MXBeanConnectionPool.class);
        context.registerService(MXBeanConnectionPool.class, mxBeanConnectionPool, null);

        activator.start(context);

        assertEquals(2, context.getAllServices().size());
        assertTrue(context.isServiceRegistered(RequestReceiver.class.getName(), GCRequestReceiver.class));

        activator.stop(context);
    }

    @Test
    public void verifyActivatorDoesNotRegisterServiceOnMissingDeps() throws Exception {
        activator.start(context);

        assertEquals(0, context.getAllServices().size());

        activator.stop(context);
    }
}
