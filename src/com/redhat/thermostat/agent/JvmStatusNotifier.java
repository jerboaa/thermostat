package com.redhat.thermostat.agent;

public interface JvmStatusNotifier {

    /**
     * Request to be informed when JVM processes are started or stopped.
     * 
     * @param listener the receiver of future {@link JvmStatusListener.jvmStarted()}
     * and {@link JvmStatusListener.jvmStopped()} calls
     */
    public void addJvmStatusListener(JvmStatusListener listener);

    /**
     * Request to no longer be informed when JVM processes are started or stopped.
     * @param listener the {@link JvmStatusListener} to be unregistered.
     */
    public void removeJvmStatusListener(JvmStatusListener listener);

}
