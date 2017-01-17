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

package com.redhat.thermostat.platform.internal.application;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.platform.ApplicationProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

public class ApplicationRegistry {

    public static enum RegistryEvent {
        APPLICATION_REGISTERED,
        APPLICATION_REMOVED,

        ;
    }

    private static final Logger logger = LoggingUtils.getLogger(ApplicationRegistry.class);

    private RegistryHandler handler;
    private Registry registry;

    private Map<String, ApplicationProvider> providers;

    private ActionNotifier<RegistryEvent> actionNotifier;

    public ApplicationRegistry(BundleContext context) {
        providers = new HashMap<>();

        handler = createRegistryHandler(context);
        actionNotifier = createNotifier();
        registry = createRegistry(actionNotifier);

        ApplicationListener listener = new ApplicationListener();
        handler.addActionListener(listener);
    }

    // Testing hook
    ActionNotifier<RegistryEvent> createNotifier() {
        return new ActionNotifier<>(this);
    }

    // Testing hook
    Registry createRegistry(ActionNotifier<RegistryEvent> actionNotifier) {
        return new Registry(providers, actionNotifier);
    }

    // Testing hook
    RegistryHandler createRegistryHandler(BundleContext context) {
        RegistryHandler handler = null;
        try {
            handler = new RegistryHandler(context);

        } catch (InvalidSyntaxException ex) {
            // the handler could throw the exception, yes, but it doesn't since
            // we know the underlying search filter is well formed... this
            // should be fixed in the registry API... the exception should
            // be unchecked really...
            throw new RuntimeException(ex);
        }
        return handler;
    }

    public boolean containsProvider(String providerClassName) {
        return providers.containsKey(providerClassName);
    }

    public ApplicationProvider getProvider(String providerClassName) {
        return providers.get(providerClassName);
    }

    public void addRegistryEventListener(ActionListener<RegistryEvent> listener) {
        actionNotifier.addActionListener(listener);
    }

    public void removeRegistryEventListener(ActionListener<RegistryEvent> listener) {
        actionNotifier.removeActionListener(listener);
    }

    public void start() {
        handler.start();
    }
    
    public void stop() {
        handler.stop();
    }
    
    static class RegistryHandler extends ThermostatExtensionRegistry<ApplicationProvider> {
        private static final String FILTER =
                "(" + Constants.OBJECTCLASS + "=" +
                ApplicationProvider.class.getName() + ")";
        
        public RegistryHandler(BundleContext context)
                throws InvalidSyntaxException
        {
            super(context, FILTER, ApplicationProvider.class);
        }
    }
    
    private class ApplicationListener implements ActionListener<RegistryHandler.Action> {
        @Override
        public void actionPerformed(ActionEvent<RegistryHandler.Action> actionEvent) {
            ApplicationProvider application = (ApplicationProvider) actionEvent.getPayload();
            switch (actionEvent.getActionId()) {
                case SERVICE_ADDED:
                    logger.log(Level.CONFIG, "application added: " + application);
                    registry.registerApplication(application);
                    break;

                case SERVICE_REMOVED:
                    logger.log(Level.CONFIG, "application removed: " + application);
                    registry.unregisterApplication(application);
                    break;

                default:
                    break;
            }
        }
    }

    public static String getKeyFor(Object target) {
        return target.getClass().getCanonicalName() + ".class";
    }

    static class Registry {
        private Map<String, ApplicationProvider> providers;
        private ActionNotifier<RegistryEvent> actionNotifier;

        public Registry(Map<String, ApplicationProvider> providers,
                        ActionNotifier<RegistryEvent> actionNotifier)
        {
            this.providers = providers;
            this.actionNotifier = actionNotifier;
        }

        public void registerApplication(ApplicationProvider provider) {
            String key = getKeyFor(provider);
            providers.put(key, provider);

            actionNotifier.fireAction(RegistryEvent.APPLICATION_REGISTERED,
                                      provider);
        }

        public void unregisterApplication(ApplicationProvider provider) {
            String key = getKeyFor(provider);
            providers.remove(key);

            actionNotifier.fireAction(RegistryEvent.APPLICATION_REMOVED,
                                      provider);
        }
    }
}
