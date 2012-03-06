/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Connection {

    public enum ConnectionType {
        LOCAL(false),
        REMOTE(true),
        CLUSTER(true),
        ;

        boolean isDisplayable = false;
        boolean needsUrl = false;

        private ConnectionType(boolean needsUrl) {
            this.needsUrl = needsUrl;
        }

        private ConnectionType(boolean isDisplayable, boolean needsUrl) {
            this.isDisplayable = isDisplayable;
        }

        public boolean isDisplayable() {
            return isDisplayable;
        }

        public boolean needsUrl() {
            return needsUrl;
        }
    }

    public enum ConnectionStatus {
        CONNECTED,
        FAILED_TO_CONNECT,
        DISCONNECTED,
    }

    public interface ConnectionListener {
        public void changed(ConnectionStatus newStatus);
    }

    protected boolean connected = false;

    private ConnectionType type;
    private String url;

    private List<ConnectionListener> listeners = new CopyOnWriteArrayList<ConnectionListener>();

    public void setType(ConnectionType type) {
        this.type = type;
    }

    public ConnectionType getType() {
        return type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        if (url == null) {
            return type.toString();
        }
        return type.toString() + " to " + url;
    }

    public abstract void connect();

    public abstract void disconnect();

    public boolean isConnected() {
        return connected;
    }

    public void addListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ConnectionListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireChanged(ConnectionStatus status) {
        for (ConnectionListener listener: listeners) {
            listener.changed(status);
        }
    }
}
