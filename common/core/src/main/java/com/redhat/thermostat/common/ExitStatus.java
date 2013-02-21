package com.redhat.thermostat.common;

import com.redhat.thermostat.annotations.Service;

/**
 * Service which can be used for certain commands to set the exit status on
 * JVM termination. If the status is set multiple times, the last thread which
 * calls {@link #setExitStatus(int)} wins.
 */
@Service
public interface ExitStatus {

    /**
     * Sets the exit status, which will be used as status when the
     * JVM terminates.
     * 
     * @param newExitStatus
     */
    void setExitStatus(int newExitStatus);
    
    /**
     * Get the currently set exit status.
     * 
     * @return The exit status which should be used on JVM shutdown.
     */
    int getExitStatus();
}
