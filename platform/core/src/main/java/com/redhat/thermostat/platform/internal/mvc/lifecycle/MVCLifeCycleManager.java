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

package com.redhat.thermostat.platform.internal.mvc.lifecycle;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.platform.MDIService;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.event.EventQueue;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers.MVCExtensionLinker;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.state.PlatformServiceRegistrar;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.state.StateMachine;
import com.redhat.thermostat.platform.mvc.MVCProvider;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

public class MVCLifeCycleManager implements MDIService {

    private BooleanProperty shutdownProperty;

    private MVCRegistry registry;
    private EventQueue eventQueue;
    private Deque<StateMachine> providers;

    private MVCExtensionLinker linker;
    private Platform platform;
    private PlatformServiceRegistrar serviceRegistrar;

    public MVCLifeCycleManager() {
        this(new MVCRegistry());
    }
    
    // Testing hook
    MVCLifeCycleManager(MVCRegistry registry) {

        shutdownProperty = new BooleanProperty(false);

        this.registry = registry;

        providers = new ConcurrentLinkedDeque<>();

        MVCListener listener = new MVCListener();
        registry.addMVCRegistryListener(listener);

        eventQueue = createEventQueue();
        linker = new MVCExtensionLinker();
    }

    // Testing hook
    EventQueue createEventQueue() {
        return new EventQueue("MVC Life Cycle Thread");
    }

    public void start() {
        ExecutorService executor =
                platform.getAppService().getApplicationExecutor();
        serviceRegistrar = new PlatformServiceRegistrar(executor);

        eventQueue.start();
        registry.start();
    }
    
    public void stop() {
        registry.stop();

        for (StateMachine stateMachine : providers) {
            stateMachine.stop();
        }

        doShutdown();
    }

    private void doShutdown() {
        // TODO
    }

    public BooleanProperty shutdownProperty() {
        BooleanProperty property = new BooleanProperty();
        property.bind(shutdownProperty);
        return property;
    }

    public void startLifeCycle(final MVCProvider provider) {

        StateMachine stateMachine = new StateMachine(provider, platform,
                                                     serviceRegistrar,
                                                     eventQueue, linker);
        providers.add(stateMachine);
        stateMachine.start();
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
        linker.setPlatform(platform);
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public void registerMVC(MVCProvider provider) {
        eventQueue.runLater(new StartProvider(provider));
    }

    private class StartProvider implements Runnable {

        private MVCProvider provider;
        private StartProvider(MVCProvider provider) {
            this.provider = provider;
        }

        @Override
        public void run() {
            startLifeCycle(provider);
        }
    }

    class MVCListener implements ActionListener<ThermostatExtensionRegistry.Action> {

        @Override
        public void actionPerformed(ActionEvent<Action> actionEvent) {
            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED: {
                // start the triplet life cycle handler
                MVCProvider provider = (MVCProvider) actionEvent.getPayload();
                eventQueue.runLater(new StartProvider(provider));

            } break;
                
            case SERVICE_REMOVED: {
                // Removed services should already have
                // their life cycle methods called, at this point we
                // don't need to do anything
            } break;

            default:
                break;
            }
        }
    }
}
