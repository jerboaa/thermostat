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

package com.redhat.thermostat.setup.command.internal.model;

import com.redhat.thermostat.setup.command.internal.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

import java.io.IOException;

public class ThermostatQuickSetup {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private ThermostatSetup thermostatSetup;

    private String storageUsername;
    private String agentUsername;
    private String clientUsername;
    private char[] storagePassword;
    private char[] agentPassword;
    private char[] clientPassword;

    public ThermostatQuickSetup(ThermostatSetup thermostatSetup) {
        this.thermostatSetup = thermostatSetup;
        generateCredentials();
    }

    private void generateCredentials() {
        if(thermostatSetup.isWebAppInstalled()) {
            CredentialGenerator storage = new CredentialGenerator(translator.localize(LocaleResources.MONGO_USER_PREFIX).getContents());
            CredentialGenerator agent = new CredentialGenerator(translator.localize(LocaleResources.AGENT_USER_PREFIX).getContents());
            CredentialGenerator client = new CredentialGenerator(translator.localize(LocaleResources.CLIENT_USER_PREFIX).getContents());

            storageUsername = storage.generateRandomUsername();
            storagePassword = storage.generateRandomPassword();
            agentUsername = agent.generateRandomUsername();
            agentPassword = agent.generateRandomPassword();
            clientUsername = client.generateRandomUsername();
            clientPassword = client.generateRandomPassword();
        } else {
            CredentialGenerator user = new CredentialGenerator(translator.localize(LocaleResources.USER_PREFIX).getContents());

            String username = user.generateRandomUsername();
            char[] password = user.generateRandomPassword();
            storageUsername = username;
            storagePassword = password;
            agentUsername = username;
            agentPassword = password;
            clientUsername = username;
            clientPassword = password;
        }
    }

    public void run() throws IOException {
        thermostatSetup.createMongodbUser(storageUsername, storagePassword);
        if (thermostatSetup.isWebAppInstalled()) {
            thermostatSetup.createAgentUser(agentUsername, agentPassword);
            thermostatSetup.createClientAdminUser(clientUsername, clientPassword);
        }

        thermostatSetup.commit();
    }

    public String getAgentUsername() {
        return agentUsername;
    }

    public char[] getAgentPassword() {
        return agentPassword;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public char[] getClientPassword() {
        return clientPassword;
    }

}