/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.io.IOException;
import java.util.logging.Logger;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.tools.StorageAuthInfoGetter;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.utils.keyring.Keyring;

public class InteractiveStorageCredentials implements StorageCredentials {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(InteractiveStorageCredentials.class);

    private ClientPreferences prefs;
    private Keyring keyring;
    private String url;
    private StorageAuthInfoGetter getter;

    public InteractiveStorageCredentials(ClientPreferences prefs, Keyring keyring, String url, Console console) {
        this.prefs = prefs;
        this.keyring = keyring;
        this.url = url;
        try {
            this.getter = new StorageAuthInfoGetter(console);
        } catch (IOException e) {
            String message = "IOException while creating interactive authentication credential getter.";
            logger.severe(message);
            throw new InteractiveException(message, e);
        }
    }

    @Override
    public String getUsername() {
        String username = null;
        if (url.equals(prefs.getConnectionUrl())) {
            prefs.getUserName();
        }
        if (username == null) {
            try {
                username = getter.getUserName(url);
            } catch (IOException e) {
                throw new InteractiveException(t.localize(LocaleResources.LAUNCHER_USER_AUTH_PROMPT_ERROR).getContents(), e);
            }
        }
        return username.length() == 0 ? null : username;
    }

    @Override
    public char[] getPassword() {
        char[] password = null;
        if (url.equals(prefs.getConnectionUrl())) {
            keyring.getPassword(url, prefs.getUserName());
        }
        if (password == null) {
            try {
                password = getter.getPassword(url);
            } catch (IOException e) {
                throw new InteractiveException(t.localize(LocaleResources.LAUNCHER_USER_AUTH_PROMPT_ERROR).getContents(), e);
            }
        }
        return password.length == 0 ? null : password;
    }

    class InteractiveException extends RuntimeException {

        private static final long serialVersionUID = -7921653973987512258L;

        InteractiveException(String message, Exception cause) {
            super(message, cause);
        }
    }
}

