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

package com.redhat.thermostat.launcher.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.DbService;

public class ShellPrompt {

    public static final String DEFAULT_CONNECTED_TOKEN = "+";
    public static final String DEFAULT_DISCONNECTED_TOKEN = "-";

    private static final String SHELL_PROMPT_FORMAT_KEY = "shell-prompt";
    private static final String CONNECT_PROMPT_FORMAT_KEY = "connected-prompt";
    private static final String DISCONNECT_PROMPT_FORMAT_KEY = "disconnected-prompt";

    private static final String DEFAULT_PROMPT_FORMAT = "Thermostat %connect > ";

    private static final Logger logger = LoggingUtils.getLogger(ShellCommand.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private Map<String, String> promptConfig = new HashMap<>();

    private enum Tokens {
        CONNECT("%connect", DEFAULT_DISCONNECTED_TOKEN), //Default to disconnected

        CONNECTION_URL("%url", ""),

        PROTOCOL("%protocol", ""),
        HOST("%host", ""),
        PORT("%port", ""),

        USER("%user", ""),
        //TODO: implement tokens below
        SECURE("%secure", ""),
        ;

        private String token;
        private String defaultValue;

        private Tokens(String token, String defaultValue) {
            this.token = token;
            this.defaultValue = defaultValue;
        }

        public String getToken() {
            return this.token;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }
    }

    public ShellPrompt() {
        promptConfig.put(SHELL_PROMPT_FORMAT_KEY, DEFAULT_PROMPT_FORMAT);
        promptConfig.put(CONNECT_PROMPT_FORMAT_KEY, DEFAULT_CONNECTED_TOKEN);
        promptConfig.put(DISCONNECT_PROMPT_FORMAT_KEY, DEFAULT_DISCONNECTED_TOKEN);
    }

    public String getPrompt() {
        String format = promptConfig.get(SHELL_PROMPT_FORMAT_KEY);
        return replaceTokens(format);
    }

    private String replaceTokens(String prompt) {
        for (Tokens t : Tokens.values()) {
            String token = t.getToken();
            prompt = prompt.replaceAll(token,
                    promptConfig.containsKey(token) ? promptConfig.get(token) : t.getDefaultValue());
        }
        return prompt;
    }

    public void storageConnected(DbService dbService) {
        String connectionURL = dbService.getConnectionUrl();

        buildUrlTokens(connectionURL);
        promptConfig.put(Tokens.USER.getToken(), dbService.getUserName());

        buildConnectToken(CONNECT_PROMPT_FORMAT_KEY);
    }

    public void storageDisconnected() {
        clearUrlTokens();
        promptConfig.put(Tokens.USER.getToken(), Tokens.USER.getDefaultValue());

        buildConnectToken(DISCONNECT_PROMPT_FORMAT_KEY);
    }

    private void buildConnectToken(String promptFormatKey) {
        //Reset connect token beforehand since replaceTokens will use the value
        promptConfig.put(Tokens.CONNECT.getToken(), "");
        promptConfig.put(Tokens.CONNECT.getToken(), replaceTokens(promptConfig.get(promptFormatKey)));
    }

    private void buildUrlTokens(String connectionURL) {
        try {
            URI uri = new URI(connectionURL);
            promptConfig.put(Tokens.CONNECTION_URL.getToken(), uri.toString());
            promptConfig.put(Tokens.PROTOCOL.getToken(), uri.getScheme());
            promptConfig.put(Tokens.HOST.getToken(), uri.getHost());

            int port = uri.getPort();
            promptConfig.put(Tokens.PORT.getToken(), (port >= 0) ? String.valueOf(port) : "");
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, t.localize(LocaleResources.INVALID_DB_URL, connectionURL).getContents());
            promptConfig.put(Tokens.CONNECT.getToken(), connectionURL);
        }
    }

    private void clearUrlTokens() {
        promptConfig.put(Tokens.CONNECTION_URL.getToken(), Tokens.CONNECTION_URL.getDefaultValue());
        promptConfig.put(Tokens.PROTOCOL.getToken(), Tokens.PROTOCOL.getDefaultValue());
        promptConfig.put(Tokens.HOST.getToken(), Tokens.HOST.getDefaultValue());
        promptConfig.put(Tokens.PORT.getToken(), Tokens.PORT.getDefaultValue());
    }

    public void overridePromptConfig(Map<String, String> newConfig) {
        for (Map.Entry<String, String> entry : newConfig.entrySet()) {
            this.promptConfig.put(entry.getKey(), entry.getValue());
        }

        if (newConfig.containsKey(DISCONNECT_PROMPT_FORMAT_KEY)) {
            buildConnectToken(DISCONNECT_PROMPT_FORMAT_KEY);
        }
    }
}
