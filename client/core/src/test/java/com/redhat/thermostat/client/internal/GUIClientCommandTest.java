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

package com.redhat.thermostat.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.internal.GUIClientCommand;
import com.redhat.thermostat.client.internal.Main;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.osgi.service.ContextAction;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;

public class GUIClientCommandTest {

    private GUIClientCommand cmd;
    private Main clientMain;

    @Before
    public void setUp() {
        clientMain = mock(Main.class);
        cmd = new GUIClientCommand(clientMain);
    }

    @After
    public void tearDown() {
        cmd = null;
        clientMain = null;
    }

    @Test
    public void testRun() throws CommandException {
        BundleContext bCtx = mock(BundleContext.class);
        CommandContextFactory cmdCtxFactory = mock(CommandContextFactory.class);

        CommandContext cmdCtx = mock(CommandContext.class);
        when(cmdCtx.getCommandContextFactory()).thenReturn(cmdCtxFactory);

        cmd.setBundleContext(bCtx);
        cmd.run(cmdCtx);

        verify(clientMain).run();
        verify(bCtx).registerService(eq(ApplicationService.class.getName()), isNotNull(), any(Dictionary.class));
        verify(bCtx).registerService(eq(ContextAction.class.getName()), isNotNull(), any(Dictionary.class));
    }

    @Test
    public void testName() {
        assertEquals("gui", cmd.getName());
    }

    @Test
    public void testDescAndUsage() {
        assertNotNull(cmd.getDescription());
        assertNotNull(cmd.getUsage());
    }

    @Test
    public void testRequiresStorage() {
        assertFalse(cmd.isStorageRequired());
    }

    @Test
    public void testOptions() {
        Options options = cmd.getOptions();
        assertNotNull(options);
        assertEquals(0, options.getOptions().size());
    }
}
