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

package com.redhat.thermostat.local.command.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;

public class LocalCommandTest {
    private static final String SHOW_SPLASH = "--show-splash";
    private LocalCommand cmd;
    private CommandContext ctxt;
    private CommonPaths paths;
    private Launcher launcher;
    private ServiceLauncher service;

    @Before
    public void setup() {
        paths = mock(CommonPaths.class);
        when(paths.getSystemBinRoot()).thenReturn(new File(""));
        launcher = mock(Launcher.class);
        ctxt = mock(CommandContext.class);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Verifies that a watch service gets set up which listens for a delete
     * event of the splash screen stamp file and then closes the splash screen.
     * 
     * This tests relies on a certain number of times
     * {@link CommonPaths#getUserSplashScreenStampFile()}
     * is being invoked in LocalCommand.
     * 
     * @throws CommandException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test(timeout=5000)
    public void testSplashScreenFunctionalityAndClosing() throws CommandException, InterruptedException, IOException {
        final File stamp = new File(tempFolder.getRoot(), "splashscreen.stamp");
        assertFalse(stamp.exists());
        cmd = createLocalCommandForSplashTests(new Runnable() {
            @Override
            public void run() {
                assertTrue(stamp.exists()); // LocalCommand created it
            }
        });
        final boolean[] pollWatchDeleteEventHappened = new boolean[1];
        when(ctxt.getArguments().hasArgument(SHOW_SPLASH)).thenReturn(true);
        when(paths.getUserPersistentDataDirectory()).thenReturn(tempFolder.getRoot());
        final CountDownLatch pollEventLatch = new CountDownLatch(1);
        when(paths.getUserSplashScreenStampFile()).thenAnswer(new Answer<File>() {
            private int getStampHitCount = 1;
            @Override
            public File answer(InvocationOnMock invocation) {
                // The third invocation of getUserSplashScreenStampFile() will
                // be in LocalCommand.closeSplashScreen()'s while condition.
                // At that point, we know the stamp file exists (asserted earlier)
                // and the WatchService for deletion events have been registered.
                // Aside: The first invocation is in LocalCommand.run() and the
                //        second invocation is in LocalCommand.closeSplashScreen()
                //        before the WatchService got registered.
                // We only start the delete thread after we know the WatchService
                // has been installed via register(). This guarantees that the
                // WatchService's poll() action will return (no timeout) without
                // introducing a race as much as possible. There is a slim chance
                // of the File.exists() call to return false when the delete
                // stamp file thread happens to run before this answer call returns.
                if (getStampHitCount == 3) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // We need to do this asynchronously since the
                            // File.exists() call on the returned stamp should
                            // first return true before we delete it.
                            stamp.delete();
                        }
                    }).start();
                }
                if (getStampHitCount == 4) {
                    // At this point we are handling the WatchEvent.
                    pollWatchDeleteEventHappened[0] = true;
                    pollEventLatch.countDown();
                }
                getStampHitCount++;
                return stamp;
           }
        });
        cmd.run(ctxt);
        pollEventLatch.await();
        assertTrue(cmd.isSplashScreenEnabled());
        assertTrue("Expected poll event for deleted file to happen",
                   pollWatchDeleteEventHappened[0]);
        assertFalse(stamp.exists());
    }

    @Test
    public void testNoShowSplashOption() throws CommandException, InterruptedException {
        cmd = createLocalCommandForSplashTests();
        cmd.run(ctxt);
        assertFalse(cmd.isSplashScreenEnabled());
    }
    
    private LocalCommand createLocalCommandForSplashTests(final Runnable execProcCallback) {
        cmd = new LocalCommand() {
            @Override
            ServiceLauncher createServiceLauncher() {
                return mock(ServiceLauncher.class);
            }

            @Override
            Process execProcess(String... command) throws IOException {
                execProcCallback.run();
                return mock(Process.class);
            }
        };
        when(ctxt.getArguments()).thenReturn(mock(Arguments.class));
        cmd.setPaths(paths);
        cmd.setLauncher(launcher);
        return cmd;
    }

    private LocalCommand createLocalCommandForSplashTests() throws CommandException, InterruptedException {
        Runnable doNothingCallBack = new Runnable() {
            
            @Override
            public void run() {
                // nothing
            }
        };
        return createLocalCommandForSplashTests(doNothingCallBack);
    }

    @Test
    public void testPathsNotSetFailure() throws CommandException {
        cmd = createLocalCommand();
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Required service CommonPaths is unavailable"));
        }
    }

    @Test
    public void testLauncherNotSetFailure() throws CommandException {
        cmd = createLocalCommand();
        cmd.setPaths(paths);
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Required service Launcher is unavailable"));
        }
    }

    private LocalCommand createLocalCommand() throws CommandException {
        service = mock(ServiceLauncher.class);
        doNothing().when(service).start();
        doNothing().when(service).stop();
        return new LocalCommand() {
            @Override
            ServiceLauncher createServiceLauncher() {
                return service;
            }
        };
    }

    @Test
    public void testRunLocalCommand() throws CommandException, InterruptedException {
        service = mock(ServiceLauncher.class);
        doNothing().when(service).start();
        doNothing().when(service).stop();

        final Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenReturn(0);
        when(ctxt.getArguments()).thenReturn(mock(Arguments.class));

        cmd = new LocalCommand() {
            @Override
            ServiceLauncher createServiceLauncher() {
                return service;
            }

            @Override
            Process execProcess(String... command) throws IOException {
                String[] expectedArgs = {"/thermostat", "gui"};
                assertArrayEquals("Incorrect thermostat service args", expectedArgs, command);
                return mockProcess;
            }
        };

        cmd.setPaths(paths);
        cmd.setLauncher(launcher);
        cmd.run(ctxt);

        String[] args = {"gui"};
        verify(service, times(1)).start();
        verify(service, times(1)).stop();
        verify(mockProcess, times(1)).waitFor();
    }
}

