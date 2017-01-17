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

package com.redhat.thermostat.setup.command.internal.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.tools.StorageAuthInfoGetter;
import com.redhat.thermostat.setup.command.internal.LocaleResources;
import com.redhat.thermostat.setup.command.internal.model.UserCredsValidator;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

class PasswordCredentialsReader {

    private static final int MAX_TRIES = 100;
    private static final LocalizedString UNUSED = new LocalizedString("ignored");
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final Console console;
    private final LocalizedString passwordPrompt;
    private final LocalizedString confirmPasswordPrompt;
    private final PrintWriter errWriter;
    
    PasswordCredentialsReader(Console console, LocalizedString passwordPrompt, LocalizedString confirmPasswordPrompt) {
        this.console = console;
        this.passwordPrompt = passwordPrompt;
        this.confirmPasswordPrompt = confirmPasswordPrompt;
        this.errWriter = new PrintWriter(console.getError());
    }
    
    // Read in passwords twice. Once the initial password, a second time we
    // expect the same content again for confirmation.
    char[] readPassword() throws IOException {
        char[] password;
        char[] confirmation;
        boolean isValid = false;
        int currTry = 0;
        do {
            StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console, UNUSED, passwordPrompt);
            password = getter.getPassword(null);
            if (password == null) {
                throw new IOException("Unexpected EOF while reading password.");
            }
            StorageAuthInfoGetter confirmGetter = new StorageAuthInfoGetter(console, UNUSED, confirmPasswordPrompt);
            confirmation = confirmGetter.getPassword(null);
            if (confirmation == null) {
                throw new IOException("Unexpected EOF while reading password confirmation.");
            }
            currTry++;
            try {
                verifyPassword(password, confirmation);
                Arrays.fill(confirmation, '\0');
                confirmation = null;
                isValid = true;
            } catch (InvalidPasswordException e) {
                printError(LocaleResources.CLI_SETUP_PASSWORD_INVALID);
                clearPasswords(password, confirmation);
            } catch (PasswordMismatchException e) {
                printError(LocaleResources.CLI_SETUP_PASSWORD_MISMATCH);
                clearPasswords(password, confirmation);
            }
        } while (!isValid && currTry < MAX_TRIES);
        // If we have reached maximum tries then we might still be invalid
        if (!isValid) {
            throw new IOException("Tried " + MAX_TRIES + " times and got invalid input each time.");
        }
        return password;
    }
    
    private void clearPasswords(char[] password, char[] confirmation) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        if (confirmation != null) {
            Arrays.fill(confirmation, '\0');
        }
        password = null;
        confirmation = null;
    }

    private void printError(LocaleResources resource, String...strings) {
        errWriter.println(t.localize(resource, strings).getContents());
        errWriter.flush();
    }
    
    private void verifyPassword(char[] password, char[] confirmation) throws InvalidPasswordException, PasswordMismatchException {
        UserCredsValidator validator = new UserCredsValidator();
        try {
            validator.validatePassword(password);
            validator.validatePassword(confirmation);
        } catch (IllegalArgumentException e) {
            throw new InvalidPasswordException();
        }
        // passwords are now non-null and not empty
        checkPasswordsMatch(password, confirmation);
    }

    void checkPasswordsMatch(char[] first, char[] second) throws PasswordMismatchException {
        // Auth getter never returns null. Use as precondition
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        
        if (first.length != second.length) {
            throw new PasswordMismatchException();
        }
        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                throw new PasswordMismatchException();
            }
        }
    }
    
    @SuppressWarnings("serial")
    private static class PasswordMismatchException extends Exception {
        // nothing
    }
    
    @SuppressWarnings("serial")
    private static class InvalidPasswordException extends Exception {
        // nothing
    }
}
