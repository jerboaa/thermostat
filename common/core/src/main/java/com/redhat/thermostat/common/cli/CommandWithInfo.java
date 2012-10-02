package com.redhat.thermostat.common.cli;

import org.apache.commons.cli.Options;

public abstract class CommandWithInfo implements Command {

    private CommandInfo info;
    private static final String noDesc = "Description not available.";
    private static final String noUsage = "Usage not available.";

    void setCommandInfo(CommandInfo info) {
        this.info = info; 
    }

    @Override
    public String getDescription() {
        String desc = null;
        try {
            desc = info.getDescription();
        } catch (NullPointerException infoWasNotSet) {}

        if (desc == null) {
            desc = noDesc;
        }
        return desc;
    }

    @Override
    public String getUsage() {
        String usage = null;
        try {
            usage = info.getUsage();
        } catch (NullPointerException infoNotSet) {}
        if (usage == null) {
            usage = noUsage;
        }
        return usage;
    }

    @Override
    public Options getOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isStorageRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAvailableInShell() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAvailableOutsideShell() {
        // TODO Auto-generated method stub
        return false;
    }

}
