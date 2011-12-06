package com.redhat.thermostat.backend;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.Storage;
import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.system.SystemBackend;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * A registry for {@link Backend}s. Each {@link Backend} should call
 * {@link #register(Backend)} to register itself.
 */
public final class BackendRegistry {

    private static final Logger logger = LoggingUtils.getLogger(BackendRegistry.class);

    private final Map<String, Backend> registeredBackends;

    public BackendRegistry(StartupConfiguration config, Storage storage) throws BackendLoadException {
        registeredBackends = new HashMap<String, Backend>();

        /*
         * Configure the always-on backends
         */
        Backend systemBackend = new SystemBackend();
        logger.log(Level.FINE, "Initializing backend: \"" + systemBackend.getClass().getCanonicalName() + "\"");
        systemBackend.setInitialConfiguration(config.getStartupBackendConfigMap(systemBackend.getName()));
        systemBackend.setStorage(storage);
        register(systemBackend);

        /*
         * Configure the dynamic/custom backends
         */
        for (String backendClassName : config.getStartupBackendClassNames()) {
            logger.log(Level.FINE, "Initializing backend: \"" + backendClassName + "\"");
            Backend backend = null;
            try {
                Class<? > c = Class.forName(backendClassName);
                Class<? extends Backend> narrowed = c.asSubclass(Backend.class);
                Constructor<? extends Backend> backendConstructor = narrowed.getConstructor();
                backend = backendConstructor.newInstance();
                backend.setInitialConfiguration(config.getStartupBackendConfigMap(backend.getName()));
                backend.setStorage(storage);
            } catch (Exception e) {
                throw new BackendLoadException("Could not instantiate configured backend class: " + backendClassName, e);
            }
            register(backend);
        }
    }

    private synchronized void register(Backend backend) throws BackendLoadException {
        if (registeredBackends.containsKey(backend.getName())) {
            throw new BackendLoadException("Attempt to register two backends with the same name: " + backend.getName());
        }
        registeredBackends.put(backend.getName(), backend);
    }

    private synchronized void unregister(Backend backend) {
        registeredBackends.remove(backend.getName());
    }

    public synchronized Collection<Backend> getAll() {
        return registeredBackends.values();
    }

    public synchronized Backend getByName(String name) {
        for (Backend backend : registeredBackends.values()) {
            if (backend.getName().equals((name))) {
                return backend;
            }
        }
        return null;
    }
}
