package com.redhat.thermostat.client;

import java.util.List;

public interface SummaryPanelFacade {

    public abstract long getTotalConnectedVms();

    public abstract long getTotalConnectedAgents();

    public List<String> getIssues();

}
