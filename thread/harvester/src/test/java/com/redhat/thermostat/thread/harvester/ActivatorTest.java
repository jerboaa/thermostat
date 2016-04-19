/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ActivatorTest {

    private Bundle bundle;
    private Version version;
    private WriterID writerId;
    private ThreadDao threadDao;

    @Before
    public void setUp() {
        version = new Version("0.1.2");

        bundle = mock(Bundle.class);
        when(bundle.getVersion()).thenReturn(version);

        writerId = mock(WriterID.class);

        threadDao = mock(ThreadDao.class);
    }

    @Ignore("Activator assumes that Harvester is always registered and fails with NullPointerException")
    @Test
    public void verifyThreadCountUpdaterIsRegistered() throws Exception {
        StubBundleContext bundleContext = new StubBundleContext();
        bundleContext.setBundle(bundle);

        bundleContext.registerService(WriterID.class, writerId, null);
        bundleContext.registerService(ThreadDao.class, threadDao, null);

        Activator activator = new Activator();

        activator.start(bundleContext);

        assertTrue(bundleContext.isServiceRegistered(VmUpdateListener.class.getName(), ThreadCountBackend.class));

        activator.stop(bundleContext);
    }
}
