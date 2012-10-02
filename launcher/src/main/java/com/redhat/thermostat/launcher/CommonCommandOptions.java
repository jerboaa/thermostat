
package com.redhat.thermostat.launcher;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.Command;

public class CommonCommandOptions {

    public static final String DB_URL_ARG = "dbUrl";
    public static final String USERNAME_ARG = "username";
    public static final String PASSWORD_ARG = "password";

    public static final String DB_URL_DESC = "the URL of the storage to connect to";
    public static final String USERNAME_DESC = "the username to use for authentication";
    public static final String PASSWORD_DESC = "the password to use for authentication";

    public static final String LOG_LEVEL_ARG = "logLevel";
    private static final String LOG_LEVEL_DESC = "log level";

    public Options getOptionsFor(Command cmd) {

        Options options = cmd.getOptions();
        addDbUrlOptionForStorageCommand(cmd, options);
        addLogLevelOption(options);
        addOptionalAuthenticationArguments(options);
        return options;
    }

    private void addDbUrlOptionForStorageCommand(Command cmd, Options options) {
        if (cmd.isStorageRequired()) {
            Option option = new Option("d", DB_URL_ARG, true, DB_URL_DESC);
            option.setRequired(false);
            options.addOption(option);
        }
    }

    private void addLogLevelOption(Options options) {
        Option option = new Option(null, LOG_LEVEL_ARG, true, LOG_LEVEL_DESC);
        option.setRequired(false);
        options.addOption(option);
    }

    private void addOptionalAuthenticationArguments(Options options) {

        Option userOption = new Option(null, USERNAME_ARG, true, USERNAME_DESC);
        userOption.setRequired(false);
        options.addOption(userOption);
        Option passwordOption = new Option(null, PASSWORD_ARG, true, PASSWORD_DESC);
        passwordOption.setRequired(false);
        options.addOption(passwordOption);
    }

}

