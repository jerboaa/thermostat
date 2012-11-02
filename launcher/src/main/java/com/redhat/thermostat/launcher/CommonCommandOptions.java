/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.launcher;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandInfo;

public class CommonCommandOptions {

    public static final String DB_URL_ARG = "dbUrl";
    public static final String USERNAME_ARG = "username";
    public static final String PASSWORD_ARG = "password";

    public static final String DB_URL_DESC = "the URL of the storage to connect to";
    public static final String USERNAME_DESC = "the username to use for authentication";
    public static final String PASSWORD_DESC = "the password to use for authentication";

    public static final String LOG_LEVEL_ARG = "logLevel";
    private static final String LOG_LEVEL_DESC = "log level";

    public Options getOptionsFor(Command cmd) {

        Options options = cmd.getOptions();
        addDbUrlOptionForStorageCommand(cmd, options);
        addLogLevelOption(options);
        addOptionalAuthenticationArguments(options);
        return options;
    }

    public Options getOptionsFor(CommandInfo info) {
        // TODO make storageRequired part of CommandInfo (in command.properties)
        Options options = info.getOptions();
        addLogLevelOption(options);
        addOptionalAuthenticationArguments(options);
        return options;
    }

    private void addDbUrlOptionForStorageCommand(Command cmd, Options options) {
        if (cmd.isStorageRequired()) {
            Option option = new Option("d", DB_URL_ARG, true, DB_URL_DESC);
            option.setRequired(false);
            options.addOption(option);
        }
    }

    private void addLogLevelOption(Options options) {
        Option option = new Option(null, LOG_LEVEL_ARG, true, LOG_LEVEL_DESC);
        option.setRequired(false);
        options.addOption(option);
    }

    private void addOptionalAuthenticationArguments(Options options) {

        Option userOption = new Option(null, USERNAME_ARG, true, USERNAME_DESC);
        userOption.setRequired(false);
        options.addOption(userOption);
        Option passwordOption = new Option(null, PASSWORD_ARG, true, PASSWORD_DESC);
        passwordOption.setRequired(false);
        options.addOption(passwordOption);
    }

}

