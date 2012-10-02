package com.redhat.thermostat.common.cli;

import java.util.logging.Logger;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.utils.LoggingUtils;

public abstract class CommandWithInfo implements Command {

    private static final Logger logger = LoggingUtils.getLogger(CommandWithInfo.class);
    private CommandInfo info;
    private static final String noDesc = "Description not available.";
    private static final String noUsage = "Usage not available.";

    void setCommandInfo(CommandInfo info) {
        this.info = info; 
    }

    boolean hasCommandInfo() {
        return info != null;
    }

    @Override
    public String getDescription() {
        String desc = null;
        if (hasCommandInfo()) {
            desc = info.getDescription();
        }
        if (desc == null) {
            desc = noDesc;
        }
        return desc;
    }

    @Override
    public String getUsage() {
        String usage = null;
        if (hasCommandInfo()) { 
            usage = info.getUsage();
        }
        if (usage == null) {
            usage = noUsage;
        }
        return usage;
    }

    @Override
    public Options getOptions() {
        try {
            return info.getOptions();
        } catch (NullPointerException e) {
            logger.warning("CommandInfo not yet set, returning empty Options.");
            return new Options();
        }
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
