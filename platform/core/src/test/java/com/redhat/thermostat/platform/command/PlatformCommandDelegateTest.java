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

package com.redhat.thermostat.platform.command;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import com.redhat.thermostat.platform.internal.application.lifecycle.ApplicationLifeCycleManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertTrue;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.platform.internal.application.ApplicationInfo;
import com.redhat.thermostat.platform.internal.application.ApplicationRegistry;
import com.redhat.thermostat.platform.internal.application.ConfigurationManager;

import static com.redhat.thermostat.platform.internal.locale.LocaleResources.*;

public class PlatformCommandDelegateTest {

    private CommandContext ctx;
    private ConfigurationManager manager;
    private Console console;
    private ByteArrayOutputStream stream;
    private PrintStream out;
    private CountDownLatch latch;

    private ApplicationRegistry registry;
    private ApplicationLifeCycleManager lifeCycleManager;

    private static Locale locale;
    
    @BeforeClass
    public static void init() {
        locale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }
    
    @AfterClass
    public static void cleanup() {
        Locale.setDefault(locale);
    }
    
    @Before
    public void setUp() {
        ctx = mock(CommandContext.class);
        manager = mock(ConfigurationManager.class);
        console = mock(Console.class);        
        registry = mock(ApplicationRegistry.class);
        latch = mock(CountDownLatch.class);
        lifeCycleManager = mock(ApplicationLifeCycleManager.class);

        stream = new ByteArrayOutputStream();
        out = new PrintStream(stream);
        
        when(ctx.getConsole()).thenReturn(console);
        when(console.getOutput()).thenReturn(out);
    }
    
    @Test
    public void testListCommands() {
        
        ApplicationInfo.Application info0 = new ApplicationInfo.Application();
        info0.name = "0";
        info0.provider = "provider0";
        
        ApplicationInfo.Application info1 = new ApplicationInfo.Application();
        info1.name = "1";
        info1.provider = "provider1";
        
        ApplicationInfo infos = new ApplicationInfo();
        infos.applications = new ArrayList<>();
        infos.applications.add(info0);
        infos.applications.add(info1);
        
        when(manager.getApplicationConfigs()).thenReturn(infos);

        PlatformCommandDelegate.listApplications(ctx, manager);
        
        String content = stream.toString();

        // the number of spaces is dependent on the
        // header length, so also changes based on the locale
        assertTrue(content.contains("0       |  provider0"));
        assertTrue(content.contains("1       |  provider1"));
    }
    
    @Test
    public void testStartInvalidPlatform() {
        ApplicationInfo.Application info0 = new ApplicationInfo.Application();
        info0.name = "0";
        info0.provider = "provider0";
        
        ApplicationInfo infos = new ApplicationInfo();
        infos.applications = new ArrayList<>();
        infos.applications.add(info0);
        
        when(manager.getApplicationConfigs()).thenReturn(infos);
        when(ctx.getConsole()).thenReturn(console);
        when(console.getOutput()).thenReturn(out);

        PlatformCommandDelegate.startPlatform(ctx, "1", manager, registry,
                                              lifeCycleManager);
        
        String content = stream.toString();

        // this is also dependent on the locale
        assertTrue(content.contains(translate(INVALID_PLATFORM, "1")));
    }
    
    @Test
    public void testStartPlatform() {
        ApplicationInfo.Application info0 = new ApplicationInfo.Application();
        info0.name = "0";
        info0.provider = "provider0";
        
        // this also implicitly tests that name '0' is correctly passed
        // as an argument to getApplicationConfig inside the startPlatform
        // method
        when(manager.getApplicationConfig("0")).thenReturn(info0);

        PlatformCommandDelegate.startPlatform(ctx, "0", manager, registry,
                                              lifeCycleManager);
        
        verify(registry).start();
    }
}
