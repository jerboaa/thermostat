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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class DisconnectCommandTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private StubBundleContext context;
    private DisconnectCommand cmd;
    private TestCommandContextFactory cmdCtxFactory;

    @Before
    public void setUp() {
        context = new StubBundleContext();
        cmdCtxFactory = new TestCommandContextFactory(context);

        cmd = new DisconnectCommand(context);
    }

    @After
    public void tearDown() {
        cmdCtxFactory = null;
        cmd = null;
    }

    @Test
    public void verifyNotConnectedThrowsException() {
        try {
            cmd.run(cmdCtxFactory.createContext(new SimpleArguments()));
            fail("cmd.run() should have thrown exception.");
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.COMMAND_DISCONNECT_NOT_CONNECTED).getContents(), e.getMessage());
        }
    }
    
    @Test
    public void verifyConnectedDisconnects() throws CommandException {
        DbService dbService = mock(DbService.class);
        context.registerService(DbService.class, dbService, null);
        
        CommandContext ctx = cmdCtxFactory.createContext(new SimpleArguments());
        cmd.run(ctx);
        verify(dbService).disconnect();
    }

    @Test
    public void testIsStorageRequired() {
        assertFalse(cmd.isStorageRequired());
    }

}

