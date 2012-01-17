package com.redhat.thermostat.client;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class SummaryPanelFacadeImpl implements SummaryPanelFacade {

    private DB db;
    private DBCollection agentConfigCollection;
    private DBCollection vmInfoCollection;

    public SummaryPanelFacadeImpl(DB db) {
        this.db = db;
        this.agentConfigCollection = db.getCollection("agent-config");
        this.vmInfoCollection = db.getCollection("vm-info");
    }

    @Override
    public long getTotalConnectedVms() {
        return vmInfoCollection.getCount();
    }

    @Override
    public long getTotalConnectedAgents() {
        return agentConfigCollection.getCount();
    }

    @Override
    public List<String> getIssues() {
        return new ArrayList<String>();
    }

}
