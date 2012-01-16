package com.redhat.thermostat.client;

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

    private ConnectionType type;
    private String url;

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
}
