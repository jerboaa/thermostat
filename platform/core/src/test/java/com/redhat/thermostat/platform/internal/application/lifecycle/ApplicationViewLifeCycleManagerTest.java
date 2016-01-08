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

package com.redhat.thermostat.platform.internal.application.lifecycle;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.internal.application.ApplicationInfo;
import com.redhat.thermostat.platform.internal.application.ApplicationRegistry;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.MVCLifeCycleManager;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationViewLifeCycleManagerTest {

    private ApplicationLifeCycleManager manager;
    private CountDownLatch shutdownLatch;
    private ApplicationLifeCycleThread lifeCycleThread;
    private Thread thread;
    private MVCLifeCycleManager mvcLifeCycleManager;

    @Before
    public void setUp() {

        mvcLifeCycleManager = mock(MVCLifeCycleManager.class);

        shutdownLatch = mock(CountDownLatch.class);
        lifeCycleThread = mock(ApplicationLifeCycleThread.class);
        thread = mock(Thread.class);
        
        manager = new ApplicationLifeCycleManager(shutdownLatch,
                                                  lifeCycleThread,
                                                  mvcLifeCycleManager)
        {
            @Override
            Thread createThread(ApplicationLifeCycleThread lifeCycleRunnable) {
                return thread;
            }
        };
    }
    
    @Test
    public void testExecute() throws Exception {
        manager.execute();
        
        verify(shutdownLatch).await();
        verify(thread).start();
    }
    
    @Test
    public void testTarget() {
        
        ApplicationRegistry registry = mock(ApplicationRegistry.class);
        
        ApplicationInfo.Application target = new ApplicationInfo.Application();
        target.name = "test";
        target.provider = "test.class";
        
        manager.setTarget(target);
        
        ApplicationProvider provider = mock(ApplicationProvider.class);
        
        when(registry.containsProvider("test.class")).thenReturn(true);
        when(registry.getProvider("test.class")).thenReturn(provider);

        ActionEvent event = mock(ActionEvent.class);
        when(event.getSource()).thenReturn(registry);
    
        manager.actionPerformed(event);
    }
}
