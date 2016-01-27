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

package com.redhat.thermostat.web.endpoint.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Test;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.web.endpoint.internal.EmbeddedServletContainerConfiguration;
import com.redhat.thermostat.web.endpoint.internal.WebappLauncherCommand;
import com.redhat.thermostat.web.endpoint.internal.EmbeddedServletContainerConfiguration.ConfigKeys;

public class WebappLauncherCommandTest {
    
    private TestLogHandler handler;
    private Logger logger;
    
    @After
    public void tearDown() {
        if (handler != null && logger != null) {
            logger.removeHandler(handler);
            handler = null;
        }
    }

    @Test
    public void testIsStorageRequired() {
        WebappLauncherCommand cmd = new WebappLauncherCommand(null /* not used */);
        assertFalse(cmd.isStorageRequired());
    }
    
    @Test
    public void testRunCommandWithNoConfigListenAddressSpecified() {
        CommonPaths paths = mock(CommonPaths.class);
        
        File thermostatHomeNotExisting = new File("/thermostat-home/not-existing");
        when(paths.getSystemThermostatHome()).thenReturn(thermostatHomeNotExisting);
        // config needs non-null paths for configuration files. It does not
        // matter if that file actually exists. Using the fake THERMOSTAT_HOME
        // variable will do.
        File nonNullConfigFile = new File("doesn't matter");
        
        String matchString = "CONFIG_LISTEN_ADDRESS";
        runTestWithPathsAndConfigFiles(paths, nonNullConfigFile, nonNullConfigFile, matchString);
    }
    
    @Test
    public void testRunCommandWithWebArchiveNotExisting() throws Exception {
        CommonPaths paths = mock(CommonPaths.class);
        
        File thermostatHomeNotExisting = new File("/thermostat-home/not-existing");
        when(paths.getSystemThermostatHome()).thenReturn(thermostatHomeNotExisting);
        File nonNullSysConfig = new File("no matter");
        
        Properties userProperties = new Properties();
        userProperties.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), "127.0.0.1:8888");
        File userPropsTempFile = File.createTempFile("thermostat", WebappLauncherCommandTest.class.getName());
        userPropsTempFile.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(userPropsTempFile)) {
            userProperties.store(fout, "test props");
        } catch (IOException e) {
            // ignore
        }
        assertTrue(userPropsTempFile.exists());
        
        String matchString = "Exploded web archive";
        runTestWithPathsAndConfigFiles(paths, userPropsTempFile, nonNullSysConfig, matchString);
    }
    
    private void runTestWithPathsAndConfigFiles(final CommonPaths paths, final File userConfig,
                                                final File systemConfig, final String matchString) {
        StubBundleContext context = new StubBundleContext();
        context.registerService(CommonPaths.class, paths, null);
        Launcher launcher = new TestLauncher();
        context.registerService(Launcher.class, launcher, null);
        context.registerService(SSLConfiguration.class, mock(SSLConfiguration.class), null);
        final EmbeddedServletContainerConfiguration testConfig = new EmbeddedServletContainerConfiguration(paths, systemConfig, userConfig);
        WebappLauncherCommand cmd = new WebappLauncherCommand(context) {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return testConfig;
            }
        };
        CommandContextFactory factory = new CommandContextFactory(context);
        CommandContext ctxt = factory.createContext(new Arguments() {

            @Override
            public List<String> getNonOptionArguments() {
                return Collections.emptyList();
            }

            @Override
            public boolean hasArgument(String name) {
                return false;
            }

            @Override
            public String getArgument(String name) {
                return null;
            }
            
        });
        setupLogger(matchString);
        assertFalse(handler.gotLogMessage);
        try {
            cmd.run(ctxt);
            fail("Should have failed to run command");
        } catch (CommandException e) {
            // pass
            assertTrue("Did not match. message was: '" + e.getMessage() + "'", handler.gotLogMessage);
        }
    }
    
    private void setupLogger(String matchString) {
        logger = Logger.getLogger("com.redhat.thermostat");
        handler = new TestLogHandler(matchString);
        logger.addHandler(handler);
    }

    private static class TestLauncher implements Launcher {

        @Override
        public void run(String[] args, boolean inShell) {
            // no-op
        }

        @Override
        public void run(String[] args,
                Collection<ActionListener<ApplicationState>> listeners,
                boolean inShell) {
            if (args[0].equals("storage") && args[1].equals("--start")) {
                // start storage
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                for (ActionListener<ApplicationState> l: listeners) {
                    ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.START);
                    l.actionPerformed(fakeEvent);
                }
            }
        }
        
    }
    
    private static class TestLogHandler extends Handler {
        
        private final String matchString;
        private boolean gotLogMessage = false;
        
        private TestLogHandler(String matchString) {
            this.matchString = matchString;
        }
        
        @Override
        public void publish(LogRecord record) {
            String logMessage = record.getMessage();
            if (record.getLevel().intValue() >= Level.WARNING.intValue() && 
                    logMessage.contains(matchString)) {
                gotLogMessage = true;
            };
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() throws SecurityException {
            // no-op
        }
    }
    
}
