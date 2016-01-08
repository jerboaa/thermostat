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
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.platform.Application;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.internal.application.ApplicationState;
import com.redhat.thermostat.platform.internal.application.lifecycle.ApplicationHandler.StateChangeEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationHandlerTest {

    private ApplicationProvider provider;
    private ActionListener<StateChangeEvent> listener;
    private ApplicationHandler handler;
    private Application application;

    private ArgumentCaptor<ActionEvent> eventCaptor;
    private Platform fakePlatform = new Platform() {
        
        @Override
        public void queueOnViewThread(Runnable runnable) {}
        
        @Override
        public void queueOnApplicationThread(Runnable runnable) {
            runnable.run();
        }
        
        @Override
        public boolean isViewThread() {
            return false;
        }
        
        @Override
        public boolean isApplicationThread() {
            return true;
        }
        
        @Override
        public ApplicationService getAppService() {
            return null;
        }
    };
    
    @Before
    public void setUp() {
        ApplicationHandler.__test__ = true;

        provider = mock(ApplicationProvider.class);
        listener = mock(ActionListener.class);
        application = mock(Application.class);
        when(provider.getApplication()).thenReturn(application);
        
        eventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        doNothing().when(listener).actionPerformed(eventCaptor.capture());
        
        handler = new ApplicationHandler();
        handler.initHandler(provider);
        handler.addStateChangeListener(listener);
        
        handler.application = application;
        handler.platform = fakePlatform;
    }

    @Test
    public void testCreate() {
        
        // reset application
        handler.application = null;
        
        handler.create();
        assertStateChange(ApplicationState.CREATED);
    }
    
    @Test
    public void testInit() {
        handler.init();
        
        verify(application).init();
        assertStateChange(ApplicationState.INITIALIZED);
    }

    @Test
    public void testStart() {
        handler.start();
        
        verify(application).start();
        assertStateChange(ApplicationState.STARTED);
    }
    
    @Test
    public void testStop() {
        handler.stop();
        
        verify(application).stop();
        assertStateChange(ApplicationState.STOPPED);
    }
    
    @Test
    public void testDestroy() {
        handler.destroy();
        
        verify(application).destroy();
        assertStateChange(ApplicationState.DESTROYED);
    }
    
    private void assertStateChange(ApplicationState state) {
        ActionEvent event = eventCaptor.getValue();
        assertEquals(state, event.getPayload());
    }
}
