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

package com.redhat.thermostat.common;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.HelpCommand;
import com.redhat.thermostat.common.cli.Launcher;

public class ActivatorTest {

    @Test
    public void testRegisterServices() throws Exception {
        final Map<ServiceRegistration, Object> regs = new HashMap<>();
        BundleContext bCtx = mock(BundleContext.class);
        when(bCtx.registerService(anyString(), any(), any(Dictionary.class))).then(new Answer<ServiceRegistration>() {

            @Override
            public ServiceRegistration answer(InvocationOnMock invocation) throws Throwable {
                ServiceRegistration reg = mock(ServiceRegistration.class);
                when(reg.getReference()).thenReturn(mock(ServiceReference.class));
                regs.put(reg, invocation.getArguments()[1]);
                return reg;
            }
        });
        when(bCtx.getService(isA(ServiceReference.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceReference ref = (ServiceReference) invocation.getArguments()[0];
                for (Entry<ServiceRegistration,Object> registration: regs.entrySet()) {
                    if (registration.getKey().getReference().equals(ref)) {
                        return registration.getValue();
                    }
                }
                return null;
            }
        });

        Activator activator = new Activator();

        activator.start(bCtx);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Command.NAME, "help");
        verify(bCtx).registerService(eq(Command.class.getName()), isA(HelpCommand.class), eq(props));

        verify(bCtx).registerService(eq(Launcher.class.getName()), isA(Launcher.class), any(Dictionary.class));

        activator.stop(bCtx);

        for (ServiceRegistration reg : regs.keySet()) {
            verify(reg).unregister();
        }
    }

}
