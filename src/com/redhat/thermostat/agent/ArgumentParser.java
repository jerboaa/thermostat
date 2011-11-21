package com.redhat.thermostat.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.redhat.thermostat.common.Constants;

public class ArgumentParser {

    private final boolean inSingleMode;
    private final String connectionURL;
    private final Level logLevel;

    public ArgumentParser(StartupConfiguration startupConfig, String[] args) {
        List<String> arguments = new ArrayList<String>(Arrays.asList(args));
        boolean single = false;
        String url = Constants.MONGO_DEFAULT_URL;
        Level level = Level.WARNING;
        int port = -1;

        int index = 0;
        while (index < arguments.size()) {
            if (arguments.get(index).equals("--loglevel")) {
                int next = index + 1;
                if (next < arguments.size()) {
                    level = Level.parse(arguments.get(next).toUpperCase());
                    System.out.println("log level is: " + level);
                    index++;
                }
            } else if (arguments.get(index).equals("--local")) {
                single = true;
            } else if (arguments.get(index).equals("--remote")) {
                single = false;
                int next = index + 1;
                if (next < arguments.size()) {
                    port = Integer.parseInt(arguments.get(next));
                    index++;
                }
            }

            index++;
        }

        if (port != -1) {
            url = url + ":" + port + "/";
        } else if (single) {
            url = url + ":" + startupConfig.getPortForLocal() + "/";
        } else {
            url = url + ":" + startupConfig.getPortForRemote() + "/";
        }

        inSingleMode = single;
        connectionURL = url;
        logLevel = level;
    }

    public boolean inSingleMode() {
        return inSingleMode;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public Level getLogLevel() {
        return logLevel;
    }

}
