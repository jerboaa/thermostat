package com.redhat.thermostat.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * A registry for {@link Backend}s. Each {@link Backend} should call
 * {@link #register(Backend)} to register itself.
 */
public final class BackendRegistry {

    private static BackendRegistry INSTANCE = null;

    private final List<Backend> registeredBackends;

    private BackendRegistry() {
        registeredBackends = new ArrayList<Backend>();
    }

    public static synchronized BackendRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BackendRegistry();
        }
        return INSTANCE;
    }

    public synchronized void register(Backend backend) {
        registeredBackends.add(backend);
    }

    public synchronized void unregister(Backend backend) {
        registeredBackends.remove(backend);
    }

    public synchronized Backend[] getAll() {
        return registeredBackends.toArray(new Backend[0]);
    }

    public synchronized Backend getByName(String name) {
        for (Backend backend : registeredBackends) {
            if (backend.getName().equals((name))) {
                return backend;
            }
        }
        return null;
    }
}
