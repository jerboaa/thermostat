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

package com.redhat.thermostat.storage.mongodb.internal;

import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * Dispatcher making sure either -d or -s has been provided.
 *
 */
class AddUserCommandDispatcher extends BaseAddUserCommand {
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final String START_STORAGE_ARG = "startStorage";
    private static final String DB_URL_ARG = "dbUrl";
    static final String COMMAND_NAME = "add-mongodb-user";
    private final BundleContext context;
    private BaseAddUserCommand command;
    
    AddUserCommandDispatcher(BundleContext context, BaseAddUserCommand cmd) {
        this.context = context;
        this.command = cmd;
    }

    AddUserCommandDispatcher(BundleContext context) {
        this(context, new AddUserCommand(context));
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        if (ctx.getArguments().hasArgument(START_STORAGE_ARG)) {
            // decorate and run
            command = new StartStopAddUserCommandDecorator(context, command);
        } else if (!ctx.getArguments().hasArgument(DB_URL_ARG)) {
            throw new CommandException(t.localize(LocaleResources.DISPATCHER_WRONG_OPTION));
        }
        command.run(ctx);
    }

}
