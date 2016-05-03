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

package com.redhat.thermostat.setup.command.internal.cli;

import java.io.IOException;
import java.io.PrintWriter;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.tools.StorageAuthInfoGetter;
import com.redhat.thermostat.setup.command.internal.LocaleResources;
import com.redhat.thermostat.setup.command.internal.model.UserCredsValidator;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

class UsernameCredentialsReader {

    private static final int MAX_TRIES = 100;
    private static final LocalizedString UNUSED = new LocalizedString("ignored");
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final Console console;
    private final LocalizedString usernamePrompt;
    private final PrintWriter errWriter;
    
    UsernameCredentialsReader(Console console, LocalizedString usernamePrompt) {
        this.console = console;
        this.usernamePrompt = usernamePrompt;
        this.errWriter = new PrintWriter(console.getError());
    }
    
    String read() throws IOException {
        StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console, usernamePrompt, UNUSED);
        boolean isValid = false;
        UserCredsValidator validator = new UserCredsValidator();
        String username = null;
        int currTry = 0;
        while (!isValid && currTry < MAX_TRIES) {
            username = getter.getUserName(null);
            if (username == null) {
                throw new IOException("Unexpected EOF while reading username.");
            }
            currTry++;
            try {
                validator.validateUsername(username);
                isValid = true;
            } catch (IllegalArgumentException e) {
                printError(LocaleResources.CLI_SETUP_USERNAME_INVALID, username);
                // continue loop
            }
        }
        // If we have reached maximum tries then we might still be invalid
        if (!isValid) {
            throw new IOException("Tried " + MAX_TRIES + " times and got invalid input each time.");
        }
        return username;
    }

    private void printError(LocaleResources resource, String...strings) {
        errWriter.println(t.localize(resource, strings).getContents());
        errWriter.flush();
    }
    
}
