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

package com.redhat.thermostat.storage.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import com.redhat.thermostat.service.process.ProcessHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;
import com.redhat.thermostat.testutils.TestUtils;

public class StorageCommandTest {
    
    private static final String PORT = "27518";
    private static final String BIND = "127.0.0.1";
    private static final String DB = "data" + File.separatorChar + "db";

    private String tmpDir;
    private ExitStatus exitStatus;
    private ProcessHandler processHandler;
    private CommonPaths paths;
    
    @Before
    public void setup() {
        exitStatus = mock(ExitStatus.class);
        // need to create a dummy config file for the test
        try {
            Properties props = new Properties();
            
            props.setProperty(DBConfig.BIND.name(), BIND);
            props.setProperty(DBConfig.PORT.name(), PORT);

            tmpDir = TestUtils.setupStorageConfigs(props);
        } catch (IOException e) {
            Assert.fail("cannot setup tests: " + e);
        }

        processHandler = mock(ProcessHandler.class);

        paths = mock(CommonPathsImpl.class);
        File baseDir = new File(tmpDir);
        File userRuntimeDir = new File(baseDir, "run");
        File userDataDir = new File(baseDir, "data");
        File logsDir = new File(baseDir, "logs");
        File confDir = new File(baseDir, "etc");

        when(paths.getUserThermostatHome()).thenReturn(baseDir);
        when(paths.getUserRuntimeDataDirectory()).thenReturn(userRuntimeDir);
        when(paths.getUserPersistentDataDirectory()).thenReturn(userDataDir);
        when(paths.getUserConfigurationDirectory()).thenReturn(confDir);
        when(paths.getUserStorageDirectory()).thenCallRealMethod();
        when(paths.getUserStorageConfigurationFile()).thenCallRealMethod();
        when(paths.getUserLogDirectory()).thenReturn(logsDir);
        when(paths.getUserStorageLogFile()).thenCallRealMethod();
        when(paths.getUserStoragePidFile()).thenCallRealMethod();

        when(paths.getSystemThermostatHome()).thenReturn(baseDir);
        when(paths.getSystemConfigurationDirectory()).thenCallRealMethod();
        when(paths.getSystemStorageConfigurationFile()).thenCallRealMethod();
    }
    
    @After
    public void tearDown() {
        exitStatus = null;
    }
    
    @Test
    public void testConfig() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("quiet", null);
        args.addArgument("start", null);
        args.addArgument("dryRun", null);
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.getArguments()).thenReturn(args);

        StorageCommand service = new StorageCommand(exitStatus, processHandler, paths) {
            @Override
            MongoProcessRunner createRunner() {
                throw new AssertionError("dry run should never create an actual runner");
            }
        };

        service.run(ctx);
        
        DBStartupConfiguration conf = service.getConfiguration();
        
        Assert.assertEquals(tmpDir + DB, conf.getDBPath().getPath());
        Assert.assertEquals(Integer.parseInt(PORT), conf.getPort());
        Assert.assertEquals("mongodb://" + BIND + ":" + PORT , conf.getDBConnectionString());
    }
    
    private StorageCommand prepareService(boolean startSuccess) throws IOException,
            InterruptedException, InvalidConfigurationException, ApplicationException
    {
        final MongoProcessRunner runner = mock(MongoProcessRunner.class);
        if (!startSuccess) {
           doThrow(new ApplicationException("mock exception")).when(runner).startService();
        }
        
        // TODO: stop not tested yet, but be sure it's not called from the code
        doThrow(new ApplicationException("mock exception")).when(runner).stopService();
        
        StorageCommand service = new StorageCommand(exitStatus, processHandler, paths) {
            @Override
            MongoProcessRunner createRunner() {
                return runner;
            }
        };
        
        return service;
    }
    
    @Test
    public void testListeners() throws InterruptedException, IOException, ApplicationException, InvalidConfigurationException, CommandException
    {
        StorageCommand service = prepareService(true);
        
        final CountDownLatch latch = new CountDownLatch(2);
        
        final boolean[] result = new boolean[2];
        service.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = false;
                    latch.countDown();
                    latch.countDown();
                    break;
                    
                case SUCCESS:
                    result[0] = true;
                    latch.countDown();
                    break;

                case START:
                    result[1] = true;
                    latch.countDown();
                    break;
                }
            }
        });
        
        service.run(prepareContext());
        latch.await();
        
        Assert.assertTrue(result[0]);
        Assert.assertTrue(result[1]);
    }
    
    @Test
    public void testListenersFail() throws InterruptedException, IOException, ApplicationException, CommandException, InvalidConfigurationException
    {
        StorageCommand service = prepareService(false);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        service.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = true;
                    break;
                    
                case SUCCESS:
                    result[0] = false;
                    break;
                }
                latch.countDown();
            }
        });
        
        service.run(prepareContext());
        latch.await();
        
        Assert.assertTrue(result[0]);
    }
    
    @Test
    public void exceptionSetsExitStatusOnFailure() throws Exception {
        this.exitStatus = new ExitStatus() {
            
            private int exitStatus = -1;
            
            @Override
            public void setExitStatus(int newExitStatus) {
                exitStatus = newExitStatus;
            }
            
            @Override
            public int getExitStatus() {
                return exitStatus;
            }
        };
        assertEquals(-1, this.exitStatus.getExitStatus());
        StorageCommand command = prepareService(false);
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        command.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = true;
                    break;
                    
                case SUCCESS:
                    result[0] = false;
                    break;
                }
                latch.countDown();
            }
        });
        command.run(prepareContext());
        latch.await();
        // should have failed
        assertTrue(result[0]);
        assertEquals(ExitStatus.EXIT_ERROR, this.exitStatus.getExitStatus());
    }
    
    @Test
    public void exitStatusRemainsUntouchedOnSuccess() throws Exception {
        this.exitStatus = new ExitStatus() {
            
            private int exitStatus = -1;
            
            @Override
            public void setExitStatus(int newExitStatus) {
                exitStatus = newExitStatus;
            }
            
            @Override
            public int getExitStatus() {
                return exitStatus;
            }
        };
        StorageCommand command = prepareService(true);
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        command.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = false;
                    break;
                    
                case SUCCESS:
                    result[0] = true;
                    break;
                }
                latch.countDown();
            }
        });
        command.run(prepareContext());
        latch.await();
        // should have worked
        assertTrue(result[0]);
        // this impl of ExitStatus has a default value of -1
        assertEquals(-1, this.exitStatus.getExitStatus());
    }

    private CommandContext prepareContext() {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("quiet", "--quiet");
        args.addArgument("start", "--start");
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.getArguments()).thenReturn(args);
        return ctx;
    }

}

