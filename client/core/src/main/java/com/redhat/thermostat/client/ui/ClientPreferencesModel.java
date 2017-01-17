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

package com.redhat.thermostat.client.ui;

import java.io.IOException;

import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ClientPreferencesModel {

    private ClientPreferences prefs;
    private Keyring keyring;
    private char[] password;

    public ClientPreferencesModel(Keyring keyring, ClientPreferences prefs) {
        this.prefs = prefs;
        this.keyring = keyring;
    }

    public boolean getSaveEntitlements() {
        return prefs.getSaveEntitlements();
    }
    
    public void setSaveEntitlements(boolean save) {
        prefs.setSaveEntitlements(save);
    }
    
    public String getConnectionUrl() {
        return prefs.getConnectionUrl();
    }

    public void setConnectionUrl(String url) {
        prefs.setConnectionUrl(url);
    }

    public char[] getPassword() {
        if (getSaveEntitlements()) {
            return keyring.getPassword(getConnectionUrl(), getUserName());
        } else {
            return password;
        }
    }

    public String getUserName() {
        return prefs.getUserName();
    }

    public void setCredentials(String userName, char[] password) {
        prefs.setUserName(userName);
        this.password = password;
    }
    
    public void flush() throws IOException {
        if (getSaveEntitlements()) {
            keyring.savePassword(getConnectionUrl(), getUserName(), password);
        } 
        prefs.flush(); // preferences are always written back on flush()
    }

    public ClientPreferences getPreferences() {
        return prefs;
    }
}

