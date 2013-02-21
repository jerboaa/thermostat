package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.ExitStatus;

/**
 * @see {@link ExitStatus}
 *
 */
public class ExitStatusImpl implements ExitStatus {

    private int exitStatus;
    
    ExitStatusImpl(int initialExitStatus) {
        this.exitStatus = initialExitStatus;
    }
    
    public synchronized void setExitStatus(int newExitStatus) {
        this.exitStatus = newExitStatus;
    }
    
    public synchronized int getExitStatus() {
        return this.exitStatus;
    }
}
