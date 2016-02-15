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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

public class CLISetup {

    private static final int EOF = -1;
    private static final Logger logger = LoggingUtils.getLogger(CLISetup.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final ThermostatSetup thermostatSetup;
    private final Console console;
    private final PrintWriter outWriter;
    private final PrintWriter errWriter;

    public CLISetup(ThermostatSetup setup, Console console) {
        this.thermostatSetup = setup;
        this.console = console;
        this.outWriter = new PrintWriter(console.getOutput());
        this.errWriter = new PrintWriter(console.getError());
    }

    public void run() throws CommandException {
        runSetup();
        println(LocaleResources.CLI_SETUP_FINISH_SUCCESS);
    }

    private void runSetup() throws CommandException {
        try {
            printBlurb();
            boolean shouldContinue = readContinueAnswer();
            if (!shouldContinue) {
                throw new CommandException(t.localize(LocaleResources.SETUP_CANCELLED));
            }
            readMongodbCredentials();
            if (thermostatSetup.isWebAppInstalled()) {
                readThermostatUserCredentials();
            }
            thermostatSetup.commit();
        } catch (IOException e) {
            logger.log(Level.INFO, "Setup failed. ", e);
            throw new CommandException(t.localize(LocaleResources.SETUP_FAILED), e);
        }
    }

    // package-private for testing
    void readThermostatUserCredentials() throws IOException {
        String clientUsername;
        char[] clientPassword;
        String agentUsername;
        char[] agentPassword;
        boolean isUsernameValid = false;
        LocalizedString passwordPrompt;
        LocalizedString passwordPromptRepeat;

        println(LocaleResources.CLI_SETUP_THERMOSTAT_USER_CREDS_INTRO);
        do {
            LocalizedString clientUsernamePrompt = t.localize(LocaleResources.CLI_SETUP_THERMOSTAT_CLIENT_USERNAME_PROMPT);
            UsernameCredentialsReader clientUserReader = new UsernameCredentialsReader(console, clientUsernamePrompt);
            clientUsername = clientUserReader.read();
            passwordPrompt = t.localize(LocaleResources.CLI_SETUP_PASSWORD_PROMPT, clientUsername);
            passwordPromptRepeat = t.localize(LocaleResources.CLI_SETUP_PASSWORD_REPEAT_PROMPT, clientUsername);
            PasswordCredentialsReader clientPasswordReader = new PasswordCredentialsReader(console, passwordPrompt, passwordPromptRepeat);
            clientPassword = clientPasswordReader.readPassword();

            LocalizedString agentUsernamePrompt = t.localize(LocaleResources.CLI_SETUP_THERMOSTAT_AGENT_USERNAME_PROMPT);
            UsernameCredentialsReader agentUserReader = new UsernameCredentialsReader(console, agentUsernamePrompt);
            agentUsername = agentUserReader.read();

            try {
                checkUsernamesNotIdentical(clientUsername, agentUsername);
                isUsernameValid = true;
            } catch (IdenticalUsernameException e) {
                Arrays.fill(clientPassword, '\0');
                printErr(LocaleResources.CLI_SETUP_USERNAMES_IDENTICAL, clientUsername);
            }
        } while (!isUsernameValid);
        passwordPrompt = t.localize(LocaleResources.CLI_SETUP_PASSWORD_PROMPT, agentUsername);
        passwordPromptRepeat = t.localize(LocaleResources.CLI_SETUP_PASSWORD_REPEAT_PROMPT, agentUsername);
        PasswordCredentialsReader agentPasswordReader = new PasswordCredentialsReader(console, passwordPrompt, passwordPromptRepeat);
        agentPassword = agentPasswordReader.readPassword();
        thermostatSetup.createClientAdminUser(clientUsername, clientPassword);
        thermostatSetup.createAgentUser(agentUsername, agentPassword);
    }

    void checkUsernamesNotIdentical(String first, String second) throws IdenticalUsernameException {
        if (first.equals(second)) {
            throw new IdenticalUsernameException();
        }
    }

    // package-private for testing
    void readMongodbCredentials() throws IOException {
        println(LocaleResources.CLI_SETUP_MONGODB_USER_CREDS_INTRO);
        LocalizedString usernamePrompt = t.localize(LocaleResources.CLI_SETUP_MONGODB_USERNAME_PROMPT);
        UsernameCredentialsReader usernameReader = new UsernameCredentialsReader(console, usernamePrompt);
        String username = usernameReader.read();
        LocalizedString passwordPrompt = t.localize(LocaleResources.CLI_SETUP_PASSWORD_PROMPT, username);
        LocalizedString confirmPasswordPrompt = t.localize(LocaleResources.CLI_SETUP_PASSWORD_REPEAT_PROMPT, username);
        PasswordCredentialsReader passwordReader = new PasswordCredentialsReader(console, passwordPrompt, confirmPasswordPrompt);
        char[] password = passwordReader.readPassword();
        thermostatSetup.createMongodbUser(username, password);
    }

    /**
     * @return {@code true} if user wants to continue, {@code false} otherwise.
     * @throws IOException
     */
    private boolean readContinueAnswer() throws IOException {
        final String localizedProceedToken = t.localize(LocaleResources.CLI_SETUP_PROCEED_WORD).getContents();
        final String localizedCancelToken = t.localize(LocaleResources.CLI_SETUP_CANCEL_WORD).getContents();
        LocalizedString yes = t.localize(LocaleResources.CLI_SETUP_YES);
        LocalizedString no = t.localize(LocaleResources.CLI_SETUP_NO);
        print(LocaleResources.CLI_SETUP_PROCEED_QUESTION, yes.getContents(), no.getContents());
        String input;
        InputStream in = console.getInput();
        final int maxTries = 100;
        int currTry = 0;
        do {
            input = readLine(in);
            if (input == null) {
                throw new IOException("Unexpected EOF while reading answer.");
            }
            if (input.equals(localizedCancelToken)) {
                return false;
            }
            if (input.equals(localizedProceedToken)) {
                return true;
            }
            printErr(LocaleResources.CLI_SETUP_UNKNOWN_RESPONSE, input, yes.getContents(), no.getContents());
            currTry++;
        } while (currTry < maxTries);
        logger.log(Level.WARNING, "Tried " + maxTries + " times with invalid input. Cancelling.");
        return false;
    }

    private String readLine(InputStream in) throws IOException {
        int c;
        StringBuilder builder = new StringBuilder();
        while ((c = in.read()) != EOF) {
            char token = (char) c;
            if (token == '\n') {
                break;
            }
            builder.append(token);
        }
        if (c == EOF) {
            // reached EOF before we read any other character.
            return null;
        }
        return builder.toString();
    }

    private void printBlurb() {
        String userGuideURL = new ApplicationInfo().getUserGuide();
        println(LocaleResources.CLI_SETUP_INTRO, userGuideURL);
    }

    private void println(LocaleResources resource, String... strings) {
        outWriter.println(t.localize(resource, strings).getContents());
        outWriter.flush();
    }

    private void print(LocaleResources resource, String... strings) {
        outWriter.print(t.localize(resource, strings).getContents());
        outWriter.flush();
    }

    private void printErr(LocaleResources resource, String... strings) {
        errWriter.println(t.localize(resource, strings).getContents());
        errWriter.flush();
    }

    @SuppressWarnings("serial")
    private static class IdenticalUsernameException extends Exception {
        // nothing
    }
}
