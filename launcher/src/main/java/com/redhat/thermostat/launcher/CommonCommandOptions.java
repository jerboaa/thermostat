
package com.redhat.thermostat.launcher;

import java.util.ArrayList;
import java.util.Collection;

import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;

public class CommonCommandOptions {

    public static final String DB_URL_ARG = "dbUrl";
    public static final String USERNAME_ARG = "username";
    public static final String PASSWORD_ARG = "password";

    public static final String DB_URL_DESC = "the URL of the storage to connect to";
    public static final String USERNAME_DESC = "the username to use for authentication";
    public static final String PASSWORD_DESC = "the password to use for authentication";

    public static final String LOG_LEVEL_ARG = "logLevel";
    private static final String LOG_LEVEL_DESC = "log level";

    public Collection<ArgumentSpec> getAcceptedOptionsFor(Command cmd) {

        Collection<ArgumentSpec> acceptedArguments = cmd.getAcceptedArguments();
        acceptedArguments = new ArrayList<>(acceptedArguments);
        addDbUrlOptionForStorageCommand(cmd, acceptedArguments);
        addLogLevelOption(acceptedArguments);
        addOptionalAuthenticationArguments(acceptedArguments);
        return acceptedArguments;
    }

    private void addDbUrlOptionForStorageCommand(Command cmd, Collection<ArgumentSpec> acceptedArguments) {
        if (cmd.isStorageRequired()) {
            acceptedArguments.add(new SimpleArgumentSpec(DB_URL_ARG, "d", DB_URL_DESC, false, true));
        }
    }

    private void addLogLevelOption(Collection<ArgumentSpec> acceptedArguments) {
        acceptedArguments.add(new SimpleArgumentSpec(LOG_LEVEL_ARG, LOG_LEVEL_DESC, false, true));
    }

    private void addOptionalAuthenticationArguments(Collection<ArgumentSpec> acceptedArguments) {
        acceptedArguments.add(new SimpleArgumentSpec(USERNAME_ARG, USERNAME_DESC, false, true));
        acceptedArguments.add(new SimpleArgumentSpec(PASSWORD_ARG, PASSWORD_DESC, false, true));
    }

}

