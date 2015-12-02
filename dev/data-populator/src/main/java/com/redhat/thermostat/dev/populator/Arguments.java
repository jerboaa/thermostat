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

package com.redhat.thermostat.dev.populator;

import java.util.HashMap;
import java.util.Map;

class Arguments {
    
    private static final String OPTIONS_PREFIX = "--";
    private static final String DB_URL_NAME = "dbUrl";
    private static final String USERNAME_NAME = "username";
    private static final String PASSWORD_NAME = "password";
    private static final String CONFIG_FILE_NAME = "config";
    
    private final Map<String, String> args;
    
    private Arguments() {
        args = new HashMap<>();
    }
    
    public String getDbUrl() {
        return args.get(DB_URL_NAME);
    }
    
    public String getUsername() {
        return args.get(USERNAME_NAME);
    }
    
    public String getPassword() {
        return args.get(PASSWORD_NAME);
    }
    
    public String getConfigFile() {
        return args.get(CONFIG_FILE_NAME);
    }
    
    private void addArgument(String name, String value) {
        args.put(name, value);
    }
    
    private void validate() throws IllegalArgumentException {
        if (args.size() != 4) {
            throw new IllegalStateException();
        }
        if (args.get(DB_URL_NAME) == null ||
                args.get(PASSWORD_NAME) == null ||
                args.get(USERNAME_NAME) == null ||
                args.get(CONFIG_FILE_NAME) == null) {
            throw new IllegalStateException();
        }
    }
    
    static Arguments processArguments(String[] args) throws IllegalArgumentException {
        if (args.length != 8) {
            throw new IllegalArgumentException("missing required arguments");
        }
        Arguments myArgs = new Arguments();
        for (int i = 0; i < args.length - 1; i += 2) {
            String optionName = args[i];
            String optionValue = args[i+1];
            if (!optionName.startsWith(OPTIONS_PREFIX)) {
                throw new IllegalArgumentException();
            } else {
                if (optionValue.startsWith(OPTIONS_PREFIX)) {
                    throw new IllegalArgumentException();
                }
                myArgs.addArgument(optionName.substring(OPTIONS_PREFIX.length()), optionValue);
            }
        }
        try {
            myArgs.validate();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Missing required arguments");
        }
        return myArgs;
    }

}
