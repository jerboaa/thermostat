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

package com.redhat.thermostat.vm.profiler.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;

public class ActivatorTest {

    @Test
    public void requestHandlerIsRegistered() throws Exception {
        File homeFile = mock(File.class);
        when(homeFile.getAbsolutePath()).thenReturn("${thermostat.home}");
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemThermostatHome()).thenReturn(homeFile);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        WriterID writerService = mock(WriterID.class);
        ProfileDAO dao = mock(ProfileDAO.class);

        StubBundleContext context = new StubBundleContext();
        context.registerService(CommonPaths.class, paths, null);
        context.registerService(MXBeanConnectionPool.class, pool, null);
        context.registerService(ProfileDAO.class, dao, null);
        context.registerService(WriterID.class, writerService, null);

        Activator activator = new Activator();

        activator.start(context);

        assertTrue(context.isServiceRegistered(RequestReceiver.class.getName(), ProfilerRequestReceiver.class));
        assertTrue(context.isServiceRegistered(VmStatusListener.class.getName(), ProfilerVmStatusListener.class));

        activator.stop(context);

        assertFalse(context.isServiceRegistered(RequestReceiver.class.getName(), ProfilerRequestReceiver.class));
        assertFalse(context.isServiceRegistered(VmStatusListener.class.getName(), ProfilerVmStatusListener.class));
    }
}
