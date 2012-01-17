package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;

public abstract class Storage {
    private Map<String, Backend> categoryMap;

    public Storage() {
        categoryMap = new HashMap<String, Backend>();
    }

    public abstract void connect(String uri) throws UnknownHostException;

    public abstract void setAgentId(UUID id);

    public abstract void addAgentInformation(StartupConfiguration config, BackendRegistry registry);

    public abstract void removeAgentInformation();

    /**
     * @return {@code null} if the value is invalid or missing
     */
    public abstract String getBackendConfig(String backendName, String configurationKey);

    public final void registerCategory(Category category, Backend backend) {
        if (categoryMap.containsKey(category.getName())) {
            throw new IllegalStateException("Category may only be associated with one backend.");
        }
        categoryMap.put(category.getName(), backend);
    }

    public final void putChunk(Chunk chunk, Backend backend) {
        Category category = chunk.getCategory();
        if (backend != categoryMap.get(category.getName())) { // This had better be not just equivalent, but actually the same object.
            throw new IllegalArgumentException("Invalid category-backend combination while inserting data.  Category: " + category.getName() + "  Backend: " + backend.getName());
        }
        addChunkImpl(chunk);
    }
    
    protected abstract void addChunkImpl(Chunk chunk);
}
