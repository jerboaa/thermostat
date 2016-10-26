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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.state;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.platform.annotations.PlatformService;
import com.redhat.thermostat.platform.mvc.MVCComponent;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class PlatformServiceRegistrar {
    private ExecutorService executor;
    private BundleContext context;

    public PlatformServiceRegistrar(ExecutorService executor) {
        this(executor, FrameworkUtil.getBundle(PlatformServiceRegistrar.class).
                       getBundleContext());
    }

    // Testing hook
    PlatformServiceRegistrar(ExecutorService executor, BundleContext context) {
        this.executor = executor;
        this.context = context;
    }

    public void checkAndRegister(MVCProvider provider) {
        checkAndRegisterImpl(provider.getModel());
        checkAndRegisterImpl(provider.getController());
        checkAndRegisterImpl(provider.getView());
    }

    // Testing hook
    void checkAndRegisterImpl(MVCComponent component) {
        List<String> serviceIds =
                getPlatformServiceIDs(component.getClass());
        if (!serviceIds.isEmpty()) {
            registerComponent(component, serviceIds);
        }
    }

    // Testing hook
    void registerComponent(final MVCComponent component,
                           final List<String> serviceIds)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                Class<? extends MVCComponent> clazz = component.getClass();
                Dictionary<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, clazz.getName());

                for (String serviceId : serviceIds) {
                    context.registerService(serviceId, component, properties);
                }
            }
        };
        executor.execute(runnable);
    }

    // Testing hook
    List<String> getPlatformServiceIDs(Class<? extends MVCComponent> clazz) {

        List<String> services = new ArrayList<>();

        boolean isService = clazz.isAnnotationPresent(PlatformService.class);
        if (isService) {
            PlatformService service = clazz.getAnnotation(PlatformService.class);
            Class<? extends MVCComponent>[] components = service.service();
            for (Class<? extends MVCComponent> component : components) {
                services.add(component.getName());
            }

            String value = service.value();
            if (services.isEmpty() && !value.isEmpty()) {
                services.add(value);
            }

            if (services.isEmpty()) {
                services.add(clazz.getName());
            }
        }

        return services;
    }
}
