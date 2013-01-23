/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.killvm.agent.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.killvm.agent.internal.Activator;
import com.redhat.thermostat.killvm.agent.internal.KillVmReceiver;
import com.redhat.thermostat.service.process.UNIXProcessHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest(OSGIUtils.class)
public class ActivatorTest {

    /**
     * Makes sure receiver is registered and unix service gets set.
     * 
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void startStopTest() throws Exception {
        OSGIUtils utils = mock(OSGIUtils.class);
        PowerMockito.mockStatic(OSGIUtils.class);
        when(OSGIUtils.getInstance()).thenReturn(utils);
        BundleContext ctx = mock(BundleContext.class);
        ServiceRegistration serviceReg = mock(ServiceRegistration.class);
        when(ctx.registerService(anyString(), any(), any(Dictionary.class))).thenReturn(serviceReg);
        Activator activator = new Activator();
        activator.start(ctx);
        verify(utils).getService(UNIXProcessHandler.class);
        verify(ctx).registerService(eq(RequestReceiver.class.getName()), isA(KillVmReceiver.class), any(Dictionary.class));
        activator.stop(ctx);
        verify(serviceReg).unregister();
    }

}

