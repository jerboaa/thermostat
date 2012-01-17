package com.redhat.thermostat.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Connection {

    public enum ConnectionType {
        LOCAL(false),
        REMOTE(true),
        CLUSTER(true),
        NONE(false, false), ;

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
