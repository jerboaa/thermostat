/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.cli;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.annotations.ExtensionPoint;

/**
 * Represents a command on the command line.
 * <p>
 * In order to be runnable, a command must be registered as an OSGi service
 * with the {@link #NAME} set to the value of {@link #getName()}. If this is
 * done directly, the developer is then responsible for enabling and disabling
 * it at the appropriate times.
 * <p>
 * Bundles which provide {@link Command}s may choose to have their BundleActivator
 * extend from {@link CommandLoadingBundleActivator} and in so doing avoid needing to
 * explicitly register their {@link Command} implementations as services.
 * <p>
 * It is also possible to use an instance of {@link CommandRegistry}, registering the
 * {@link Command}s when the bundle starts and and unregistering them when the
 * bundle stops.
 * <p>
 * Most {@link Command} implementations will want to choose one of the {@link AbstractCommand}
 * or {@link AbstractStateNotifyingCommand} classes to descend from, as they provide
 * sensible default implementations of most methods and/or provide some other functionality.
 * <p>
 * @see CommandLoadingBundleActivator
 * @see CommandRegistry
 * @see AbstractCommand
 * @see AbstractStateNotifyingCommand
 */
@ExtensionPoint
public interface Command {

    public static final String NAME = "COMMAND_NAME";

    /**
     * Execute the command
     */
    public void run(CommandContext ctx) throws CommandException;

    /**
     * Returns a name for this command. This will be used by the user to select
     * this command.
     */
    public String getName();

    /**
     * A short description for the command indicating what it does.
     */
    public String getDescription();

    /**
     * How the user should invoke this command
     */
    public String getUsage();

    /**
     * Returns the Options that the command is prepared to handle.
     * If the user provides unknown or malformed arguments, this command will
     * not be invoked.
     */
    public Options getOptions();

    /**
     * Whether the command depends on {@link Storage}
     * @return true if {@link Storage} is required.
     */
    public boolean isStorageRequired();

    /**
     * Whether the command is available to be invoked from within the shell.
     * @return true if the command can be invoked from within the shell
     */
    public boolean isAvailableInShell();

    /**
     * Indicates if the command is available to be invoked from outside the
     * shell (as an argument to the main thermostat program).
     * @return true if can command can be invoked from the command line
     */
    public boolean isAvailableOutsideShell();

}

