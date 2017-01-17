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

package com.redhat.thermostat.platform.internal.application.lifecycle;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.platform.Application;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.internal.application.ApplicationState;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.MVCLifeCycleManager;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationViewLifeCycleThreadTest {

    private BlockingQueue<ApplicationState> queue;
    private ApplicationHandler handler;
    private CountDownLatch shutdownLatch;
    private MVCLifeCycleManager mvcLifeCycleManager;
    private Application application;
    private Platform platform;

    private BooleanProperty shutdown;

    @Before
    public void setUp() {

        shutdown = mock(BooleanProperty.class);

        queue = mock(BlockingQueue.class);
        handler = mock(ApplicationHandler.class);
        shutdownLatch = mock(CountDownLatch.class);
        mvcLifeCycleManager = mock(MVCLifeCycleManager.class);
        when(mvcLifeCycleManager.shutdownProperty()).thenReturn(shutdown);
        application = mock(Application.class);
        platform = mock(Platform.class);

        when(handler.create()).thenReturn(application);
        when(application.getPlatform()).thenReturn(platform);
    }
    
    @Test
    public void testCreate() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
        
        lifeCycleThread.handleState(ApplicationState.CREATE);
        verify(handler).create();
    }
    
    @Test
    public void testCreated() throws Exception {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
        
        lifeCycleThread.handleState(ApplicationState.CREATED);
        verify(queue).put(ApplicationState.INITIALIZE);
    }
    
    @Test
    public void testInitialize() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.INITIALIZE);
        verify(handler).init();
    }

    @Test
    public void testInitialized() throws Exception {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.INITIALIZED);
        verify(queue).put(ApplicationState.START);
    }
    
    @Test
    public void testStart() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.START);
        verify(handler).start();
    }
    
    @Test
    public void testStarted() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);

        // since we skip the create phase we need this shortcut
        lifeCycleThread.application = application;
                
        lifeCycleThread.handleState(ApplicationState.STARTED);
        verify(mvcLifeCycleManager).start();
        verify(mvcLifeCycleManager).setPlatform(platform);
    }
    
    @Test
    public void testStop() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.STOP);
        verify(mvcLifeCycleManager).stop();
    }
    
    @Test
    public void testStopped() {}
    
    @Test
    public void testDestroy() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.DESTROY);
        verify(handler).destroy();
    }
    
    @Test
    public void testDestroyed() {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
                
        lifeCycleThread.handleState(ApplicationState.DESTROYED);
        verify(shutdownLatch).countDown();
    }

    @Test
    public void testQueueState() throws Exception {
        ApplicationLifeCycleThread lifeCycleThread =
                new ApplicationLifeCycleThread(queue, handler,
                                               mvcLifeCycleManager,
                                               shutdownLatch);
        
        lifeCycleThread.queueState(ApplicationState.DESTROY);
        verify(queue).put(ApplicationState.DESTROY);

        lifeCycleThread.queueState(ApplicationState.INITIALIZE);
        verify(queue).put(ApplicationState.INITIALIZE);
    }
}
