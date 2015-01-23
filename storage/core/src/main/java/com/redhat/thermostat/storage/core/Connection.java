/*
 * Copyright 2012-2015 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.storage.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * {@link Storage} delegates to this class for connecting to storage.
 *
 */
public abstract class Connection {

    /**
     * @see {@link ConnectionListener},
     * 
     */
    public enum ConnectionStatus {
        CONNECTED,
        CONNECTING,
        FAILED_TO_CONNECT,
        DISCONNECTED,
    }

    public interface ConnectionListener {
        /**
         * Called by the connection reporting a status update.
         * 
         * @param newStatus The new status.
         * 
         * @see ConnectionStatus
         */
        public void changed(ConnectionStatus newStatus);
    }

    protected boolean connected = false;

    private String url;

    private List<Connection.ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Sets the connection URL to which to connect to.
     * 
     * @param url The connection URL.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 
     * @return The connection URL.
     */
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return url == null ? "" : url;
    }

    /**
     * Connects to storage. Implementors are responsible to fire at least the
     * following connection events:
     * <ul>
     * <li>{@link ConnectionStatus.CONNECTED}: Once the connection has been made
     * successfully.</li>
     * <li>{@link ConnectionStatus.FAILED_TO_CONNECT}: If the connection could
     * not be established for some reason.</li>
     * </ul>
     * 
     * These events are mutually exclusive. One of the two needs to be fired in
     * any case.
     */
    public abstract void connect();

    /**
     * Disconnects from storage. Implementors are responsible to fire at least
     * the following connection events:
     * <ul>
     * <li>{@link ConnectionStatus.DISCONNECTED}: Once disconnect has finished.</li>
     * </ul>
     */
    public abstract void disconnect();

    
    /**
     * May be used to determine the state of a connection if no connect or
     * disconnect attempt is currently in progress.
     * 
     * @return Provided the connection is not in a transitive state (e.g.
     *         ongoing connect/disconnect), true if storage is connected. False
     *         otherwise.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Adds a connection listener to this connection. This may be used for
     * progress reporting.
     * 
     * @param listener
     *            The listener which will be notified of connection events.
     */
    public void addListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a formerly registered listener. No-op if the given listener
     * is not currently registered.
     * 
     * @param listener The listener which should get removed.
     */
    public void removeListener(ConnectionListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners of this ConnectionStatus event.
     * 
     * @param status The status which is passed on to listeners.
     */
    protected void fireChanged(ConnectionStatus status) {
        for (ConnectionListener listener: listeners) {
            listener.changed(status);
        }
    }
}

