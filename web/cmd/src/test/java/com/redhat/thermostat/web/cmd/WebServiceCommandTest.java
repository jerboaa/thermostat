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

package com.redhat.thermostat.web.cmd;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class WebServiceCommandTest {

    private TestCommandContextFactory cmdCtxFactory;
    private BundleContext bundleContext;
    private WebServiceLauncher launcher;
    private WebServiceCommand cmd;
    
    @Before
    public void setUp() {
        Bundle sysBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(sysBundle);
        cmdCtxFactory = new TestCommandContextFactory(bundleContext);
        launcher = mock(WebServiceLauncher.class);
        cmd = new WebServiceCommand(launcher);
    }
    
    @After
    public void tearDown() {
        cmdCtxFactory = null;
        cmd = null;
        launcher = null;
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void verifyLauncherStart() throws Exception {
        SimpleArguments args = new SimpleArguments();
        String storageUrl = "mongodb://127.0.0.1:27518";
        args.addArgument("dbUrl", storageUrl);
        args.addArgument("bindAddrs", "127.0.0.1:8888,127.0.0.2:9999");
        try {
            cmd.run(cmdCtxFactory.createContext(args));
        } catch (CommandException e) {
            fail("should not throw exception");
        }
        verify(launcher).setStorageURL(storageUrl);
        verify(launcher).setStorageUsername(null);
        verify(launcher).setStoragePassword(null);
        verify(launcher).setIpAddresses(any(List.class));
        verify(launcher).start();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyLauncherStartWithAuth() throws Exception {
        SimpleArguments args = new SimpleArguments();
        String storageUrl = "mongodb://127.0.0.1:27518";
        args.addArgument("dbUrl", storageUrl);
        args.addArgument("bindAddrs", "127.0.0.1:8888,127.0.0.2:9999");
        args.addArgument("username", "testuser");
        args.addArgument("password", "testpasswd");
        try {
            cmd.run(cmdCtxFactory.createContext(args));
        } catch (CommandException e) {
            fail("should not throw exception");
        }
        verify(launcher).setStorageURL(storageUrl);
        verify(launcher).setStorageUsername("testuser");
        verify(launcher).setStoragePassword("testpasswd");
        verify(launcher).setIpAddresses(any(List.class));
        verify(launcher).start();
    }
}
