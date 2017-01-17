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

package com.redhat.thermostat.common.tools;

import java.io.IOException;
import java.util.Arrays;

import jline.console.ConsoleReader;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * Utility class to isolate the user-facing strings necessary when prompting user
 * for authentication parameters for storage connection.  Provides simple convenience
 * wrappers for jline.console.ConsoleReader methods, and localization of prompt.
 */
public class StorageAuthInfoGetter {

    private static final int EOF = -1;
    private static final int PW_SIZE_INCREMENT = 16;

    private final ConsoleReader reader;
    private final Translate<LocaleResources> t;
    private final LocalizedString userPrompt;
    private final LocalizedString passwordPrompt;
    
    /**
     * Constructor. Allows for setting of prompt(s).
     * @param console The console to use.
     * @param userPrompt The prompt printed when asking for username.
     * @param passwordPrompt The prompt printed when asking for password.
     * @throws IOException
     */
    public StorageAuthInfoGetter(Console console, LocalizedString userPrompt,
                                 LocalizedString passwordPrompt) throws IOException {
        reader = new ConsoleReader(console.getInput(), console.getOutput());
        t = LocaleResources.createLocalizer();
        this.passwordPrompt = passwordPrompt;
        this.userPrompt = userPrompt;
    }

    public StorageAuthInfoGetter(Console console) throws IOException {
        this(console, null, null);
    }

    /**
     * Prompt the user for username necessary for connecting to a given url.
     * @param url
     * @return The username entered by the user.  This could be the empty string.
     * @throws IOException 
     */
    public String getUserName(String url) throws IOException {
        LocalizedString prompt = userPrompt;
        if (prompt == null) {
            prompt = t.localize(LocaleResources.USERNAME_PROMPT, url); 
        }
        String name = reader.readLine(prompt.getContents());
        return name;
    }

    /**
     * Prompt the user for password necessary for connecting to a given url.
     * The caller is responsible for clearing the char[] to minimize the time during
     * which the password is available in cleartext in the heap.
     * @param url
     * @return The password entered by the user.  This could be the empty string.
     * @throws IOException 
     */
    public char[] getPassword(String url) throws IOException {
        LocalizedString prompt = passwordPrompt;
        if (prompt == null) {
            prompt = t.localize(LocaleResources.PASSWORD_PROMPT, url); 
        }
        char[] password = new char[PW_SIZE_INCREMENT];
        reader.setHistoryEnabled(false);
        reader.print(prompt.getContents());
        reader.flush();
        Character oldEcho = reader.getEchoCharacter();
        reader.setEchoCharacter('\0');
        int pwChar = reader.readCharacter();
        if (pwChar == EOF) {
            return null;
        }
        int length = 0;
        while ((char) pwChar != '\n' && (char) pwChar != '\r' && pwChar != EOF) {
            password[length] = (char) pwChar;
            length++;
            if (length >= password.length) {
                password = secureCopyCharArray(password, length + PW_SIZE_INCREMENT);
            }
            pwChar = reader.readCharacter();
        }
        reader.setEchoCharacter(oldEcho);
        reader.setHistoryEnabled(true);
        reader.println();
        reader.flush();
        password = secureCopyCharArray(password, length);
        return password;
    }

    // returns new array, fills original with '\0'
    private char[] secureCopyCharArray(char[] original, int newLength) {
        char[] newArray = Arrays.copyOf(original, newLength);
        Arrays.fill(original, '\0');
        return newArray;
    }
}

