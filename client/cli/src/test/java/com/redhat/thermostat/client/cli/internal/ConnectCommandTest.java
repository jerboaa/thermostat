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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ConnectCommandTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private StubBundleContext context;
    private ConnectCommand cmd;
    private TestCommandContextFactory cmdCtxFactory;
    private BundleContext bundleContext;
    private DbServiceFactory dbServiceFactory;

    @Before
    public void setUp() {
        setupCommandContextFactory();

        context = new StubBundleContext();
        dbServiceFactory = mock(DbServiceFactory.class);
        cmd = new ConnectCommand(context, dbServiceFactory);
    }

    private void setupCommandContextFactory() {
        Bundle sysBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(sysBundle);
        cmdCtxFactory = new TestCommandContextFactory(bundleContext);
        
    }

    @After
    public void tearDown() {
        cmdCtxFactory = null;
        cmd = null;
    }

    @Test
    public void verifyConnectedThrowsExceptionWithDiagnosticMessage() {
        Keyring keyring = mock(Keyring.class);
        context.registerService(Keyring.class, keyring, null);
        String dbUrl = "fluff";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(dbUrl);
        context.registerService(DbService.class, dbService, null);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", dbUrl);
        try {
            cmd.run(cmdCtxFactory.createContext(args));
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.COMMAND_CONNECT_ALREADY_CONNECTED, dbUrl).getContents(), e.getMessage());
        }
    }
    
    @Test
    public void verifyNotConnectedConnects() throws CommandException {
        Keyring keyring = mock(Keyring.class);
        context.registerService(Keyring.class, keyring, null);
        DbService dbService = mock(DbService.class);

        String username = "testuser";
        String password = "testpassword";
        String dbUrl = "mongodb://10.23.122.1:12578";
        when(dbServiceFactory.createDbService(eq(username), eq(password), eq(dbUrl))).thenReturn(dbService);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("dbUrl", dbUrl);
        CommandContext ctx = cmdCtxFactory.createContext(args);
        cmdCtxFactory.setInput(username + '\r' + password + '\r');
        cmd.run(ctx);
        verify(dbService).connect();
    }
    
    @Test
    public void verifyNoKeyring() throws CommandException {
        DbService dbService = mock(DbService.class);

        String dbUrl = "mongodb://10.23.122.1:12578";
        SimpleArguments args = new SimpleArguments();
        args.addArgument("dbUrl", dbUrl);
        CommandContext ctx = cmdCtxFactory.createContext(args);
        
        try {
            cmd.run(ctx);
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.COMMAND_CONNECT_NO_KEYRING).getContents(), e.getMessage());
        }
        
        verify(dbService, never()).connect();
    }
    
    @Test
    public void testIsStorageRequired() {
        assertFalse(cmd.isStorageRequired());
    }

}

