package com.redhat.thermostat.common.cli;

import java.util.List;

public interface CommandInfo {

    public String getName();

    public String getDescription();

    public String getUsage();

    public List<String> getDependencyResourceNames();

}
