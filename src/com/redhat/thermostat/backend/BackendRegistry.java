package com.redhat.thermostat.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * A registry for {@link Backend}s. Each {@link Backend} should call
 * {@link #register(Backend, BackendFeature[])} to register itself.
 */
public final class BackendRegistry {

    private static BackendRegistry INSTANCE = null;

    private static ServiceLoader<Backend> backendLoader = ServiceLoader.load(Backend.class);

    private final List<Backend> registeredBackends;
    private final Map<BackendFeature, Set<Backend>> featureToBackend;

    private BackendRegistry() {
        registeredBackends = new ArrayList<Backend>();
        featureToBackend = new HashMap<BackendFeature, Set<Backend>>();

        for (Backend newBackend : backendLoader) {
            register(newBackend, newBackend.getSupportedFeatures());
        }
    }

    public static synchronized BackendRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BackendRegistry();
        }
        return INSTANCE;
    }

    public synchronized void register(Backend backend, BackendFeature[] features) {
        registeredBackends.add(backend);
        for (BackendFeature feature : features) {
            if (!featureToBackend.containsKey(feature)) {
                featureToBackend.put(feature, new HashSet<Backend>());
            }
            featureToBackend.get(feature).add(backend);
        }
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

    public synchronized Backend[] getBackendsForFeature(BackendFeature feature) {
        return featureToBackend.get(feature).toArray(new Backend[0]);
    }

}
