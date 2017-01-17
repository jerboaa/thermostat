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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.annotations.Extension;
import com.redhat.thermostat.platform.annotations.ExtensionPoint;
import com.redhat.thermostat.platform.mvc.MVCComponent;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class MVCExtensionLinker {

    private static final Logger logger = LoggingUtils.getLogger(MVCExtensionLinker.class);

    private HashMap<Class<?>, List<MethodInfo>> extensions;
    private HashMap<Class<?>, List<MVCComponent>> extensionConsumers;

    private Platform platform;

    private static class MethodInfo {
        Method method;
        Class<?> extension;
        MVCComponent provider;
    }

    public MVCExtensionLinker() {
        extensions = new HashMap<>();
        extensionConsumers = new HashMap<>();
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public void link(MVCProvider provider) {

        checkExtensions(provider.getController());
        checkExtensions(provider.getModel());
        checkExtensions(provider.getView());
    }

    void link(final MVCComponent consumer, final MethodInfo info) {

        Runnable action = new Runnable() {
            @Override
            public void run() {
                try {
                    info.method.setAccessible(true);
                    info.method.invoke(info.provider, consumer);

                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                               "Cannot link provider " + info.provider +
                               " with consumer " + consumer, e);
                }
            }
        };
        if (info.provider instanceof View) {
            platform.queueOnViewThread(action);
        } else {
            platform.queueOnApplicationThread(action);
        }
    }

    void checkExtensions(MVCComponent component) {
        Class<? extends MVCComponent> componentClass = component.getClass();
        if (componentClass.isAnnotationPresent(Extension.class)) {
            // before adding, let's first see if we have any component exporting
            // this extension

            Class<?> extension = componentClass.getAnnotation(Extension.class).value();

            if (extensions.containsKey(extension)) {
                for (MethodInfo info : extensions.get(extension)) {
                    link(component, info);
                }
            }

            List<MVCComponent> consumers = extensionConsumers.get(extension);
            if (consumers == null) {
                consumers = new ArrayList<>();
                extensionConsumers.put(extension, consumers);
            }
            consumers.add(component);
        }

        // check what extension this component exports
        for (Method method : component.getClass().getDeclaredMethods()) {
            ExtensionPoint extension = method.getAnnotation(ExtensionPoint.class);
            if (extension != null) {
                MethodInfo info = new MethodInfo();
                info.method = method;
                info.extension = extension.value();
                info.provider = component;

                List<MethodInfo> infos = extensions.get(info.extension);
                if (infos == null) {
                    infos = new ArrayList<>();
                    extensions.put(info.extension, infos);
                }
                infos.add(info);

                // check if this component is exporting something that another
                // components wants to use
                List<MVCComponent> consumers = extensionConsumers.get(info.extension);
                if (consumers != null) {
                    for (MVCComponent consumer : consumers) {
                        link(consumer, info);
                    }
                }
            }
        }
    }
}
