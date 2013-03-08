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
    
    public synchronized void setExitStatus(int newExitStatus)
            throws IllegalArgumentException {
        if (newExitStatus < 0 || newExitStatus > 255) {
            throw new IllegalArgumentException(
                    "Status (" + newExitStatus + ") out ouf range. 0 <= status <= 255.");
        }
        this.exitStatus = newExitStatus;
    }
    
    public synchronized int getExitStatus() {
        return this.exitStatus;
    }
}
