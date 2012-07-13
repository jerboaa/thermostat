package com.redhat.thermostat.client.ui;

import javax.swing.JTabbedPane;

import org.fest.swing.core.GenericTypeMatcher;

public class TabbedPaneMatcher extends GenericTypeMatcher<JTabbedPane> {

    public TabbedPaneMatcher(Class<JTabbedPane> supportedType) {
        super(supportedType);
    }

    @Override
    protected boolean isMatching(JTabbedPane tab) {
        return tab.getTabCount() > 0;
    }

}
