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

package com.redhat.thermostat.common.config;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.redhat.thermostat.utils.keyring.Credentials;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ClientPreferences {

    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String CONNECTION_URL = "connection-url";
    static final String SAVE_ENTITLEMENTS = "save-entitlements";
    static final String DEFAULT_CONNECTION_URL = "mongodb://127.0.0.1:27518";
    
    private final Preferences prefs;
    
    private Keyring keyring;
    
    private Credentials userCredentials;
    
    public ClientPreferences(Keyring keyring) {
        this(Preferences.userRoot().node("thermostat"), keyring);
    }

    ClientPreferences(Preferences prefs, Keyring keyring) {
        this.prefs = prefs;
        this.keyring = keyring;
        this.userCredentials = new Credentials();
        userCredentials.setDescription("thermostat keychain");

        // load the initial defaults
        userCredentials.setUserName(prefs.get(USERNAME, ""));
        this.userCredentials.setPassword("");
        if (getSaveEntitlements()) {
            keyring.loadPassword(userCredentials);
        }
    }

    public boolean getSaveEntitlements() {
        return prefs.getBoolean(SAVE_ENTITLEMENTS, false);
    }
    
    public void setSaveEntitlements(boolean save) {
        prefs.putBoolean(SAVE_ENTITLEMENTS, save);
    }
    
    public String getConnectionUrl() {
        return prefs.get(CONNECTION_URL, DEFAULT_CONNECTION_URL);
    }

    public String getPassword() {
        if (getSaveEntitlements()) {
            keyring.loadPassword(userCredentials);
        }
        return userCredentials.getPassword();
    }

    public String getUserName() {        
        return prefs.get(USERNAME, "");
    }

    public void setConnectionUrl(String url) {
        prefs.put(CONNECTION_URL, url);
    }

    public void setCredentials(String userName, String password) {
        
        userCredentials.setUserName(userName);
        userCredentials.setPassword(password);
        prefs.put(USERNAME, userName);
        
        if (getSaveEntitlements()) {
            keyring.savePassword(userCredentials);
        }
    }
    
    public void flush() throws BackingStoreException {
        prefs.flush();
        userCredentials.setUserName(getUserName());
        if (getSaveEntitlements()) {
            keyring.loadPassword(userCredentials);
        }
    }
}

