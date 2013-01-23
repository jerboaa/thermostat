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

package com.redhat.thermostat.launcher.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;

import com.redhat.thermostat.common.locale.Translate;

/*
 * Container class for launcher-added options, such as dbUrl.
 */
final class CommonOptions {

    // The launcher uses username, password and dbUrl options for establishing a
    // DB connection before the command is run. These options can be added via
    // this special option in the options section of command.properties.
    static final String OPTIONS_COMMON_DB_OPTIONS = "AUTO_DB_OPTIONS";
    // The launcher will auto-add a logLevel option if this special option is
    // specified in the command.properties option section. 
    static final String OPTIONS_COMMON_LOG_OPTION = "AUTO_LOG_OPTION";
    
    static final String LOG_LEVEL_ARG = "logLevel";
    static final String DB_URL_ARG = "dbUrl";
    static final String USERNAME_ARG = "username";
    static final String PASSWORD_ARG = "password";
    static final Set<String> ALL_COMMON_OPTIONS = new HashSet<>(4);
    
    static {
        ALL_COMMON_OPTIONS.add(LOG_LEVEL_ARG);
        ALL_COMMON_OPTIONS.add(DB_URL_ARG);
        ALL_COMMON_OPTIONS.add(USERNAME_ARG);
        ALL_COMMON_OPTIONS.add(PASSWORD_ARG);
    }
    
    static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    static List<Option> getDbOptions() {
        String dbUrlDesc = t.localize(LocaleResources.OPTION_DB_URL_DESC);
        Option dbUrlOption = new Option("d", DB_URL_ARG, true, dbUrlDesc);
        dbUrlOption.setRequired(false);
        dbUrlOption.setArgName(DB_URL_ARG);
        String usernameDesc = t.localize(LocaleResources.OPTION_USERNAME_DESC);
        Option usernameOption = new Option("u", USERNAME_ARG, true, usernameDesc);
        usernameOption.setRequired(false);
        usernameOption.setArgName(USERNAME_ARG);
        String passwordDesc = t.localize(LocaleResources.OPTION_PASSWORD_DESC);
        Option passwordOption = new Option("p", PASSWORD_ARG, true, passwordDesc);
        passwordOption.setRequired(false);
        passwordOption.setArgName(PASSWORD_ARG);
        List<Option> options = new ArrayList<>(3);
        options.add(dbUrlOption);
        options.add(usernameOption);
        options.add(passwordOption);
        return options;
    }
    
    static Option getLogOption() {
        String desc = t.localize(LocaleResources.OPTION_LOG_LEVEL_DESC);
        Option logOption = new Option("l", LOG_LEVEL_ARG, true, desc);
        logOption.setRequired(false);
        logOption.setArgName(LOG_LEVEL_ARG);
        return logOption;
    }
}

