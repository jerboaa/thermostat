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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.test.StubBundleContext;
import com.redhat.thermostat.tools.cli.ShellCommand;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FrameworkUtil.class)
public class ActivatorTest {

    @Test
    public void testCommandsRegistered() throws Exception {
        /*
         * ServiceLoader pulls in all commands from bundles on the classpath.
         * During maven builds the tools bundle is on the classpath which uses
         * the no-arg constructor of ShellCommand. That in turn uses
         * FrameworkUtil. We need to mock FrameworkUtil here in order to avoid
         * that throwing NPEs.
         */
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(ShellCommand.class)).thenReturn(mockBundle);
        StubBundleContext ctx = new StubBundleContext();
        when(mockBundle.getBundleContext()).thenReturn(ctx);
        Activator activator = new Activator();
        
        activator.start(ctx);
        
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), DumpHeapCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), FindObjectsCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), FindRootCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), ListHeapDumpsCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), ObjectInfoCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), SaveHeapDumpToFileCommand.class));
        assertTrue(ctx.isServiceRegistered(Command.class.getName(), ShowHeapHistogramCommand.class));
    }
}
