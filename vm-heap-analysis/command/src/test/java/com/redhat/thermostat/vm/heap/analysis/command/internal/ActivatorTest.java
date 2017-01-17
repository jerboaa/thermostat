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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import com.redhat.thermostat.testutils.StubBundleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.redhat.thermostat.testutils.Asserts.assertCommandIsRegistered;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class})
public class ActivatorTest {

    @Test
    public void testCommandsRegistered() throws Exception {
        StubBundleContext ctx = new StubBundleContext();
        
        makeConstructorsHappy(ctx);
        
        Activator activator = new Activator();
        
        activator.start(ctx);

        assertCommandIsRegistered(ctx, "dump-heap", DumpHeapCommand.class);
        assertCommandIsRegistered(ctx, "find-objects", FindObjectsCommand.class);
        assertCommandIsRegistered(ctx, "find-root", FindRootCommand.class);
        assertCommandIsRegistered(ctx, "list-heap-dumps", ListHeapDumpsCommand.class);
        assertCommandIsRegistered(ctx, "object-info", ObjectInfoCommand.class);
        assertCommandIsRegistered(ctx, "save-heap-dump-to-file", SaveHeapDumpToFileCommand.class);
        assertCommandIsRegistered(ctx, "show-heap-histogram", ShowHeapHistogramCommand.class);

        activator.stop(ctx);
        
        assertEquals(0, ctx.getAllServices().size());
    }

    private void makeConstructorsHappy(StubBundleContext ctx) {
        // Commands' no-arg constructors use FrameworkUtil to get
        // the bundle context. This results in NPEs when those Command classes
        // are created
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(mockBundle);
        when(mockBundle.getBundleContext()).thenReturn(ctx);
        // StubBundleContext.createFilter() needs if FrameworkUtil.createFilter
        // to work, so fix that.
        try {
            when(FrameworkUtil.createFilter(any(String.class))).thenCallRealMethod();
        } catch (InvalidSyntaxException e) {
            throw new AssertionError("mocked method threw invalid syntax exception");
        }
    }
}

