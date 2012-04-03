package com.redhat.thermostat.common.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.common.model.TimeStampedPojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

class HostLatestPojoListGetter<T extends TimeStampedPojo> implements LatestPojoListGetter<T> {

    private Storage storage;
    private Category cat;
    private Converter<T> converter;
    private HostRef ref;

    private Map<HostRef, Long> lastUpdateTimes = new HashMap<>();

    HostLatestPojoListGetter(Storage storage, Category cat, Converter<T> converter, HostRef ref) {
        this.storage = storage;
        this.cat = cat;
        this.converter = converter;
        this.ref = ref;
    }

    @Override
    public List<T> getLatest() {
        Chunk query = buildQuery();
        return getLatest(query);
    }

    private List<T> getLatest(Chunk query) {
        // TODO if multiple threads will be using this utility class, there may be some issues
        // with the updateTimes
        Long lastUpdate = lastUpdateTimes.get(ref);
        Cursor cursor = storage.findAll(query);
        List<T> result = new ArrayList<>();
        while (cursor.hasNext()) {
            Chunk chunk = cursor.next();
            T pojo = converter.fromChunk(chunk);
            result.add(pojo);
            lastUpdateTimes.put(ref, Math.max(pojo.getTimeStamp(), lastUpdate));
        }
        return result;
    }

    protected Chunk buildQuery() {
        Chunk query = new Chunk(cat, false);
        query.put(Key.AGENT_ID, ref.getAgentId());
        Long lastUpdate = lastUpdateTimes.get(ref);
        if (lastUpdate != null) {
            // TODO once we have an index and the 'column' is of type long, use
            // a query which can utilize an index. this one doesn't
            query.put(Key.WHERE, "this.timestamp > " + lastUpdate);
        } else {
            lastUpdateTimes.put(ref, Long.MIN_VALUE);
        }
        return query;
    }
}
