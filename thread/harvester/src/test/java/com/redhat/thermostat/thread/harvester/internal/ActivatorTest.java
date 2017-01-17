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

package com.redhat.thermostat.thread.harvester.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ActivatorTest {

    @Test
    public void verifyActivatorDoesNotRegisterServiceOnMissingDeps() throws Exception {
        StubBundleContext context = new StubBundleContext();

        Activator activator = new Activator();

        activator.start(context);

        assertEquals(0, context.getAllServices().size());
        assertEquals(10, context.getServiceListeners().size());

        activator.stop(context);
    }

    @Test
    public void verifyActivatorRegistersServices() throws Exception {
        StubBundleContext context = new StubBundleContext() {
            @Override
            public Bundle getBundle() {
                Bundle result = mock(Bundle.class);
                when(result.getVersion()).thenReturn(Version.emptyVersion);
                return result;
            }
        };

        BackendService backendService = mock(BackendService.class);
        WriterID idService = mock(WriterID.class);
        ThreadDao threadDao = mock(ThreadDao.class);
        LockInfoDao lockInfoDao = mock(LockInfoDao.class);
        MXBeanConnectionPool mxBeanConnectionPool = mock(MXBeanConnectionPool.class);

        context.registerService(BackendService.class.getName(), backendService, null);
        context.registerService(WriterID.class, idService, null);
        context.registerService(ThreadDao.class, threadDao, null);
        context.registerService(LockInfoDao.class, lockInfoDao, null);
        context.registerService(MXBeanConnectionPool.class, mxBeanConnectionPool, null);

        Activator activator = new Activator();

        activator.start(context);

        assertTrue(context.isServiceRegistered(Backend.class.getName(), ThreadCountBackend.class));
        assertTrue(context.isServiceRegistered(Backend.class.getName(), LockInfoBackend.class));
        assertTrue(context.isServiceRegistered(Backend.class.getName(), ThreadBackend.class));

        assertEquals(8, context.getAllServices().size());
        assertEquals(10, context.getServiceListeners().size());

        activator.stop(context);

        assertEquals(5, context.getAllServices().size());
        assertEquals(0, context.getServiceListeners().size());
    }
}
