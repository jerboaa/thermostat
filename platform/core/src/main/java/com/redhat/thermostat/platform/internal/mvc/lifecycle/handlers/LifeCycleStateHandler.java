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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers;

import com.redhat.thermostat.beans.property.BooleanProperty;
import com.redhat.thermostat.beans.property.ChangeListener;
import com.redhat.thermostat.beans.property.ObservableValue;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.ControllerLifeCycleState;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.LifeCycle;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.Workbench;

public class LifeCycleStateHandler implements ActionListener<LifeCycle> {

    private volatile boolean shutdown;

    private BooleanProperty shutdownProperty;

    private MVCProvider provider;
    private Platform platform;

    private LifeCycleTransitionDispatcher dispatcher;
    private PlatformServiceRegistrar serviceRegistrar;

    private volatile ControllerLifeCycleState currentControllerState;

    public LifeCycleStateHandler(MVCProvider provider, Platform platform,
                                 PlatformServiceRegistrar serviceRegistrar)
    {
        this.provider = provider;
        this.platform = platform;
        shutdownProperty = new BooleanProperty(false);
        this.serviceRegistrar = serviceRegistrar;
        currentControllerState = ControllerLifeCycleState.PRE_INIT;
    }

    public MVCProvider getProvider() {
        return provider;
    }

    @Override
    public void actionPerformed(ActionEvent<LifeCycle> actionEvent) {
        switch (actionEvent.getActionId()) {
            case CREATE_VIEW: {
                platform.queueOnViewThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getView().create();
                        dispatcher.requestLifeCycleTransition(LifeCycle.VIEW_CREATED);
                    }
                });
            }
            break;

            case VIEW_CREATED: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getModel().create();
                        provider.getController().create();
                        dispatcher.requestLifeCycleTransition(LifeCycle.INIT_VIEW);
                    }
                });
            }
            break;

            case INIT_VIEW: {
                platform.queueOnViewThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getView().init(platform);
                        provider.getView().showingProperty().addListener(new ChangeListener<Boolean>() {
                            @Override
                            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                                if (currentControllerState.equals(ControllerLifeCycleState.PRE_INIT)) {
                                    return;
                                }

                                if (newValue.booleanValue() && currentControllerState.equals(ControllerLifeCycleState.STOPPED)) {
                                    dispatcher.requestLifeCycleTransition(LifeCycle.START_CONTROLLER);

                                } else if (!newValue.booleanValue() && currentControllerState.equals(ControllerLifeCycleState.STARTED)) {
                                    dispatcher.requestLifeCycleTransition(LifeCycle.STOP_CONTROLLER);
                                }
                            }
                        });
                        dispatcher.requestLifeCycleTransition(LifeCycle.VIEW_INITIALIZED);
                    }
                });
            }
            break;

            case VIEW_INITIALIZED: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getModel().init(platform);
                        provider.getController().init(platform, provider.getModel(), provider.getView());
                        currentControllerState = ControllerLifeCycleState.STOPPED;
                        dispatcher.requestLifeCycleTransition(LifeCycle.REGISTER_MVC);
                    }
                });
            }
            break;

            case REGISTER_MVC: {
                serviceRegistrar.checkAndRegister(provider);
                if (provider instanceof Workbench) {
                    dispatcher.requestLifeCycleTransition(LifeCycle.START_CONTROLLER);
                }
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        if (provider.getView().showingProperty().get()) {
                            dispatcher.requestLifeCycleTransition(LifeCycle.START_CONTROLLER);
                        }
                    }
                });
            }
            break;

            case START_CONTROLLER: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getController().start();
                        currentControllerState = ControllerLifeCycleState.STARTED;
                        dispatcher.requestLifeCycleTransition(LifeCycle.START_VIEW);
                    }
                });
            }
            break;

            case START_VIEW: {
                platform.queueOnViewThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getView().start();
                        dispatcher.requestLifeCycleTransition(LifeCycle.STARTED);
                    }
                });
            }
            break;

            case STARTED: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getController().viewStarted();
                    }
                });
            }
            break;

            case STOP_CONTROLLER: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getController().stop();
                        currentControllerState = ControllerLifeCycleState.STOPPED;
                        dispatcher.requestLifeCycleTransition(LifeCycle.STOP_VIEW);
                    }
                });
            }
            break;

            case STOP_VIEW: {
                platform.queueOnViewThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getView().stop();
                        dispatcher.requestLifeCycleTransition(LifeCycle.STOPPED);
                    }
                });
            }
            break;

            case STOPPED: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getController().viewStopped();
                        if (shutdown) {
                            dispatcher.requestLifeCycleTransition(LifeCycle.DESTROY_VIEW);
                        }
                    }
                });
            }
            break;

            case DESTROY_VIEW: {
                platform.queueOnViewThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getView().destroy();
                        dispatcher.requestLifeCycleTransition(LifeCycle.DESTROY);
                    }
                });
            }
            break;

            case DESTROY: {
                platform.queueOnApplicationThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.getController().destroy();
                        provider.getModel().destroy();
                        dispatcher.requestLifeCycleTransition(LifeCycle.DESTROYED);
                    }
                });
            }
            break;

            case DESTROYED: {
                shutdownProperty.set(true);
            }
            break;

            // not handled cases
            case PRE_CREATE:
                break;
        }
    }

    public BooleanProperty shutdownProperty() {
        return shutdownProperty;
    }

    public void destroy() {
        if (currentControllerState == ControllerLifeCycleState.STARTED) {
            shutdown = true;
            dispatcher.requestLifeCycleTransition(LifeCycle.STOP_CONTROLLER);
        } else {
            dispatcher.requestLifeCycleTransition(LifeCycle.DESTROY_VIEW);
        }
    }

    public void setDispatcher(LifeCycleTransitionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public LifeCycleTransitionDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public String toString() {
        return "[LifeCycleStateHandler: " + provider.getClass() + "]";
    }

    // Test hook
    ControllerLifeCycleState getCurrentControllerState() {
        return currentControllerState;
    }
}
