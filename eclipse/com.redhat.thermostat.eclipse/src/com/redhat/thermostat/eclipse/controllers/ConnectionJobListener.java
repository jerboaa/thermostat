package com.redhat.thermostat.eclipse.controllers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;

import com.redhat.thermostat.eclipse.Activator;
import com.redhat.thermostat.eclipse.views.HostsVmsTreeViewPart;

public class ConnectionJobListener extends JobChangeAdapter {

    private HostsVmsTreeViewPart view;
    private Action connectAction;
    
    public ConnectionJobListener(Action connectAction, HostsVmsTreeViewPart view) {
        this.view = view;
        this.connectAction = connectAction;
    }
    
    @Override
    public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if (result.isOK()) {
            connectAction.setImageDescriptor(Activator
                    .getImageDescriptor("icons/online.png"));
            connectAction.setEnabled(!Activator.getDefault().isConnected());
            connectAction.setToolTipText("Online");
            view.showHostVmsPage();
        }
    }
}
