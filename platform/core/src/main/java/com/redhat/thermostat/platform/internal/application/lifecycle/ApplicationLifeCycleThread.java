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

import com.redhat.thermostat.beans.property.ChangeListener;
import com.redhat.thermostat.beans.property.ObservableValue;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.platform.Application;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.PlatformShutdown;
import com.redhat.thermostat.platform.internal.application.ApplicationState;
import com.redhat.thermostat.platform.internal.application.lifecycle.ApplicationHandler.StateChangeEvent;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.MVCLifeCycleManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class ApplicationLifeCycleThread implements Runnable, PlatformShutdown {

    private ApplicationHandler handler;
    private BlockingQueue<ApplicationState> queue;
    private StateChangeListener stateChangeListener;
    private CountDownLatch shutdownLatch;

    private MVCLifeCycleManager mvcLifeCycleManager;
    
    private boolean running;

    // Testing hook
    Application application;

    public ApplicationLifeCycleThread(CountDownLatch shutdownLatch,
                                      MVCLifeCycleManager mvcLifeCycleManager) {
        this(new LinkedBlockingQueue<ApplicationState>(),
             new ApplicationHandler(),
             mvcLifeCycleManager,
             shutdownLatch);
    }
    
    // Testing hook
    ApplicationLifeCycleThread(BlockingQueue<ApplicationState> queue,
                               ApplicationHandler handler,
                               MVCLifeCycleManager mvcLifeCycleManager,
                               CountDownLatch shutdownLatch)
    {
        this.queue = queue;
        this.handler = handler;
        
        this.shutdownLatch = shutdownLatch;
        
        this.mvcLifeCycleManager= mvcLifeCycleManager;
        
        this.stateChangeListener = new StateChangeListener();
    }

    @Override
    public void run() {
        try {
            startEventLoop();
            
        } catch (InterruptedException e) {
            // TODO: log and shutdown, this should never happen
        }
    }
    
    private void startEventLoop() throws InterruptedException {
        running = true;
        ApplicationState state = null;
        do {
            state = queue.take();
            if (state == ApplicationState.DESTROYED) {
                running = false;
            }
            
            handleState(state);
            
        } while(running);
    }
    
    // Testing hook
    void handleState(ApplicationState state) {
        switch (state) {
        case CREATE: {
            application = handler.create();
        } break;
            
        case CREATED: {
            queueState(ApplicationState.INITIALIZE);
        } break;
       
        case INITIALIZE: {
            handler.init();
        } break;
        
        case INITIALIZED: {
            queueState(ApplicationState.START);
        } break;
        
        case START: {
            handler.start();
        } break;
        
        case STARTED: {

            // now deal with MVC
            mvcLifeCycleManager.shutdownProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    handler.stop();
                }
            });
            mvcLifeCycleManager.setPlatform(application.getPlatform());
            mvcLifeCycleManager.start();
        } break;
        
        case STOP: {
            mvcLifeCycleManager.stop();
        } break;
        
        case STOPPED: {
            queueState(ApplicationState.DESTROY);
        } break;
        
        case DESTROY: {
            handler.destroy();  
        } break;
        
        case DESTROYED: {
            shutdownLatch.countDown();
        } break;
            
        default:
            break;
        }
    }

    @Override
    public void commenceShutdown() {
        queueState(ApplicationState.STOP);
    }
    
    public void create(ApplicationProvider provider) {
        handler.initHandler(provider);
        handler.addStateChangeListener(stateChangeListener);
        
        queueState(ApplicationState.CREATE);
    }

    // Testing hook
    void queueState(ApplicationState state) {
        try {
            queue.put(state);
            
        } catch (InterruptedException ignore) {}
    }
    
    class StateChangeListener implements ActionListener<ApplicationHandler.StateChangeEvent> {

        @Override
        public void actionPerformed(ActionEvent<StateChangeEvent> actionEvent) {
            ApplicationState state = (ApplicationState) actionEvent.getPayload();
            queueState(state);
        }
    }
}
