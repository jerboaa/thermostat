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

package com.redhat.thermostat.tools.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.CommonCommandOptions;
import com.redhat.thermostat.launcher.DbService;
import com.redhat.thermostat.launcher.DbServiceFactory;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.tools.LocaleResources;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OSGIUtils.class, DbServiceFactory.class })
public class ConnectCommandTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private ConnectCommand cmd;
    private TestCommandContextFactory cmdCtxFactory;
    private BundleContext bundleContext;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        setupCommandContextFactory();

        cmd = new ConnectCommand();

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
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyConnectedThrowsException() {
        DbService dbService = mock(DbService.class);
        OSGIUtils utils = mock(OSGIUtils.class);
        PowerMockito.mockStatic(OSGIUtils.class);
        when(OSGIUtils.getInstance()).thenReturn(utils);
        when(utils.getServiceAllowNull(DbService.class)).thenReturn(dbService);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        try {
            cmd.run(cmdCtxFactory.createContext(args));
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.COMMAND_CONNECT_ALREADY_CONNECTED), e.getMessage());
        }
    }
    
    @Test
    public void verifyNotConnectedConnects() throws CommandException {
        OSGIUtils utils = mock(OSGIUtils.class);
        PowerMockito.mockStatic(OSGIUtils.class);
        when(OSGIUtils.getInstance()).thenReturn(utils);
        when(utils.getServiceAllowNull(DbService.class)).thenReturn(null);

        DbService dbService = mock(DbService.class);
        PowerMockito.mockStatic(DbServiceFactory.class);
        String username = "testuser";
        String password = "testpassword";
        String dbUrl = "mongodb://10.23.122.1:12578";
        when(DbServiceFactory.createDbService(eq(username), eq(password), eq(dbUrl))).thenReturn(dbService);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("dbUrl", dbUrl);
        args.addArgument("username", username);
        args.addArgument("password", password);
        CommandContext ctx = cmdCtxFactory.createContext(args);
        cmd.run(ctx);
        verify(dbService).connect();
        verify(dbService).setServiceRegistration(any(ServiceRegistration.class));
    }
    
    @Test
    public void testIsStorageRequired() {
        assertFalse(cmd.isStorageRequired());
    }

    @Test
    public void testIsNotAvailableOutsideShell() {
        assertFalse(cmd.isAvailableOutsideShell());
    }
    
    @Test
    public void testIsAvailableInShell() {
        assertTrue(cmd.isAvailableInShell());
    }

    @Test
    public void testName() {
        assertEquals("connect", cmd.getName());
    }

    @Test
    public void testDescAndUsage() {
        assertNotNull(cmd.getDescription());
        assertNotNull(cmd.getUsage());
    }

    @Ignore
    @Test
    public void testAcceptedArguments() {
        Options options = cmd.getOptions();
        assertNotNull(options);
        assertTrue(options.getOptions().size() == 3);

        assertTrue(options.hasOption(CommonCommandOptions.DB_URL_ARG));
        Option db = options.getOption(CommonCommandOptions.DB_URL_ARG);
        assertEquals(CommonCommandOptions.DB_URL_DESC, db.getDescription());
        assertTrue(db.isRequired());
        assertTrue(db.hasArg());

        assertTrue(options.hasOption(CommonCommandOptions.USERNAME_ARG));
        Option user = options.getOption(CommonCommandOptions.USERNAME_ARG);
        assertEquals(CommonCommandOptions.USERNAME_DESC, user.getDescription());
        assertFalse(user.isRequired());
        assertTrue(user.hasArg());

        assertTrue(options.hasOption(CommonCommandOptions.PASSWORD_ARG));
        Option pass = options.getOption(CommonCommandOptions.PASSWORD_ARG);
        assertEquals(CommonCommandOptions.PASSWORD_DESC, pass.getDescription());
        assertFalse(pass.isRequired());
        assertTrue(pass.hasArg());
    }
}
