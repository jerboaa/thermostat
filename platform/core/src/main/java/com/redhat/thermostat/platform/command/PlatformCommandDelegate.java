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

package com.redhat.thermostat.platform.command;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.platform.internal.application.ApplicationInfo;
import com.redhat.thermostat.platform.internal.application.ApplicationRegistry;
import com.redhat.thermostat.platform.internal.application.ConfigurationManager;
import com.redhat.thermostat.platform.internal.application.lifecycle.ApplicationLifeCycleManager;

import static com.redhat.thermostat.platform.internal.locale.LocaleResources.COLUMN_SEPARATOR;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.COMMAND_H_SEPARATOR;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.HEADER_COMMAND;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.HEADER_PROVIDER;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.INVALID_PLATFORM;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.PROVIDER_H_SEPARATOR;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.V_SEPARATOR;
import static com.redhat.thermostat.platform.internal.locale.LocaleResources.translate;

class PlatformCommandDelegate {

    static void listApplications(CommandContext ctx, ConfigurationManager manager) {
        ApplicationInfo infos = manager.getApplicationConfigs();
        TableRenderer renderer = new TableRenderer(3, 2);
        renderer.printLine(translate(HEADER_COMMAND),
                           translate(COLUMN_SEPARATOR),
                           translate(HEADER_PROVIDER));
        renderer.printLine(translate(COMMAND_H_SEPARATOR),
                           translate(V_SEPARATOR),
                           translate(PROVIDER_H_SEPARATOR));
        for (ApplicationInfo.Application info : infos.applications) {
            renderer.printLine(info.name, translate(COLUMN_SEPARATOR), info.provider);
        }
        renderer.render(ctx.getConsole().getOutput());
    }

    static void startPlatform(CommandContext ctx, String id,
                              ConfigurationManager manager,
                              ApplicationRegistry registry,
                              ApplicationLifeCycleManager lifeCycleManager)
    {
        ApplicationInfo.Application info = manager.getApplicationConfig(id);
        if (info == null) {
            ctx.getConsole().getOutput().println(translate(INVALID_PLATFORM, id));
            return;
        }

        lifeCycleManager.setTarget(info);
        lifeCycleManager.registerShutdownService();
        lifeCycleManager.registerMDIService();
        registry.addRegistryEventListener(lifeCycleManager);
        registry.start();
    }

    static void stopPlatform(ApplicationRegistry registry) {
        registry.stop();
    }

    public static void executePlatform(CommandContext ctx, String id,
                                       ConfigurationManager manager,
                                       ApplicationRegistry registry,
                                       ApplicationLifeCycleManager lifeCycleManager)
    {
        startPlatform(ctx, id, manager, registry, lifeCycleManager);

        lifeCycleManager.execute();

        stopPlatform(registry);
    }
}
