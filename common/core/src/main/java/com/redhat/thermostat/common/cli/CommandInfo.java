package com.redhat.thermostat.common.cli;

import java.util.List;

import org.apache.commons.cli.Options;

public interface CommandInfo {

    public String getName();

    public String getDescription();

    public String getUsage();

    public Options getOptions();

    public List<String> getDependencyResourceNames();

}
